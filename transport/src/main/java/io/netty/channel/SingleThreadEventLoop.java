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

import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.RejectedExecutionHandlers;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * {@link EventLoop} that execute all its submitted tasks in a single thread and uses an {@link IoHandler} for
 * IO processing.
 */
public class SingleThreadEventLoop extends SingleThreadEventExecutor implements EventLoop {

    public static final int DEFAULT_MAX_PENDING_TASKS = Math.max(16,
            SystemPropertyUtil.getInt("io.netty.eventLoop.maxPendingTasks", Integer.MAX_VALUE));

    // TODO: Is this a sensible default ?
    protected static final int DEFAULT_MAX_TASKS_PER_RUN = Math.max(1,
            SystemPropertyUtil.getInt("io.netty.eventLoop.maxTaskPerRun", 1024 * 4));

    private final ExecutionContext context = new ExecutionContext() {
        @Override
        public boolean isTaskReady() {
            assert inEventLoop();
            return SingleThreadEventLoop.this.hasTasks() || SingleThreadEventLoop.this.hasScheduledTasks();
        }

        @Override
        public long delayNanos(long currentTimeNanos) {
            assert inEventLoop();
            return SingleThreadEventLoop.this.delayNanos(currentTimeNanos);
        }

        @Override
        public long deadlineNanos() {
            assert inEventLoop();
            return SingleThreadEventLoop.this.deadlineNanos();
        }

        @Override
        public boolean isShuttingDown() {
            assert inEventLoop();
            return SingleThreadEventLoop.this.isShuttingDown();
        }
    };

    private final Unsafe unsafe = new Unsafe() {
        @Override
        public void register(Channel channel) throws Exception {
            SingleThreadEventLoop.this.register(channel);
        }

        @Override
        public void deregister(Channel channel) throws Exception {
            SingleThreadEventLoop.this.deregister(channel);
        }
    };

    private final IoHandler ioHandler;
    private final int maxTasksPerRun;

    /**
     * Create a new instance
     *
     * @param parent            the {@link EventLoopGroup} which is the parent of this instance and belongs to it
     * @param threadFactory     the {@link ThreadFactory} which will be used for the used {@link Thread}
     * @param ioHandler         the {@link IoHandler} to use.
     */
    public SingleThreadEventLoop(EventLoopGroup parent, ThreadFactory threadFactory, IoHandler ioHandler) {
        this(parent, threadFactory, ioHandler, DEFAULT_MAX_PENDING_TASKS, RejectedExecutionHandlers.reject());
    }

    /**
     * Create a new instance
     *
     * @param parent            the {@link EventLoopGroup} which is the parent of this instance and belongs to it
     * @param executor          the {@link Executor} which will be used to run this {@link EventLoop}.
     * @param ioHandler         the {@link IoHandler} to use.
     */
    public SingleThreadEventLoop(EventLoopGroup parent, Executor executor, IoHandler ioHandler) {
        this(parent, executor, ioHandler, DEFAULT_MAX_PENDING_TASKS, RejectedExecutionHandlers.reject());
    }

    /**
     * Create a new instance
     *
     * @param parent            the {@link EventLoopGroup} which is the parent of this instance and belongs to it
     * @param threadFactory     the {@link ThreadFactory} which will be used for the used {@link Thread}
     * @param ioHandler         the {@link IoHandler} to use.
     * @param maxPendingTasks   the maximum number of pending tasks before new tasks will be rejected.
     * @param rejectedHandler   the {@link RejectedExecutionHandler} to use.
     */
    public SingleThreadEventLoop(EventLoopGroup parent, ThreadFactory threadFactory,
                                 IoHandler ioHandler, int maxPendingTasks,
                                 RejectedExecutionHandler rejectedHandler) {
        this(parent, threadFactory, ioHandler, maxPendingTasks, rejectedHandler, DEFAULT_MAX_TASKS_PER_RUN);
    }

    /**
     * Create a new instance
     *
     * @param parent            the {@link EventLoopGroup} which is the parent of this instance and belongs to it
     * @param executor          the {@link Executor} which will be used to run this {@link EventLoop}.
     * @param ioHandler         the {@link IoHandler} to use.
     * @param maxPendingTasks   the maximum number of pending tasks before new tasks will be rejected.
     * @param rejectedHandler   the {@link RejectedExecutionHandler} to use.
     */
    public SingleThreadEventLoop(EventLoopGroup parent, Executor executor,
                                 IoHandler ioHandler, int maxPendingTasks,
                                 RejectedExecutionHandler rejectedHandler) {
        this(parent, executor, ioHandler, maxPendingTasks, rejectedHandler, DEFAULT_MAX_TASKS_PER_RUN);
    }

    /**
     * Create a new instance
     *
     * @param parent            the {@link EventLoopGroup} which is the parent of this instance and belongs to it
     * @param threadFactory     the {@link ThreadFactory} which will be used for the used {@link Thread}
     * @param ioHandler         the {@link IoHandler} to use.
     * @param maxPendingTasks   the maximum number of pending tasks before new tasks will be rejected.
     * @param rejectedHandler   the {@link RejectedExecutionHandler} to use.
     * @param maxTasksPerRun    the maximum number of tasks per {@link EventLoop} run that will be processed
     *                          before trying to handle IO again.
     */
    public SingleThreadEventLoop(EventLoopGroup parent, ThreadFactory threadFactory,
                                 IoHandler ioHandler, int maxPendingTasks,
                                 RejectedExecutionHandler rejectedHandler, int maxTasksPerRun) {
        super(parent, threadFactory, maxPendingTasks, rejectedHandler);
        this.ioHandler = ObjectUtil.checkNotNull(ioHandler, "ioHandler");
        this.maxTasksPerRun = ObjectUtil.checkPositive(maxTasksPerRun, "maxTasksPerRun");
    }

