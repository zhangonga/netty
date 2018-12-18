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

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import io.netty.util.internal.StringUtil;

import java.util.concurrent.Executor;

class LocalEventLoop extends SingleThreadEventExecutor implements EventLoop {

    static final int DEFAULT_MAX_PENDING_EXECUTOR_TASKS = SingleThreadEventExecutor.DEFAULT_MAX_PENDING_EXECUTOR_TASKS;

    private static LocalChannelUnsafe cast(Channel channel) {
        Channel.Unsafe unsafe = channel.unsafe();
        if (unsafe instanceof LocalChannelUnsafe) {
            return (LocalChannelUnsafe) unsafe;
        }
        throw new IllegalArgumentException("Channel of type " + StringUtil.simpleClassName(channel) + " not supported");
    }

    private final Unsafe unsafe = new Unsafe() {
        @Override
        public void register(Channel channel) {
            assert inEventLoop();
            cast(channel).register0(LocalEventLoop.this);
        }

        @Override
        public void deregister(Channel channel) {
            assert inEventLoop();
            cast(channel).deregister0(LocalEventLoop.this);
        }
    };

    LocalEventLoop(EventLoopGroup parent, Executor executor,
                   int maxTasks, RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, executor, maxTasks, rejectedExecutionHandler);
    }

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    @Override
    public EventLoopGroup parent() {
        return (EventLoopGroup) super.parent();
    }

    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }
}
