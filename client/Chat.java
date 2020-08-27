package client;

import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import static java.lang.System.out;

import packager.CsocketClient;
import packager.State;
import packager.client.*;

public class Chat implements CsocketClient {
    public SocketChannel socketChannel;
    private Selector selector;
    private byte channelStatus = State.UNDEFINED.code;
    private Remote remote;
    private File file;
    private Key key;

    public Chat() {
        this.remote = new Remote();
        this.file = new File();
        this.key = new Key();
    }

    public void createConnection(String address, int port) {
        try {
            this.selector = Selector.open();
            this.socketChannel = SocketChannel.open();
            this.socketChannel.configureBlocking(false);
            this.socketChannel.connect(new InetSocketAddress(address, port));
            this.socketChannel.register(this.selector, SelectionKey.OP_CONNECT);
            this.socketChannel.socket().setSendBufferSize(52428800); // 52428800
            this.socketChannel.socket().setReceiveBufferSize(52428800);
        } catch (Exception err) {
            out.println("初始化連線建立失敗");
        }
    }

    public void setMainHandler() {
        while (true) {
            try {
                this.selector.select();

                Set<SelectionKey> selectionKeys = this.selector.selectedKeys();
                Iterator<SelectionKey> selectionKeysIterator = selectionKeys.iterator();

                while (selectionKeysIterator.hasNext()) {
                    SelectionKey selectionKey = (SelectionKey) selectionKeysIterator.next();
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    selectionKeysIterator.remove();
                    if (!selectionKey.isValid()) {
                        continue;
                    }
                    if (selectionKey.isConnectable()) {
                        this.setConnectHandler(socketChannel);
                        new Thread(this.setInputHandler(socketChannel)).start();
                    } else if (selectionKey.isReadable()) {
                        this.handler(socketChannel);
                    }
                }
            } catch (Exception err) {
                throw new Error("接收階段失敗");
            }
        }
    }

    public void setConnectHandler(SocketChannel socketChannel) {
        try {
            if (socketChannel.isConnectionPending()) {
                socketChannel.finishConnect();
            }
            socketChannel.configureBlocking(false);
            socketChannel.register(this.selector, SelectionKey.OP_READ);
            // socketChannel.register(this.selector, SelectionKey.OP_WRITE);
            out.println("[連線] " + socketChannel.getRemoteAddress());
        } catch (Exception err) {
            throw new Error("連線失敗");
        }
    }

    public void handler(SocketChannel socketChannel) {
        try {
            switch (this.dispatch(socketChannel)) {
                case -1:
                    out.println("[連線中斷] " + socketChannel.getRemoteAddress());
                    socketChannel.close();
                    this.channelStatus = State.UNDEFINED.code;
                    System.exit(0);
                    break;

                case 0:
                    // 無動作
                    break;
            }
        } catch (Exception err) {
            throw new Error("處理階段失敗，可能包含傳輸異常");
        }
    }

    public Runnable setInputHandler(SocketChannel socketChannel) {
        InputStreamReader ISR = new InputStreamReader(System.in);
        BufferedReader BR = new BufferedReader(ISR);
        return new Runnable() {
            @Override
            public void run() {

                while (true) {
                    try {
                        String inputText = BR.readLine();

                        Pattern patternFile = Pattern.compile("^/file\\s{1}(.+)");
                        Matcher matcherFile = patternFile.matcher(inputText);

                        Pattern patternRemote = Pattern.compile("^/remote\\s?(me)?");
                        Matcher matcherRemote = patternRemote.matcher(inputText);

                        if (key.value == null) {
                            key.send(socketChannel, inputText);
                        } else if (matcherFile.matches()) {
                            file.send(matcherFile.group(1), socketChannel);
                        } else if (matcherRemote.matches()) {
                            if (matcherRemote.group(1) != null) {
                                remote.setOutputController();
                            } else {
                                remote.setKeyboardSender(socketChannel);
                            }
                        } else {
                            Message.send(socketChannel, inputText);
                        }
                    } catch (Exception err) {
                        err.printStackTrace();
                        throw new Error("輸入階段發生例外");
                    }
                }
            }
        };
    }

    public int dispatch(SocketChannel socketChannel) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate(2048);
        int curBufferLeng = socketChannel.read(byteBuffer);

        if (curBufferLeng == -1 || curBufferLeng == 0) {
            return curBufferLeng;
        }
        byteBuffer.flip();

        byte prefix = byteBuffer.get();
        for (State c : State.values()) {
            if (c.code == prefix) {
                this.channelStatus = prefix;
            }
        }

        try {
            switch (this.channelStatus) {
                case 0x00:
                    out.println(State.UNDEFINED.desc);
                    break;

                case 0x01:
                    out.println(State.NOTHING.desc);
                    break;

                case 0x0A:
                    out.println(State.KEY.desc);
                    break;

                case 0x0B:
                    Message.handle(byteBuffer);
                    break;

                case 0x0C:
                    curBufferLeng = this.file.handle(byteBuffer, socketChannel);
                    break;

                case 0x0D:
                    this.remote.handle(byteBuffer);
                    break;
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return curBufferLeng;
    }
}