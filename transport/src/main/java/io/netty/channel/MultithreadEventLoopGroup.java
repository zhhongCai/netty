/*
 * Copyright 2012 The Netty Project
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
package io.netty.channel;

import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.RejectedExecutionHandlers;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * Abstract base class for {@link EventLoopGroup} implementations that handles their tasks with multiple threads at
 * the same time.
 */
public class MultithreadEventLoopGroup extends MultithreadEventExecutorGroup implements EventLoopGroup {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultithreadEventLoopGroup.class);

    public static final int DEFAULT_EVENT_LOOP_THREADS;

    static {
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.eventLoopThreads: {}", DEFAULT_EVENT_LOOP_THREADS);
        }
    }

    public MultithreadEventLoopGroup(SingleThreadEventLoop.IoHandlerFactory ioHandlerFactory) {
        this(0, (Executor) null, ioHandlerFactory);
    }

    public MultithreadEventLoopGroup(int nThreads, SingleThreadEventLoop.IoHandlerFactory ioHandlerFactory) {
        this(nThreads, (Executor) null, ioHandlerFactory);
    }

    public MultithreadEventLoopGroup(int nThreads, Executor executor,
                                     SingleThreadEventLoop.IoHandlerFactory ioHandlerFactory) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor,
                SingleThreadEventLoop.DEFAULT_MAX_PENDING_TASKS, RejectedExecutionHandlers.reject(), ioHandlerFactory);
    }

    public MultithreadEventLoopGroup(int nThreads, ThreadFactory threadFactory,
                                     SingleThreadEventLoop.IoHandlerFactory ioHandlerFactory) {
        this(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, threadFactory,
                SingleThreadEventLoop.DEFAULT_MAX_PENDING_TASKS, RejectedExecutionHandlers.reject(), ioHandlerFactory);
    }

    public MultithreadEventLoopGroup(int nThreads, Executor executor,  int maxPendingTasks,
                                        RejectedExecutionHandler rejectedExecutionHandler,
                                        SingleThreadEventLoop.IoHandlerFactory ioHandlerFactory) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor,
                maxPendingTasks, rejectedExecutionHandler, ioHandlerFactory);
    }

    public MultithreadEventLoopGroup(int nThreads, ThreadFactory threadFactory, int maxPendingTasks,
                                     RejectedExecutionHandler rejectedExecutionHandler,
                                     SingleThreadEventLoop.IoHandlerFactory ioHandlerFactory) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, threadFactory,
                maxPendingTasks, rejectedExecutionHandler, ioHandlerFactory);
    }

    @Override
    protected ThreadFactory newDefaultThreadFactory() {
        return new DefaultThreadFactory(getClass(), Thread.MAX_PRIORITY);
    }

    @Override
    public final EventLoop next() {
        return (EventLoop) super.next();
    }

    @Override
    protected final EventLoop newChild(Executor executor, int maxPendingTasks,
                                       RejectedExecutionHandler rejectedExecutionHandler, Object... args)
            throws Exception {
        return newChild(executor, maxPendingTasks, rejectedExecutionHandler,
                ((SingleThreadEventLoop.IoHandlerFactory) args[0]).newHandler(),
                Arrays.copyOfRange(args, 1, args.length));
    }

    protected EventLoop newChild(Executor executor, int maxPendingTasks,
                                          RejectedExecutionHandler rejectedExecutionHandler,
                                          SingleThreadEventLoop.IoHandler handler, Object... args) throws Exception {
        assert args.length == 0;
        return new SingleThreadEventLoop(this, executor, handler, maxPendingTasks, rejectedExecutionHandler);
    }

}
