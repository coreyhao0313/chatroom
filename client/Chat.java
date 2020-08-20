package client;

import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import static java.lang.System.out;

import server.Control;

public class Chat {
    private SocketChannel socketChannel;
    private Selector selector;
    private byte channelStatus = Control.UNDEFINED.code;
    private String channelKey;
    private InputStreamReader ISR = new InputStreamReader(System.in);
    private BufferedReader BR = new BufferedReader(ISR);
    private FileOutputStream FOS;
    private BufferedOutputStream BOS;

    private long uploadByteFullSize = 0;
    private long uploadByteSize = 0;

    private long downloadByteFullSize = 0;
    private long downloadByteSize = 0;

    public Chat(String address, int port) {
        this.createConnection(address, port);
        this.setHandler();
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

    public void setHandler() {
        while (true) {
            try {
                this.selector.select();

                Set<SelectionKey> selectionKeys = this.selector.selectedKeys();
                Iterator<SelectionKey> selectionKeysIterator = selectionKeys.iterator();

                while (selectionKeysIterator.hasNext()) {
                    SelectionKey selectionKey = (SelectionKey) selectionKeysIterator.next();
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    selectionKeysIterator.remove();

                    if (selectionKey.isConnectable()) {
                        this.setConnectHandler(socketChannel);
                        new Thread(this.setInputHandler(socketChannel)).start();
                    } else if (selectionKey.isReadable()) {
                        this.setChannelHandler(socketChannel);
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

    public void setChannelHandler(SocketChannel socketChannel) {
        try {
            switch (this.dispatch(socketChannel)) {
                case -1:
                    out.println("[連線中斷] " + socketChannel.getRemoteAddress());
                    socketChannel.close();
                    this.channelStatus = Control.UNDEFINED.code;
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
        return new Runnable() {
            @Override
            public void run() {

                while (true) {
                    try {
                        String inputText = BR.readLine();
                        Pattern patternFile = Pattern.compile("^/file\\s{1}(.+)");
                        Matcher matcherFile = patternFile.matcher(inputText);
                        Pattern patternOk = Pattern.compile("^/ok");
                        Matcher matcherOk = patternOk.matcher(inputText);

                        if (channelKey == null) {
                            sendKey(inputText, socketChannel);
                        } else if (matcherFile.matches()) {
                            sendPartOfFile(matcherFile.group(1), socketChannel);
                        } else if (matcherOk.matches()) {
                            BOS.close();
                        } else {
                            sendText(inputText, socketChannel);
                        }
                    } catch (Exception err) {
                        err.printStackTrace();
                        throw new Error("輸入階段發生例外");
                    }
                }
            }
        };
    }

    public void sendKey(String inputText, SocketChannel socketChannel) throws Exception {
        Pattern pattern = Pattern.compile("\\w{6,12}");
        Matcher matcher = pattern.matcher(inputText);

        if (matcher.matches()) {
            this.channelKey = inputText;

            byte[] OPByte = { Control.KEY.code };
            byte[] inputKeyByte = inputText.getBytes("UTF-8");
            byte[] ctx = new byte[inputKeyByte.length + OPByte.length];

            System.arraycopy(OPByte, 0, ctx, 0, OPByte.length);
            System.arraycopy(inputKeyByte, 0, ctx, 1, inputKeyByte.length);

            socketChannel.write(ByteBuffer.wrap(ctx));
        } else {
            out.println("[格式錯誤] Key 必須為 6-12 個字元間");
        }

    }

    public void sendText(String inputText, SocketChannel socketChannel) throws Exception {
        byte[] OPByte = { Control.MESSAGE.code };
        byte[] inputTextByte = inputText.getBytes("UTF-8");
        byte[] ctx = new byte[inputTextByte.length + OPByte.length];

        System.arraycopy(OPByte, 0, ctx, 0, OPByte.length);
        System.arraycopy(inputTextByte, 0, ctx, OPByte.length, inputTextByte.length);

        socketChannel.write(ByteBuffer.wrap(ctx));
    }

    public void sendPartOfFile(String path, SocketChannel socketChannel) {
        try {
            Path filePath = Paths.get(path).normalize();
            String fileName = filePath.getFileName().toString();
            long fileSize = Files.size(filePath);
            this.uploadByteFullSize = fileSize;
            String fileSizeString = String.valueOf(fileSize);
            out.println("[傳輸檔案] " + fileName);

            FileInputStream FIS = new FileInputStream(path);
            BufferedInputStream BIS = new BufferedInputStream(FIS, 65536);

            byte[] OPByte = { Control.FILE.code, 1 };
            byte[] fileNameByte = fileName.getBytes("UTF-8");
            byte[] OPByte_SPLIT = { Control.NOTHING.code };
            byte[] fileByte = new byte[2046];
            ByteBuffer ctx = ByteBuffer.allocate(2048);

            ctx.put(OPByte);
            ctx.put(fileNameByte);
            ctx.put(OPByte_SPLIT);
            ctx.put(fileSizeString.getBytes());
            ctx.put(OPByte_SPLIT);

            int fileByteRemaining = ctx.remaining();
            int fileByteLeng = BIS.read(fileByte, 0, fileByteRemaining);

            ctx.put(fileByte, 0, fileByteLeng);
            this.uploadByteSize = fileByteLeng;

            BIS.mark(0);
            BIS.reset();

            ctx.flip();
            socketChannel.write(ctx);
            out.println(this.uploadByteSize + " / " + this.uploadByteFullSize);

            OPByte[1] = 0;
            ctx.clear();
            while ((fileByteLeng = BIS.read(fileByte)) != -1) {
                ctx.put(OPByte);
                ctx.put(fileByte);
                ctx.flip();

                socketChannel.write(ctx);
                this.uploadByteSize += fileByteLeng;
                out.println(this.uploadByteSize + " / " + this.uploadByteFullSize);

                ctx.clear();
            }

            BIS.close();
            this.uploadByteSize = 0;
            this.uploadByteFullSize = 0;

            out.println("[傳輸完成]");
        } catch (Exception err) {
            err.printStackTrace();
            out.println("讀檔或傳輸階段失敗");
        }
    }

    public int dispatch(SocketChannel socketChannel) throws Exception {
        ByteBuffer bufferData = ByteBuffer.allocate(2048);
        int curBufferLeng = socketChannel.read(bufferData);

        if (curBufferLeng == -1 || curBufferLeng == 0) {
            return curBufferLeng;
        }
        bufferData.flip();

        byte prefix = bufferData.get();
        for (Control c : Control.values()) {
            if (c.code == prefix) {
                this.channelStatus = prefix;
            }
        }

        try {
            switch (this.channelStatus) {
                case 0x00:
                    out.println(Control.UNDEFINED.desc);
                    break;

                case 0x01:
                    out.println(Control.NOTHING.desc);
                    break;

                case 0x0A:
                    out.println(Control.KEY.desc);
                    break;

                case 0x0B:
                    while (bufferData.get() != Control.NOTHING.code)
                        ;
                    int breakpointOffset = bufferData.position();

                    byte[] clientInfoByte = new byte[breakpointOffset - 1];
                    bufferData.position(1);
                    bufferData.get(clientInfoByte, 0, breakpointOffset - 1);
                    String clientInfo = new String(clientInfoByte);
                    out.print("[" + clientInfo + "] ");

                    int messageByteLeng = bufferData.remaining();
                    byte[] clientMessageByte = new byte[messageByteLeng];
                    bufferData.get(clientMessageByte, 0, messageByteLeng);
                    out.print(new String(clientMessageByte));
                    out.println();
                    break;

                case 0x0C:
                    if (bufferData.get() == 1) {
                        while (bufferData.get() != Control.NOTHING.code)
                            ;
                        int fileNameByteLimit = bufferData.position();
                        while (bufferData.get() != Control.NOTHING.code)
                            ;
                        int fileSizeByteLimit = bufferData.position();

                        int START_POS = 2;
                        bufferData.position(START_POS);

                        int fileNameByteLeng = fileNameByteLimit - 1 - START_POS;
                        byte[] fileNameByte = new byte[fileNameByteLeng];
                        bufferData.get(fileNameByte, 0, fileNameByteLeng);
                        String fileName = new String(fileNameByte);

                        bufferData.position(fileNameByteLimit);

                        int fileSizeByteLeng = fileSizeByteLimit - 1 - fileNameByteLimit;
                        byte[] fileSizeByte = new byte[fileSizeByteLeng];
                        bufferData.get(fileSizeByte, 0, fileSizeByteLeng);
                        String fileSize = new String(fileSizeByte);
                        this.downloadByteFullSize = Long.parseLong(fileSize);
                        out.println("[接收檔案] " + fileName);

                        this.FOS = new FileOutputStream("./files/" + fileName);
                        this.BOS = new BufferedOutputStream(FOS, 65536);

                        bufferData.position(fileSizeByteLimit);
                    }
                    byte[] fileByte = new byte[2046];
                    int fileByteBufferRemaining = bufferData.remaining();
                    int fileSizeRemaining = (int) this.downloadByteFullSize - (int) this.downloadByteSize;
                    if (fileSizeRemaining < fileByteBufferRemaining) {
                        fileByteBufferRemaining = fileSizeRemaining;
                    }
                    bufferData.get(fileByte, 0, fileByteBufferRemaining);
                    this.downloadByteSize += fileByteBufferRemaining;

                    this.BOS.write(fileByte, 0, fileByteBufferRemaining);
                    out.println(this.downloadByteSize + " / " + this.downloadByteFullSize);

                    bufferData.clear();

                    while ((curBufferLeng = socketChannel.read(bufferData)) > 0) {
                        bufferData.position(2);

                        fileByteBufferRemaining = bufferData.remaining();
                        fileSizeRemaining = (int) this.downloadByteFullSize - (int) this.downloadByteSize;

                        if (fileSizeRemaining < fileByteBufferRemaining) {
                            fileByteBufferRemaining = fileSizeRemaining;
                        }
                        bufferData.get(fileByte, 0, fileByteBufferRemaining);
                        this.downloadByteSize += fileByteBufferRemaining;

                        this.BOS.write(fileByte, 0, fileByteBufferRemaining);
                        out.println(this.downloadByteSize + " / " + this.downloadByteFullSize);

                        bufferData.clear();
                    }

                    if (this.downloadByteSize >= this.downloadByteFullSize) {
                        this.BOS.close();
                        this.downloadByteSize = 0;
                        this.downloadByteFullSize = 0;

                        out.println("[接收完成]");
                    }
                    break;
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return curBufferLeng;
    }
}