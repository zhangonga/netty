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
package io.netty.example.echo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * Echoes back any received data from a client.
 */
public final class EchoServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        // Configure the server.
        // 创建boss线程组 用于服务端接受客户端的连接。
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 创建worker线程组，用于进行socketChannel的数据读写。
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        // final EchoServerHandler serverHandler = new EchoServerHandler();
        try {
            // ServerBootstrap netty服务端启动类 引导程序。原来的是链式设置，不容易看明白，我给拆开了。
            ServerBootstrap bootstrap = new ServerBootstrap();
            // 设置连接线程组，和数据处理线程组
            bootstrap.group(bossGroup, workerGroup);
            // 设置要被实例化的为 NioServerSocketChannel 类
            bootstrap.channel(NioServerSocketChannel.class);
            // 设置NioServerSocketChannel 的可选项
            bootstrap.option(ChannelOption.SO_BACKLOG, 100);
            // 设置处理器， LoggingHandler，netty提供的handler。它继承ChannelDuplexHandler，是一个双向处理器，用来打印日志。
            bootstrap.handler(new LoggingHandler(LogLevel.INFO));
            // ChannelInitializer用来初始化连入服务端的channel
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                        // 设置连入服务端的client的socketChannel 的处理器。
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {

                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            //p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(new EchoServerHandler());
                        }
                    });

            // Start the server.
            // 启动服务器，并阻塞等待连接成功。
            ChannelFuture f = bootstrap.bind(PORT).sync();

            // Wait until the server socket is closed.
            // 监听客户端关闭，并阻塞等待。
            f.channel().closeFuture().sync();
        } finally {

            // 两个线程组的优雅关闭
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
