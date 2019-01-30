package io.netty.example.nio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 基于nio的服务器
 * <p>
 *
 * @author: 张弓
 * @date: 2019/1
 * @version: 1.0.0
 */
public class NioEchoServer {

    private final int port = 8888;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    public void start() throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.configureBlocking(false);
        this.serverSocketChannel.socket().bind(new InetSocketAddress(port));

        this.selector = Selector.open();
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("nio 服务启动");

        handleKeys();
    }

    private void handleKeys() throws IOException {
        // 死循环，保持程序一直运行
        while (true) {
            int selectNums = this.selector.select(30 * 1000L);
            if (selectNums == 0) {
                continue;
            }

            System.out.println("选择 channel 数量 : " + selectNums);

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (!key.isValid()) {
                    continue;
                }

                handleKey(key);
            }
        }
    }

    private void handleKey(SelectionKey key) throws IOException {
        // 连接就绪
        if (key.isAcceptable()) {
            performAcceptableKey(key);
        }
        // 读就绪
        if (key.isReadable()) {
            performReadableKey(key);
        }
        // 写就绪
        if (key.isWritable()) {
            performWritableKey(key);
        }
    }


    private void performAcceptableKey(SelectionKey key) throws IOException {
        SocketChannel clientSocketChannel = ((ServerSocketChannel) key.channel()).accept();
        clientSocketChannel.configureBlocking(false);
        System.out.println("接受新的 channel");
        clientSocketChannel.register(this.selector, SelectionKey.OP_READ, new ArrayList<String>());
    }

    private void performReadableKey(SelectionKey key) throws ClosedChannelException {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();

        ByteBuffer readBuffer = NioCodecUtil.read(clientSocketChannel);
        if(readBuffer == null){
            System.out.println("断开 channel");
            clientSocketChannel.register(this.selector, 0);
            return;
        }

        if(readBuffer.position() > 0){
            String content = NioCodecUtil.newString(readBuffer);
            System.out.println("读取数据 : " + content);
            List<String> responseQueue = (List<String>) key.attachment();

            responseQueue.add("响应 ：" + content);
            clientSocketChannel.register(selector, SelectionKey.OP_WRITE, key.attachment());
        }
    }

    private void performWritableKey(SelectionKey key) throws ClosedChannelException {

        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        List<String> responseQueue = (List<String>) key.attachment();
        for(String content : responseQueue){
            System.out.println("写入数据 : " + content);
            NioCodecUtil.write(clientSocketChannel, content);
        }

        responseQueue.clear();
        clientSocketChannel.register(this.selector, SelectionKey.OP_READ, responseQueue);
    }

    public static void main(String[] args) throws IOException {
        NioEchoServer server = new NioEchoServer();
        server.start();
    }
}
