package server;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;

import static java.lang.System.out;

import packager.CsocketServer;
import packager.State;
import packager.server.*;

public class Center implements CsocketServer {
    public ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ArrayList<Integer> lockingKeys;
    private Key key;

    public Center() {
        this.lockingKeys = new ArrayList<Integer>();
        this.key = new Key();
    }

    public void createConnection(int port) {
        try {
            this.selector = Selector.open();
            this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel.configureBlocking(false);
            this.serverSocketChannel.bind(new InetSocketAddress(port));
            this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
            this.serverSocketChannel.socket().setReceiveBufferSize(52428800);
        } catch (Exception err) {
            throw new Error("初始化連線建立失敗");
        }
    }

    public void setMainHandler() {
        while (true) {
            try {
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> selectionKeysIterator = selectedKeys.iterator();

                while (selectionKeysIterator.hasNext()) {
                    SelectionKey selectionKey = (SelectionKey) selectionKeysIterator.next();
                    selectionKeysIterator.remove();
                    if (!selectionKey.isValid()) {
                        continue;
                    }
                    if (selectionKey.isAcceptable()) {
                        this.setConnectHandler(selectionKey);
                    } else if (selectionKey.isReadable() && !lockingKeys.contains(selectionKey.hashCode())) {
                        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                        Integer targetKey = selectionKey.hashCode();
                        new Thread(this.handler(socketChannel, targetKey)).start();
                    }
                }
            } catch (Exception err) {
                throw new Error("接收階段失敗");
            }
        }
    }

    public void setConnectHandler(SelectionKey selectionKey) {
        try {
            ServerSocketChannel ServerSocketChennal = (ServerSocketChannel) selectionKey.channel();
            SocketChannel socketChannel = ServerSocketChennal.accept().socket().getChannel();

            // if (socketChannel.isConnectionPending()) {
            //     socketChannel.finishConnect(); // padding on connection
            // }
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, selectionKey.OP_READ);
            out.println("[建立連線] " + socketChannel.getRemoteAddress());
        } catch (Exception err) {
            throw new Error("建立連線失敗");
        }
    }

    public Runnable handler(SocketChannel socketChannel, Integer targetKey) {
        this.lockingKeys.add(targetKey);

        return new Runnable() {
            @Override
            public void run() {
                try {
                    if (dispatch(socketChannel, targetKey) == -1) {
                        out.println("[連線中斷] " + socketChannel.getRemoteAddress());
                        socketChannel.close();

                        key.remove(targetKey, socketChannel);
                    }
                } catch (Exception err) {
                    throw new Error("處理階段失敗，可能包含傳輸異常");
                } finally {
                    lockingKeys.remove(targetKey);
                }
            }
        };
    }

    public int dispatch(SocketChannel socketChannel, Integer targetKey) throws Exception {
        String clientRemoteAddress = socketChannel.getRemoteAddress().toString();

        ByteBuffer byteBuffer = ByteBuffer.allocate(2048);
        int curBufferLeng = socketChannel.read(byteBuffer);

        if (curBufferLeng == -1 || curBufferLeng == 0) {
            return curBufferLeng;
        }
        byteBuffer.flip();

        byte prefix = byteBuffer.get();
        byte channelStatus = State.UNDEFINED.code;
        for (State c : State.values()) {
            if (c.code == prefix) {
                channelStatus = prefix;
            }
        }

        try {
            switch (channelStatus) {
                case 0x00:
                    out.println(State.UNDEFINED.desc);
                    break;

                case 0x01:
                    out.println(State.NOTHING.desc);
                    break;

                case 0x0A:
                    // key.groups;
                    this.key.handle(byteBuffer, socketChannel, targetKey);
                    break;

                case 0x0B:
                    Message.handle(byteBuffer, socketChannel, targetKey, this.key, clientRemoteAddress);
                    break;

                case 0x0C:
                    File.handle(byteBuffer, socketChannel, targetKey, this.key);
                    break;
                case 0x0D:
                    Remote.handle(byteBuffer, socketChannel, targetKey, this.key);
                    break;
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return curBufferLeng;
    }
}