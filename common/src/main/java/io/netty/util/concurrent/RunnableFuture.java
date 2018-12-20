package io.netty.util.concurrent;

public interface RunnableFuture<V> extends java.util.concurrent.RunnableFuture<V>, Future<V> {

    @Override
    RunnableFuture<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    RunnableFuture<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    RunnableFuture<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    RunnableFuture<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    RunnableFuture<V> sync() throws InterruptedException;

    @Override
    RunnableFuture<V> syncUninterruptibly();

    @Override
    RunnableFuture<V> await() throws InterruptedException;

    @Override
    RunnableFuture<V> awaitUninterruptibly();
}
