package io.netty.example.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

/**
 * nio 客户端
 *
 * @author zhanggong
 */
public class NioEchoClient {

    private static final int serverPort = 8888;
    private SocketChannel clientSocketChannel;
    private Selector selector;

    private CountDownLatch connected = new CountDownLatch(1);

    public NioEchoClient() throws IOException, InterruptedException {
        if (this.clientSocketChannel == null) {
            this.clientSocketChannel = SocketChannel.open();
            this.clientSocketChannel.configureBlocking(false);

            selector = Selector.open();
            clientSocketChannel.register(selector, SelectionKey.OP_CONNECT);
            clientSocketChannel.connect(new InetSocketAddress(serverPort));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        handleKeys();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            if (connected.getCount() != 0) {
                connected.await();
            }
            System.out.println("client 启动完成");
        }
    }

    @SuppressWarnings("Duplicates")
    private void handleKeys() throws IOException {

        while (true) {
            int selectNums = selector.select(30 * 1000L);
            if (selectNums == 0) {
                continue;
            }

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                // 移除下面要处理的 SelectionKey
                iterator.remove();
                // 忽略无效的 SelectionKey
                if (!key.isValid()) {
                    continue;
                }

                handleKey(key);
            }

        }
    }

    private void handleKey(SelectionKey key) throws IOException {
        // 接受连接就绪
        if (key.isConnectable()) {
            handleConnectableKey(key);
        }
        // 读就绪
        if (key.isReadable()) {
            handleReadableKey(key);
        }
        // 写就绪
        if (key.isWritable()) {
            handleWritableKey(key);
        }

    }

    private void handleWritableKey(SelectionKey key) throws ClosedChannelException {
        // Client Socket Channel
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();

        String content = (String) key.attachment();
        // 打印数据
        System.out.println("写入数据：" + content);
        NioCodecUtil.write(clientSocketChannel, content);

        // 注册 Client Socket Channel 到 Selector
        clientSocketChannel.register(selector, SelectionKey.OP_READ);
    }

    private void handleReadableKey(SelectionKey key) {
        // Client Socket Channel
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        // 读取数据
        ByteBuffer readBuffer = NioCodecUtil.read(clientSocketChannel);
        // 打印数据
        if (readBuffer.position() > 0) {
            // 写入模式下
            String content = NioCodecUtil.newString(readBuffer);
            System.out.println("读取数据：" + content);
        }
    }

    private void handleConnectableKey(SelectionKey key) throws IOException {

        // 完成连接
        if (!clientSocketChannel.isConnectionPending()) {
            return;
        }
        clientSocketChannel.finishConnect();
        // log
        System.out.println("接受新的 Channel");
        // 注册 Client Socket Channel 到 Selector
        clientSocketChannel.register(selector, SelectionKey.OP_READ);
        // 标记为已连接
        connected.countDown();
    }

    private synchronized void send(String content) throws ClosedChannelException {
        System.out.println("写入数据：" + content);
        // 注册 Client Socket Channel 到 Selector
        clientSocketChannel.register(selector, SelectionKey.OP_WRITE, content);
        selector.wakeup();
    }

    public static void main(String[] args) {
        try {
            NioEchoClient client = new NioEchoClient();
            for (int i = 0; i < 10; i++) {
                client.send("nihao" + i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
