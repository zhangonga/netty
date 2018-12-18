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
package io.netty.channel.local;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.RejectedExecutionHandlers;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * {@link MultithreadEventLoopGroup} which must be used for the local transport.
 */
public class LocalEventLoopGroup extends MultithreadEventExecutorGroup implements EventLoopGroup {

    /**
     * Create a new instance with the default number of threads.
     */
    public LocalEventLoopGroup() {
        this(0);
    }

    /**
     * Create a new instance
     *
     * @param nThreads          the number of threads to use
     */
    public LocalEventLoopGroup(int nThreads) {
        this(nThreads, (ThreadFactory) null, LocalEventLoop.DEFAULT_MAX_PENDING_EXECUTOR_TASKS,
                RejectedExecutionHandlers.reject());
    }

    /**
     * Create a new instance
     *
     * @param nThreads          the number of threads to use
     * @param threadFactory     the {@link ThreadFactory} or {@code null} to use the default
     * @param maxPendingTasks   the maximum number of pending tasks before new tasks will be rejected.
     * @param rejectedHandler   the {@link RejectedExecutionHandler} to use.
     */
    public LocalEventLoopGroup(int nThreads, ThreadFactory threadFactory, int maxPendingTasks,
                               RejectedExecutionHandler rejectedHandler) {
        super(nThreads == 0 ? MultithreadEventLoopGroup.DEFAULT_EVENT_LOOP_THREADS : nThreads,
                threadFactory, maxPendingTasks, rejectedHandler);
    }

    /**
     * Create a new instance
     *
     * @param nThreads          the number of threads to use
     * @param executor          the Executor to use, or {@code null} if the default should be used.
     * @param maxPendingTasks   the maximum number of pending tasks before new tasks will be rejected.
     * @param rejectedHandler   the {@link RejectedExecutionHandler} to use.
     */
    public LocalEventLoopGroup(int nThreads, Executor executor, int maxPendingTasks,
                               RejectedExecutionHandler rejectedHandler) {
        super(nThreads == 0 ? MultithreadEventLoopGroup.DEFAULT_EVENT_LOOP_THREADS : nThreads,
                executor, maxPendingTasks, rejectedHandler);
    }

    /**
     * Create a new instance
     *
     * @param nThreads          the number of threads to use
     * @param threadFactory     the {@link ThreadFactory} or {@code null} to use the default
     */
    public LocalEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        this(nThreads, threadFactory,
                LocalEventLoop.DEFAULT_MAX_PENDING_EXECUTOR_TASKS, RejectedExecutionHandlers.reject());
    }

    /**
     * Create a new instance
     *
     * @param nThreads          the number of threads to use
     * @param executor          the Executor to use, or {@code null} if the default should be used.
     */
    public LocalEventLoopGroup(int nThreads, Executor executor) {
        this(nThreads, executor,
                LocalEventLoop.DEFAULT_MAX_PENDING_EXECUTOR_TASKS, RejectedExecutionHandlers.reject());
    }

    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }

    @Override
    protected EventExecutor newChild(Executor executor, int maxPendingTasks,
                                     RejectedExecutionHandler rejectedExecutionHandler,
                                     Object... args) {
        assert args.length == 0;
        return new LocalEventLoop(this, executor, maxPendingTasks, rejectedExecutionHandler);
    }
}
