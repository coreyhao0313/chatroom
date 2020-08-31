package server;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
// import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import static java.lang.System.out;

import base.CsocketServer;
import base.State;
import packager.server.*;
import packager.Parser;

public class Center implements CsocketServer {
    public ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ArrayList<Integer> lockingKeys;
    private Key key;
    public Map<Integer, Parser> targetPackages;

    public Center() {
        this.lockingKeys = new ArrayList<Integer>();
        this.key = new Key();
        this.targetPackages = new HashMap<Integer, Parser>();
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
                        // new Thread(this.handler(socketChannel, targetKey)).start();
                        this.handler(socketChannel, targetKey).run();
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
                    err.printStackTrace();
                    throw new Error("處理階段失敗，可能包含傳輸異常");
                } finally {
                    lockingKeys.remove(targetKey);
                }
            }
        };
    }

    public int dispatch(SocketChannel socketChannel, Integer targetKey) throws Exception {
        Parser pkg;
        if((pkg = this.targetPackages.get(targetKey)) == null){
            pkg = new Parser(4096);
            this.targetPackages.put(targetKey, pkg);
            pkg.fetchToSetHead(socketChannel);
        }
        
        try {
            switch (pkg.type) {
                case 0x00:
                    // out.println(State.UNDEFINED.desc);
                    break;

                case 0x01:
                    // out.println(State.NOTHING.desc);
                    break;

                case 0x0A:
                    // this.key.handle(byteBuffer, socketChannel, targetKey);
                    break;

                case 0x0B:
                    // String clientRemoteAddress = socketChannel.getRemoteAddress().toString();
                    // Message.handle(byteBuffer, socketChannel, targetKey, this.key, clientRemoteAddress);
                    break;

                case 0x0C:
                    File.handle(pkg, socketChannel, targetKey, this.key);
                    break;
                case 0x0D:
                    // Remote.handle(byteBuffer, socketChannel, targetKey, this.key);
                    break;
            }
        } catch (Exception err) {
            err.printStackTrace();
        } finally {
            if(pkg.hasDone()){
                targetPackages.remove(targetKey);
            }
        }
        return pkg.readLeng;
    }
}