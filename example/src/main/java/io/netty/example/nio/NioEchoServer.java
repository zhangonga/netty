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

    /**
     * SocketChannel tcp client
     * ServerSocketChannel tcp server
     * DataGramChannel udp client | server
     * FileChannel file read | write 这个channel只能使用阻塞模式。
     */
    private final int port = 8888;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    public void start() throws IOException {
        // 获取一个服务端channel，用来监听accept事件，serverSocketChannel监听到accept事件，会生成一个socketChannel。
        this.serverSocketChannel = ServerSocketChannel.open();
        // 非阻塞
        this.serverSocketChannel.configureBlocking(false);
        // 绑定本地端口
        this.serverSocketChannel.socket().bind(new InetSocketAddress(port));

        // 获取selector
        this.selector = Selector.open();
        // 注册serverSocketChannel到selector，并且只对接受连接感兴趣。
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("nio 服务启动");

        handleKeys();
    }

    private void handleKeys() throws IOException {
        // 死循环，保持程序一直运行
        while (true) {
            // 轮询查询是否有就绪的channel。没有返回0
            int selectNums = this.selector.select(30 * 1000L);
            if (selectNums == 0) {
                continue;
            }

            System.out.println("就绪channel数量 : " + selectNums);

            // 获取可操作的 Channel, 返回就绪的SelectionKey集合
            // SelectionKey 包含 感兴趣的事件集合，就绪的事件集合，channel，selector，attachment可选的附加对象。
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (!key.isValid()) {
                    continue;
                }

                //
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
        // 接收到新连接后，会生成socketChannel，并把socketChannel注册读事件到selector
        clientSocketChannel.register(this.selector, SelectionKey.OP_READ, new ArrayList<String>());
    }

    private void performReadableKey(SelectionKey key) throws ClosedChannelException {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();

        ByteBuffer readBuffer = NioCodecUtil.read(clientSocketChannel);
        if (readBuffer == null) {
            System.out.println("断开 channel");
            clientSocketChannel.register(this.selector, 0);
            return;
        }

        if (readBuffer.position() > 0) {
            String content = NioCodecUtil.newString(readBuffer);
            System.out.println("读取数据 : " + content);
            // attachment 可选的附加对象，把接收的信息，作为附加对象放到write事件中，write事件的处理方法再拿到发出去。
            String recvContent = "recv: " + content;
            // 这一步，其实可以直接把recvContent作为参数注册进去，这样是为了验证#attach(...)和#attachment()
            key.attach(recvContent);
            clientSocketChannel.register(selector, SelectionKey.OP_WRITE, key.attachment());
        }
    }

    private void performWritableKey(SelectionKey key) throws ClosedChannelException {

        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        String recvContent = (String) key.attachment();

        System.out.println("写入数据 : " + recvContent);
        NioCodecUtil.write(clientSocketChannel, recvContent);

        clientSocketChannel.register(this.selector, SelectionKey.OP_READ);
    }

    public static void main(String[] args) throws IOException {
        NioEchoServer server = new NioEchoServer();
        server.start();
    }
}
