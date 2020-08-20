package server;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

import static java.lang.System.out;

public class Center {
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private Map<Integer, String> keys = new HashMap<Integer, String>();
    private Map<String, ArrayList<SocketChannel>> keyGroups = new HashMap<String, ArrayList<SocketChannel>>();
    private ArrayList<Integer> onHandling = new ArrayList<Integer>();

    public Center(int port) {
        this.createConnection(port);
        this.setHandler();
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

    private void setHandler() {
        while (true) {
            try {
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> selectionKeysIterator = selectedKeys.iterator();

                while (selectionKeysIterator.hasNext()) {
                    SelectionKey selectionKey = (SelectionKey) selectionKeysIterator.next();
                    selectionKeysIterator.remove();
                    if (selectionKey.isAcceptable()) {
                        this.setConnectHandler(selectionKey).run();
                    } else if (selectionKey.isReadable() && !onHandling.contains(selectionKey.hashCode())) {
                        new Thread(this.setChannelHandler(selectionKey)).start();
                        // this.setChannelHandler(selectionKey).run();
                    }
                }
            } catch (Exception err) {
                throw new Error("接收階段失敗");
            }
        }
    }

    public Runnable setConnectHandler(SelectionKey selectionKey) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocketChannel ServerSocketChennal = (ServerSocketChannel) selectionKey.channel();
                    SocketChannel socketChannel = ServerSocketChennal.accept().socket().getChannel();
                    // SocketChannel socketChannel = (SocketChannel)selectionKey.channel();

                    if (socketChannel.isConnectionPending()) {
                        socketChannel.finishConnect(); // padding on connection
                    }
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, selectionKey.OP_READ);
                    out.println("[建立連線] " + socketChannel.getRemoteAddress());
                } catch (Exception err) {
                    throw new Error("建立連線失敗");
                }
            }
        };
    }

    public Runnable setChannelHandler(SelectionKey selectionKey) {
        Integer selectionKeyHashCode = selectionKey.hashCode();
        this.onHandling.add(selectionKeyHashCode);

        return new Runnable() {
            @Override
            public void run() {
                SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

                try {
                    if (dispatch(socketChannel, selectionKeyHashCode) == -1) {
                        out.println("[連線中斷] " + socketChannel.getRemoteAddress());
                        socketChannel.close();

                        try {
                            String key = keys.get(selectionKeyHashCode);
                            ArrayList<SocketChannel> keySocketChannels = keyGroups.get(key);
                            keySocketChannels.remove(socketChannel);
                            keys.remove(selectionKeyHashCode);
                            if (keySocketChannels.size() == 0) {
                                keyGroups.remove(key);
                            }
                        } catch (NullPointerException err) {
                            throw new Error("非準則下之流程的傳輸");
                        }
                    }
                } catch (Exception err) {
                    throw new Error("處理階段失敗，可能包含傳輸異常");
                } finally {
                    onHandling.remove(selectionKeyHashCode);
                }
            }
        };
    }

    public int dispatch(SocketChannel socketChannel, Integer selectionKeyHashCode) throws Exception {
        ByteBuffer bufferData = ByteBuffer.allocate(2048);
        int curBufferLeng = socketChannel.read(bufferData);

        if (curBufferLeng == -1 || curBufferLeng == 0) {
            return curBufferLeng;
        }
        bufferData.flip();

        byte prefix = bufferData.get();
        byte channelStatus = Control.UNDEFINED.code;
        for (Control c : Control.values()) {
            if (c.code == prefix) {
                channelStatus = prefix;
            }
        }

        String key;

        try {
            switch (channelStatus) {
                case 0x00:
                    out.println(Control.UNDEFINED.desc);
                    break;

                case 0x01:
                    out.println(Control.NOTHING.desc);
                    break;

                case 0x0A:
                    int keyLeng = bufferData.remaining();
                    byte[] keyByte = new byte[keyLeng];
                    bufferData.get(keyByte, 0, keyLeng);
                    key = new String(keyByte);

                    try {
                        keyGroups.get(key).add(socketChannel);
                        out.println("[Key 登入] " + key);
                    } catch (NullPointerException err) {
                        keyGroups.put(key, new ArrayList<SocketChannel>());
                        keyGroups.get(key).add(socketChannel);
                        out.println("[Key 建立] " + key);
                    }
                    keys.put(selectionKeyHashCode, key);
                    break;

                case 0x0B:
                    byte[] msgCtx = new byte[2048];
                    byte[] OP_MESSAGE = { Control.MESSAGE.code };

                    // -head
                    System.arraycopy(OP_MESSAGE, 0, msgCtx, 0, OP_MESSAGE.length);

                    // -info
                    byte[] clientInfoByte = socketChannel.getRemoteAddress().toString().getBytes("UTF-8");
                    System.arraycopy(clientInfoByte, 0, msgCtx, OP_MESSAGE.length, clientInfoByte.length);

                    byte[] OPByte_SPLIT = { Control.NOTHING.code };

                    // -split
                    System.arraycopy(OPByte_SPLIT, 0, msgCtx, OP_MESSAGE.length + clientInfoByte.length,
                            OPByte_SPLIT.length);

                    // -message
                    int clientMsgByteLeng = bufferData.remaining();
                    byte[] clientMsgByte = new byte[clientMsgByteLeng];
                    bufferData.get(clientMsgByte, 0, clientMsgByteLeng);

                    System.arraycopy(clientMsgByte, 0, msgCtx,
                            OP_MESSAGE.length + clientInfoByte.length + OPByte_SPLIT.length, clientMsgByte.length);

                    key = keys.get(selectionKeyHashCode);

                    out.println("[" + new String(clientInfoByte) + "/" + key + "/傳輸訊息] ");
                    out.println(new String(clientMsgByte));

                    ArrayList<SocketChannel> keySocketChannelsForMessage = keyGroups.get(key);
                    Iterator<SocketChannel> keySocketChannelsForMessageIterator = keySocketChannelsForMessage
                            .iterator();
                    while (keySocketChannelsForMessageIterator.hasNext()) {
                        SocketChannel curSocketChannel = keySocketChannelsForMessageIterator.next();
                        // keySocketChannelsForMessageIterator.remove();
                        if (curSocketChannel == socketChannel) {
                            continue;
                        }
                        curSocketChannel.write(ByteBuffer.wrap(msgCtx));
                    }
                    out.println("[發送對象數] " + (keySocketChannelsForMessage.size() - 1));
                    break;

                case 0x0C:
                    // bufferData.position(0);
                    // socketChannel.write(bufferData);
                    // bufferData.clear();
                    // while (socketChannel.read(bufferData) > 0) {
                    //     bufferData.flip();
                    //     socketChannel.write(bufferData);
                    //     bufferData.clear();
                    // }

                    key = keys.get(selectionKeyHashCode);

                    // out.println("[" + new String(clientInfoByte) + "/" + key + "/傳輸檔案] ");

                    ArrayList<SocketChannel> keySocketChannelsForFile = keyGroups.get(key);
                    Iterator<SocketChannel> keySocketChannelsForFileIterator = keySocketChannelsForFile.iterator();

                    while (keySocketChannelsForFileIterator.hasNext()) {
                        SocketChannel curSocketChannel = keySocketChannelsForFileIterator.next();
                        if (curSocketChannel == socketChannel) {
                            continue;
                        }
                        bufferData.position(0);
                        curSocketChannel.write(bufferData);
                    }
                    bufferData.clear();

                    keySocketChannelsForFileIterator = keySocketChannelsForFile.iterator();

                    while (socketChannel.read(bufferData) > 0) {
                        bufferData.flip();

                        while (keySocketChannelsForFileIterator.hasNext()) {
                            SocketChannel curSocketChannel = keySocketChannelsForFileIterator.next();
                            if (curSocketChannel == socketChannel) {
                                continue;
                            }
                            curSocketChannel.write(bufferData);
                        }

                        bufferData.clear();
                    }
                    // out.println("[發送對象數] " + (keySocketChannelsForFile.size() - 1));
                    break;
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return curBufferLeng;
    }
}