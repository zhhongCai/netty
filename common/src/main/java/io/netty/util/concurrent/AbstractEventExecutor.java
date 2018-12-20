/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.util.concurrent;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Abstract base class for {@link EventExecutor} implementations.
 */
public abstract class AbstractEventExecutor extends AbstractExecutorService implements EventExecutor {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractEventExecutor.class);

    static final long DEFAULT_SHUTDOWN_QUIET_PERIOD = 2;
    static final long DEFAULT_SHUTDOWN_TIMEOUT = 15;

    private final EventExecutorGroup parent;
    private final Collection<EventExecutor> selfCollection = Collections.<EventExecutor>singleton(this);

    protected AbstractEventExecutor() {
        this(null);
    }

    protected AbstractEventExecutor(EventExecutorGroup parent) {
        this.parent = parent;
    }

    @Override
    public EventExecutorGroup parent() {
        return parent;
    }

    @Override
    public EventExecutor next() {
        return this;
    }

    @Override
    public boolean inEventLoop() {
        return inEventLoop(Thread.currentThread());
    }

    @Override
    public Iterator<EventExecutor> iterator() {
        return selfCollection.iterator();
    }

    @Override
    public Future<?> shutdownGracefully() {
        return shutdownGracefully(DEFAULT_SHUTDOWN_QUIET_PERIOD, DEFAULT_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
     */
    @Override
    @Deprecated
    public abstract void shutdown();

    /**
     * @deprecated {@link #shutdownGracefully(long, long, TimeUnit)} or {@link #shutdownGracefully()} instead.
     */
    @Override
    @Deprecated
    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }

    @Override
    public <V> Promise<V> newPromise() {
        return new DefaultPromise<V>(this);
    }

    @Override
    public <V> ProgressivePromise<V> newProgressivePromise() {
        return new DefaultProgressivePromise<V>(this);
    }

    @Override
    public <V> Future<V> newSucceededFuture(V result) {
        return new SucceededFuture<V>(this, result);
    }

    @Override
    public <V> Future<V> newFailedFuture(Throwable cause) {
        return new FailedFuture<V>(this, cause);
    }

    @Override
    public final Future<?> submit(Runnable task) {
        return (Future<?>) super.submit(task);
    }

    @Override
    public final <T> Future<T> submit(Runnable task, T result) {
        return (Future<T>) super.submit(task, result);
    }

    @Override
    public final <T> Future<T> submit(Callable<T> task) {
        return (Future<T>) super.submit(task);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return of(this.<T>newPromise(), runnable, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return of(this.<T>newPromise(), callable);
    }

    /**
     * Try to execute the given {@link Runnable} and just log if it throws a {@link Throwable}.
     */
    static void safeExecute(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            logger.warn("A task raised an exception. Task: {}", task, t);
        }
    }

    /**
     * Returns a new {@link RunnableFuture} build on top of the given {@link Promise} and {@link Callable}.
     *
     * This can be used if you want to override {@link #newTaskFor(Callable)} and return a different
     * {@link RunnableFuture}.
     */
    private static <V> RunnableFuture<V> of(Promise<V> promise, Callable<V> task) {
        return new RunnableFutureAdapter<V>(promise, task);
    }

    /**
     * Returns a new {@link RunnableFuture} build on top of the given {@link Promise} and {@link Runnable} and
     * {@code value}.
     *
     * This can be used if you want to override {@link #newTaskFor(Runnable, V)} and return a different
     * {@link RunnableFuture}.
     */
    private static <V> RunnableFuture<V> of(Promise<V> promise, Runnable task, V value) {
        return new RunnableFutureAdapter<V>(promise, Executors.callable(task, value));
    }

    private static final class RunnableFutureAdapter<V> implements RunnableFuture<V> {

        private final Promise<V> promise;
        private final Callable<V> task;

        RunnableFutureAdapter(Promise<V> promise, Callable<V> task) {
            this.promise = ObjectUtil.checkNotNull(promise, "promise");
            this.task = ObjectUtil.checkNotNull(task, "task");
        }

        @Override
        public boolean isSuccess() {
            return promise.isSuccess();
        }

        @Override
        public boolean isCancellable() {
            return promise.isCancellable();
        }

        @Override
        public Throwable cause() {
            return promise.cause();
        }

        @Override
        public RunnableFuture<V> addListener(GenericFutureListener<? extends Future<? super V>> listener) {
            promise.addListener(listener);
            return this;
        }

        @Override
        public RunnableFuture<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
            promise.addListeners(listeners);
            return this;
        }

        @Override
        public RunnableFuture<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener) {
            promise.removeListener(listener);
            return this;

        }

        @Override
        public RunnableFuture<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
            promise.removeListeners(listeners);
            return this;
        }

        @Override
        public RunnableFuture<V> sync() throws InterruptedException {
            promise.sync();
            return this;
        }

        @Override
        public RunnableFuture<V> syncUninterruptibly() {
            promise.syncUninterruptibly();
            return this;
        }

        @Override
        public RunnableFuture<V> await() throws InterruptedException {
            promise.await();
            return this;
        }

        @Override
        public RunnableFuture<V> awaitUninterruptibly() {
            promise.awaitUninterruptibly();
            return this;
        }

        @Override
        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return promise.await(timeout, unit);
        }

        @Override
        public boolean await(long timeoutMillis) throws InterruptedException {
            return promise.await(timeoutMillis);
        }

        @Override
        public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
            return promise.awaitUninterruptibly(timeout, unit);
        }

        @Override
        public boolean awaitUninterruptibly(long timeoutMillis) {
            return promise.awaitUninterruptibly(timeoutMillis);
        }

        @Override
        public V getNow() {
            return promise.getNow();
        }

        @Override
        public void run() {
            try {
                if (promise.setUncancellable()) {
                    V result = task.call();
                    promise.setSuccess(result);
                }
            } catch (Throwable e) {
                promise.setFailure(e);
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return promise.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return promise.isCancelled();
        }

        @Override
        public boolean isDone() {
            return promise.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return promise.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return promise.get(timeout, unit);
        }
    }
}