    /**
     * Create a new instance
     *
     * @param parent            the {@link EventLoopGroup} which is the parent of this instance and belongs to it
     * @param executor          the {@link Executor} which will be used to run this {@link EventLoop}.
     * @param ioHandler         the {@link IoHandler} to use.
     * @param maxPendingTasks   the maximum number of pending tasks before new tasks will be rejected.
     * @param rejectedHandler   the {@link RejectedExecutionHandler} to use.
     * @param maxTasksPerRun    the maximum number of tasks per {@link EventLoop} run that will be processed
     *                          before trying to handle IO again.
     */
    public SingleThreadEventLoop(EventLoopGroup parent, Executor executor,
                                 IoHandler ioHandler, int maxPendingTasks,
                                 RejectedExecutionHandler rejectedHandler, int maxTasksPerRun) {
        super(parent, executor, maxPendingTasks, rejectedHandler);
        this.ioHandler = ObjectUtil.checkNotNull(ioHandler, "ioHandler");
        this.maxTasksPerRun = ObjectUtil.checkPositive(maxTasksPerRun, "maxTasksPerRun");
    }

    @Override
    protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
        // This event loop never calls takeTask()
        return maxPendingTasks == Integer.MAX_VALUE ? PlatformDependent.<Runnable>newMpscQueue()
                : PlatformDependent.<Runnable>newMpscQueue(maxPendingTasks);
    }

    @Override
    public final EventLoopGroup parent() {
        return (EventLoopGroup) super.parent();
    }

    @Override
    public final EventLoop next() {
        return this;
    }

    @Override
    protected final boolean wakesUpForTask(Runnable task) {
        return !(task instanceof NonWakeupRunnable);
    }

    /**
     * Marker interface for {@link Runnable} that will not trigger an {@link #wakeup(boolean)} in all cases.
     */
    interface NonWakeupRunnable extends Runnable { }

    @Override
    public final Unsafe unsafe() {
        return unsafe;
    }

    // Methods that a user can override to easily add instrumentation and other things.

    @Override
    protected void run() {
        assert inEventLoop();
        do {
            runIo();
            runAllTasks(maxTasksPerRun);
        } while (!confirmShutdown());
    }

    /**
     * Called when IO will be processed for all the {@link Channel}s on this {@link SingleThreadEventLoop}.
     * This method returns the number of {@link Channel}s for which IO was processed.
     *
     * This method must be called from the {@link EventLoop} thread.
     */
    protected int runIo() {
        assert inEventLoop();
        return ioHandler.run(context);
    }

    /**
     * Called once a {@link Channel} should be registered on this {@link SingleThreadEventLoop}.
     *
     * This method must be called from the {@link EventLoop} thread.
     */
    protected void register(Channel channel) throws Exception {
        assert inEventLoop();
        ioHandler.register(channel);
    }

    /**
     * Called once a {@link Channel} should be deregistered from this {@link SingleThreadEventLoop}.
     */
    protected void deregister(Channel channel) throws Exception {
        assert inEventLoop();
        ioHandler.deregister(channel);
    }

    @Override
    protected final void wakeup(boolean inEventLoop) {
        ioHandler.wakeup(inEventLoop);
    }

    @Override
    protected final void cleanup() {
        assert inEventLoop();
        ioHandler.destroy();
    }

    /**
     * The execution context for an {@link IoHandler}.
     * All method must be called from the {@link EventLoop} thread.
     */
    public interface ExecutionContext {
        /**
         * Returns {@code true} if the {@link EventLoop} contains at least one task that is ready to be
         * processed without blocking. This can either be normal task or scheduled task that is ready now.
         */
        boolean isTaskReady();

        /**
         * Returns the amount of time left until the scheduled task with the closest dead line should run..
         */
        long delayNanos(long currentTimeNanos);

        /**
         * Returns the absolute point in time (relative to {@link #nanoTime()}) at which the the next
         * closest scheduled task should run.
         */
        long deadlineNanos();

        /**
         * Returns {@code true} of the {@link EventLoop} on which the {@link IoHandler} runs is shutting down.
         */
        boolean isShuttingDown();
    }

    /**
     * Factory for {@link IoHandler} instances.
     */
    public interface IoHandlerFactory {

        /**
         * Creates a new {@link IoHandler} instance.
         */
        IoHandler newHandler();
    }

    /**
     * Handles IO dispatching on a {@link SingleThreadEventLoop}.
     * All operations except {@link #wakeup(boolean)} <strong>MUST</strong> be executed
     * on the {@link EventLoop} thread and should never be called from the user-directly.
     */
    public interface IoHandler extends Unsafe {
        /**
         * Run the IO handled by this {@link IoHandler}. The {@link ExecutionContext} should be used
         * to ensure we not execute too long and so block the processing of other task that are
         * scheduled on the {@link EventLoop}. This is done by taking {@link ExecutionContext#delayNanos(long)} or
         * {@link ExecutionContext#deadlineNanos()} into account.
         *
         * @return the number of {@link Channel} for which I/O was handled.
         */
        int run(ExecutionContext runner);

        /**
         * Wakeup the {@link IoHandler}, which means if any operation blocks it should be unblocked and
         * return as soon as possible.
         */
        void  wakeup(boolean inEventLoop);

        /**
         * Destroy the {@link IoHandler} and free all its resources.
         */
        void destroy();
    }
}
