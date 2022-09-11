package ru.spliterash.pubSubMessaging.base.service;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import ru.spliterash.pubSubMessaging.base.annotations.Request;
import ru.spliterash.pubSubMessaging.base.annotations.RequestAnnotationUtils;
import ru.spliterash.pubSubMessaging.base.body.BodyContainer;
import ru.spliterash.pubSubMessaging.base.body.RequestCompleteContainer;
import ru.spliterash.pubSubMessaging.base.exceptions.PubSubTimeout;
import ru.spliterash.pubSubMessaging.base.pubSub.PubSubGateway;
import ru.spliterash.pubSubMessaging.base.pubSub.Subscribe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;

@Log4j2
public class PubSubMessagingService {
    /**
     * То КУДА приходят запросы
     */
    private static final String IN = "in";
    /**
     * То, ГДЕ мы ждём ОТВЕТЫ
     */
    private static final String OUT = "out";

    private static final Integer TIMEOUT = 1000 * 5;
    // Стартовый путь
    private final String startPath;
    private final String currentDomain;
    private final PubSubGateway pubSubGateway;
    private final ClassLoader loader;

    private final Collection<Subscribe> domainSubscribe;

    private final Map<String, RequestProcessor<?, ?>> listeners = new HashMap<>();
    private final Map<UUID, CompletableFuture<Object>> requestsInProcess = new ConcurrentHashMap<>();

    public PubSubMessagingService(String startPath, String currentDomain, PubSubGateway pubSubGateway) {
        this(PubSubMessagingService.class.getClassLoader(), startPath, currentDomain, pubSubGateway);
    }

    public PubSubMessagingService(ClassLoader loader, String startPath, String currentDomain, PubSubGateway pubSubGateway) {
        this.loader = loader;
        this.startPath = startPath;
        this.currentDomain = currentDomain;
        this.pubSubGateway = pubSubGateway;

        Subscribe input = pubSubGateway.subscribe(
                BodyContainer.class,
                getFullDomainPath(currentDomain) + pubSubGateway.getNamespaceDelimiter() + IN,
                this::processRequest
        );
        Subscribe output = pubSubGateway.subscribe(
                RequestCompleteContainer.class,
                getFullDomainPath(currentDomain) + pubSubGateway.getNamespaceDelimiter() + OUT,
                this::acceptRequestCompleteNotification
        );

        domainSubscribe = Arrays.asList(input, output);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    private void processRequest(BodyContainer<?> bodyContainer) {
        RequestProcessor<?, ?> listener = listeners.get(bodyContainer.getPath());
        Object request = bodyContainer.getRequest();

        if (listener == null) {
            log.warn("Listener for path " + bodyContainer.getPath() + " not found");
            return;
        }
        try {
            Object response = listener.onRequestUnchecked(request);
            if (response instanceof Future) {
                Future<Object> future = (Future<Object>) response;
                CompletableFuture<Object> completable;
                if (response instanceof CompletableFuture)
                    completable = (CompletableFuture<Object>) response;
                else if (future.isDone()) {
                    completable = CompletableFuture.completedFuture(future.get());
                } else {
                    completable = CompletableFuture.supplyAsync(() -> {
                        try {
                            return future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new CompletionException(e.getCause());
                        }
                    });
                }
                completable.whenCompleteAsync((o, throwable) -> {
                    RequestCompleteContainer<Object> container;
                    if (throwable != null) {
                        Throwable finalThrowable;
                        if (throwable instanceof CompletionException)
                            finalThrowable = throwable.getCause();
                        else
                            finalThrowable = throwable;

                        container = new RequestCompleteContainer<>(bodyContainer.getId(), finalThrowable);
                    } else
                        container = new RequestCompleteContainer<>(bodyContainer.getId(), o);

                    sendResult(bodyContainer.getSenderDomain(), container);
                });
            } else {
                sendResult(bodyContainer.getSenderDomain(), new RequestCompleteContainer<>(bodyContainer.getId(), response));
            }
        } catch (Exception exception) {
            sendResult(bodyContainer.getSenderDomain(), new RequestCompleteContainer<>(bodyContainer.getId(), exception));
        }

    }

    private void sendResult(String senderDomain, RequestCompleteContainer<?> container) {
        pubSubGateway.dispatch(getFullDomainPath(senderDomain + pubSubGateway.getNamespaceDelimiter() + OUT), container);
    }

    private void acceptRequestCompleteNotification(RequestCompleteContainer<?> complete) {
        UUID id = complete.getRequestID();

        CompletableFuture<Object> waitingFuture = requestsInProcess.remove(id);

        if (waitingFuture != null) {
            if (complete.isSuccess())
                waitingFuture.complete(complete.getResponse());
            else
                waitingFuture.completeExceptionally(complete.getException());
        }
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

        requestsInProcess.put(requestID, future);
        pubSubGateway.dispatch(getFullDomainPath(domain) + pubSubGateway.getNamespaceDelimiter() + IN, objectBodyContainer);

        return future;
    }

    private void makeRequestVoid(String domain, String path, Object body) {
        UUID requestID = UUID.randomUUID();
        BodyContainer<Object> objectBodyContainer = new BodyContainer<>(requestID, currentDomain, path, body);

        pubSubGateway.dispatch(getFullDomainPath(domain) + pubSubGateway.getNamespaceDelimiter() + IN, objectBodyContainer);
    }

    private void registerListener(String path, RequestProcessor<Object, Object> handleFunc) {
        listeners.put(path, handleFunc);
    }

    public void destroy() {
        domainSubscribe.forEach(Subscribe::unsubscribe);
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
            Object arg;
            if (args == null || args.length == 0)
                arg = null;
            else if (args.length == 1)
                arg = args[0];
            else
                throw new RuntimeException("Too many arguments on method " + method.getName() + " in class " + clientClass.getName());
            String path = RequestAnnotationUtils.getFullPath(clientClass, annotation);

            if (method.getReturnType().equals(Void.class)) {
                makeRequestVoid(domain, path, arg);
                return null;
            }
            CompletableFuture<Object> requestFuture = makeRequest(domain, path, arg);
            // Если этот метод возвращает фучуре, то вернём то, что он хочет
            if (Future.class.isAssignableFrom(method.getReturnType())) {
                return requestFuture;
            } else
                try {
                    return requestFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (TimeoutException timeoutException) {
                    throw new PubSubTimeout(domain, annotation.value());
                } catch (ExecutionException exception) {
                    throw exception.getCause();
                }
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
                int parameterCount = method.getParameterTypes().length;

                String methodPath = RequestAnnotationUtils.getFullPath(listenerClass, request);
                if (parameterCount == 0)
                    registerListener(methodPath, input -> methodHandle.invokeWithArguments()); //Input null
                else if (parameterCount == 1)
                    registerListener(methodPath, methodHandle::invokeWithArguments);
                else
                    throw new RuntimeException("Too many arguments on method " + method.getName() + " in class " + listenerClass.getName());

            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
