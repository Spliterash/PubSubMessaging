package ru.spliterash.pubSubServiceMessaging.base.service;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import ru.spliterash.pubSubServiceMessaging.base.annotations.Request;
import ru.spliterash.pubSubServiceMessaging.base.body.BodyContainer;
import ru.spliterash.pubSubServiceMessaging.base.body.RequestCompleteContainer;
import ru.spliterash.pubSubServiceMessaging.base.pubSub.PubSubGateway;
import ru.spliterash.pubSubServiceMessaging.base.pubSub.Subscribe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Log4j2
public class PubSubServiceMessagingService {
    /**
     * То КУДА приходят запросы
     */
    private static final String IN = "in";
    /**
     * То, ГДЕ мы ждём ОТВЕТЫ
     */
    private static final String OUT = "out";

    private static final Integer TIMEOUT = 1000 * 5;
    private final Executor executor;
    // Стартовый путь
    private final String startPath;
    private final String currentDomain;
    private final PubSubGateway pubSubGateway;
    private final ClassLoader loader;


    private final Map<String, RequestProcessor<?, ?>> listeners = new HashMap<>();
    private final Map<String, Subscribe> domainSubscribe = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Object>> requestsInProcess = new ConcurrentHashMap<>();

    public PubSubServiceMessagingService(String startPath, String currentDomain, PubSubGateway pubSubGateway) {
        this(Executors.newCachedThreadPool(), PubSubServiceMessagingService.class.getClassLoader(), startPath, currentDomain, pubSubGateway);
    }

    public PubSubServiceMessagingService(Executor executor, ClassLoader loader, String startPath, String currentDomain, PubSubGateway pubSubGateway) {
        this.executor = executor;
        this.loader = loader;
        this.startPath = startPath;
        this.currentDomain = currentDomain;
        this.pubSubGateway = pubSubGateway;

        pubSubGateway.subscribe(BodyContainer.class, getFullDomainPath(currentDomain) + pubSubGateway.getNamespaceDelimiter() + IN, this::processRequest);
        pubSubGateway.subscribe(RequestCompleteContainer.class, getFullDomainPath(currentDomain) + pubSubGateway.getNamespaceDelimiter() + OUT, this::acceptRequestCompleteNotification);
    }

    @SneakyThrows
    private void processRequest(BodyContainer<?> bodyContainer) {
        RequestProcessor<?, ?> listener = listeners.get(bodyContainer.getPath());
        Object request = bodyContainer.getRequest();

        if (listener == null) {
            log.warn("Listener for path " + bodyContainer.getPath() + " not found");
            return;
        }

        Object response = listener.onRequestUnchecked(request); // TODO Возвращать эксепшены
        RequestCompleteContainer<Object> completeContainer = new RequestCompleteContainer<>(bodyContainer.getId(), response);

        pubSubGateway.dispatch(getFullDomainPath(bodyContainer.getSenderDomain() + pubSubGateway.getNamespaceDelimiter() + OUT), completeContainer);
    }

    private void acceptRequestCompleteNotification(RequestCompleteContainer<?> complete) {
        UUID id = complete.getRequestID();

        CompletableFuture<Object> waitingFuture = requestsInProcess.remove(id);

        if (waitingFuture != null)
            waitingFuture.complete(complete.getResponse());
    }

    protected String getFullDomainPath(String domain) {
        return startPath + pubSubGateway.getNamespaceDelimiter() + domain;
    }

    /**
     * Выполнить запрос
     *
     * @param domain На какой домен шлём запрос
     * @param path   По какому пути
     * @param body   Тело запроса
     * @return То что вернёт запрос
     */
    private CompletableFuture<Object> makeRequest(String domain, String path, Object body) {
        CompletableFuture<Object> future = new CompletableFuture<>();

        UUID requestID = UUID.randomUUID();
        BodyContainer<Object> objectBodyContainer = new BodyContainer<>(requestID, currentDomain, path, body);

        pubSubGateway.dispatch(getFullDomainPath(domain) + pubSubGateway.getNamespaceDelimiter() + IN, objectBodyContainer);
        requestsInProcess.put(requestID, future);

        return future;
    }

    private void registerListener(String path, RequestProcessor<Object, Object> handleFunc) {
        listeners.put(path, handleFunc);
    }

    public void destroy() {
        if (executor instanceof ExecutorService)
            ((ExecutorService) executor).shutdown();

        domainSubscribe.values().forEach(Subscribe::unsubscribe);
        domainSubscribe.clear();
    }

    /**
     * Под доменом в данном случае подразумевается какой-то отдельный инстанц
     * <p>
     * В случае с майнкрафтом это будет имя сервера
     */
    @SuppressWarnings("unchecked")
    public <T> T createClient(String domain, Class<T> clientClass) {
        return (T) Proxy.newProxyInstance(loader, new Class[]{clientClass}, (proxy, method, args) -> {
            Request annotation = method.getAnnotation(Request.class);

            if (annotation == null)
                throw new RuntimeException("Annotation request not present");
            if (args.length == 0)
                throw new RuntimeException("No method argument");

            CompletableFuture<Object> future = makeRequest(domain, annotation.value(), args[0]);

            return future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        });
    }

    public <T> void registerHandler(Class<T> listenerClass, Object instance) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            for (Method method : listenerClass.getMethods()) {
                Request request = method.getAnnotation(Request.class);
                if (request == null)
                    throw new RuntimeException("Request not present exception");
                MethodHandle methodHandle = lookup.unreflect(method).bindTo(instance);

                registerListener(request.value(), methodHandle::invoke);
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
