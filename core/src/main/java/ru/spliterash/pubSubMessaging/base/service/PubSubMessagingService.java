package ru.spliterash.pubSubMessaging.base.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.spliterash.pubSubMessaging.base.annotations.Request;
import ru.spliterash.pubSubMessaging.base.annotations.RequestAnnotationUtils;
import ru.spliterash.pubSubMessaging.base.body.BodyContainer;
import ru.spliterash.pubSubMessaging.base.body.HeartbeatContainer;
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

@Slf4j
public class PubSubMessagingService {
    /**
     * То КУДА приходят запросы
     */
    private static final String IN = "in";
    /**
     * То, ГДЕ мы ждём ОТВЕТЫ
     */
    private static final String OUT = "out";
    /**
     * Куда приходят уведы о том, что запрос ещё не сдох, просто долго
     */
    private static final String HEARTBEAT = "heartbeat";

    // Стартовый путь
    private final String startPath;
    private final String currentDomain;
    private final PubSubGateway pubSubGateway;
    private final ClassLoader loader;

    private final Collection<Subscribe> domainSubscribe;
    private final ScheduledExecutorService scheduledExecutorService;

    private final Map<String, RequestProcessor<?, ?>> listeners = new HashMap<>();
    private final Map<UUID, OutboundRequestInProcess> outboundRequestsInProcess = new ConcurrentHashMap<>();

    public PubSubMessagingService(String startPath, String currentDomain, PubSubGateway pubSubGateway) {
        this(PubSubMessagingService.class.getClassLoader(), startPath, currentDomain, pubSubGateway, Executors.newSingleThreadScheduledExecutor((r) -> {
            Thread thread = new Thread(r);
            thread.setName("PubSubSchedulerThread");
            return thread;
        }));
    }

    public PubSubMessagingService(
            ClassLoader loader,
            String startPath,
            String currentDomain,
            PubSubGateway pubSubGateway,
            ScheduledExecutorService executorService
    ) {
        this.loader = loader;
        this.startPath = startPath;
        this.currentDomain = currentDomain;
        this.pubSubGateway = pubSubGateway;
        this.scheduledExecutorService = executorService;

        Subscribe input = pubSubGateway.subscribe(
                BodyContainer.class,
                getFullDomainPath(currentDomain) + pubSubGateway.getNamespaceDelimiter() + IN,
                this::processRequest
        );
        Subscribe heartbeat = pubSubGateway.subscribe(
                HeartbeatContainer.class,
                getFullDomainPath(currentDomain) + pubSubGateway.getNamespaceDelimiter() + HEARTBEAT,
                this::acceptHeartbeat
        );
        Subscribe output = pubSubGateway.subscribe(
                RequestCompleteContainer.class,
                getFullDomainPath(currentDomain) + pubSubGateway.getNamespaceDelimiter() + OUT,
                this::acceptRequestCompleteNotification
        );

        domainSubscribe = Arrays.asList(input, heartbeat, output);

    }

    private void acceptHeartbeat(HeartbeatContainer heartbeatContainer) {
        OutboundRequestInProcess request = outboundRequestsInProcess.get(heartbeatContainer.getRequestID());
        if (request == null) {
            log.warn("Received heartbeat, but no task with id {} found", heartbeatContainer.getRequestID());
            return;
        }

        request.stillAlive();
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
            if (listener.isVoid())
                return;
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
                InboundRequestInProcess requestInProcess = new InboundRequestInProcess(bodyContainer.getId(), bodyContainer.getSenderDomain());
                if (!completable.isDone())
                    requestInProcess.startHeartbeatTask();

                completable.whenCompleteAsync((o, throwable) -> {
                    if (throwable != null) {
                        Throwable finalThrowable;
                        if (throwable instanceof CompletionException)
                            finalThrowable = throwable.getCause();
                        else
                            finalThrowable = throwable;

                        requestInProcess.completeExceptionally(finalThrowable);
                    } else
                        requestInProcess.complete(o);
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

    private void sendHeartbeat(String domain, UUID requestId) {
        pubSubGateway.dispatch(getFullDomainPath(domain + pubSubGateway.getNamespaceDelimiter() + HEARTBEAT), new HeartbeatContainer(requestId));
    }

    private void acceptRequestCompleteNotification(RequestCompleteContainer<?> complete) {
        UUID id = complete.getRequestID();

        OutboundRequestInProcess request = outboundRequestsInProcess.remove(id);
        if (request == null) {
            log.warn("Received completion for id " + id + ", but no task with current id found");
            return;
        }
        request.cancelTask();

        CompletableFuture<Object> waitingFuture = request.result;

        if (complete.isSuccess())
            waitingFuture.complete(complete.getResponse());
        else
            waitingFuture.completeExceptionally(complete.getException());
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
        UUID requestID = UUID.randomUUID();
        BodyContainer<Object> objectBodyContainer = new BodyContainer<>(requestID, currentDomain, path, body);
        OutboundRequestInProcess request = new OutboundRequestInProcess(requestID, domain, path);
        request.stillAlive();

        outboundRequestsInProcess.put(requestID, request);
        pubSubGateway.dispatch(getFullDomainPath(domain) + pubSubGateway.getNamespaceDelimiter() + IN, objectBodyContainer);

        return request.result;
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
            Object arg;
            if (args == null || args.length == 0)
                arg = null;
            else if (args.length == 1)
                arg = args[0];
            else
                throw new RuntimeException("Too many arguments on method " + method.getName() + " in class " + clientClass.getName());
            String path = RequestAnnotationUtils.getFullPath(clientClass, method, annotation);

            if (method.getReturnType().equals(void.class)) {
                makeRequestVoid(domain, path, arg);
                return null;
            }
            CompletableFuture<Object> requestFuture = makeRequest(domain, path, arg);
            // Если этот метод возвращает фучуре, то вернём то, что он хочет
            if (Future.class.isAssignableFrom(method.getReturnType())) {
                return requestFuture;
            } else
                try {
                    return requestFuture.get();
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
                MethodHandle methodHandle = lookup.unreflect(method).bindTo(instance);
                int parameterCount = method.getParameterTypes().length;

                String methodPath = RequestAnnotationUtils.getFullPath(listenerClass, method, request);
                boolean isVoid = method.getReturnType().equals(void.class);

                if (parameterCount == 0)
                    registerListener(methodPath, new RequestProcessor<Object, Object>() {
                        @Override
                        public Object onRequest(Object input) throws Throwable {
                            return methodHandle.invokeWithArguments();
                        }

                        @Override
                        public boolean isVoid() {
                            return isVoid;
                        }
                    }); //Input null
                else if (parameterCount == 1)
                    registerListener(methodPath, new RequestProcessor<Object, Object>() {
                        @Override
                        public Object onRequest(Object input) throws Throwable {
                            return methodHandle.invokeWithArguments(input);
                        }

                        @Override
                        public boolean isVoid() {
                            return isVoid;
                        }
                    });
                else
                    throw new RuntimeException("Too many arguments on method " + method.getName() + " in class " + listenerClass.getName());

            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @RequiredArgsConstructor
    private class OutboundRequestInProcess {
        private final UUID id;
        private final String domain;
        private final String path;
        private final CompletableFuture<Object> result = new CompletableFuture<>();
        private ScheduledFuture<?> cancellationTask;

        public void stillAlive() {
            if (cancellationTask != null) cancellationTask.cancel(false);
            cancellationTask = scheduledExecutorService.schedule(() -> {
                OutboundRequestInProcess removed = outboundRequestsInProcess.remove(id);
                if (removed != this) return;

                result.completeExceptionally(new PubSubTimeout(domain, path));
            }, 5, TimeUnit.SECONDS);
        }

        public void cancelTask() {
            if (cancellationTask != null) cancellationTask.cancel(false);
            cancellationTask = null;
        }
    }

    private final class InboundRequestInProcess {
        private final UUID id;
        private final String domain;
        private ScheduledFuture<?> heartbeatTask;

        public InboundRequestInProcess(UUID id, String domain) {
            this.id = id;
            this.domain = domain;
        }

        public void startHeartbeatTask() {
            heartbeatTask = scheduledExecutorService.scheduleWithFixedDelay(() -> {
                sendHeartbeat(domain, id);
            }, 2, 2, TimeUnit.SECONDS);
        }

        public void complete(Object result) {
            if (heartbeatTask != null) heartbeatTask.cancel(false);
            sendResult(domain, new RequestCompleteContainer<>(id, result));
        }

        public void completeExceptionally(Throwable exception) {
            if (heartbeatTask != null) heartbeatTask.cancel(false);
            sendResult(domain, new RequestCompleteContainer<>(id, exception));
        }
    }

    private interface RequestProcessor<I, O> {
        O onRequest(I input) throws Throwable;

        boolean isVoid();

        default Object onRequestUnchecked(Object in) throws Throwable {
            //noinspection unchecked
            return onRequest((I) in);
        }
    }
}
