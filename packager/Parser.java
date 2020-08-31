package packager;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Parser {
    private static final int INT_LENG = 4;
    public static final int INFO_LENG = INT_LENG + 1;

    public ByteBuffer ctx;
    public int collLeng = 0;
    public int dataLeng = 0;
    public int maxLeng = 0;
    public byte type = 0;
    public int readLeng = 0;
    public boolean hasHead = false;
    private boolean keep = false;
    public int collPOS = 0;

    public Parser(int capacity) {
        if (capacity < INFO_LENG) {
            throw new Error("不可小於辨識用之長度");
        }
        this.ctx = ByteBuffer.allocate(capacity);
    }

    public Parser() {
        this.ctx = ByteBuffer.allocate(4096);
    }

    public void get(Parser self) {
        ;
    }

    public void finish(Parser self) {
        ;
    }

    public void over(Parser self) {
        ;
    }

    public void fetchToSetHead(SocketChannel socketChannel) throws Exception {
        if (this.hasHead) {
            return;
        }
        socketChannel.read(this.ctx);
        this.ctx.flip();
        setHead();
    }

    public boolean setHead() {
        this.readLeng = this.ctx.remaining();
        if (this.readLeng <= INFO_LENG) {
            return false; // 小於可解析之長度
        }
        byte[] lengBytes = new byte[INT_LENG];
        this.ctx.get(lengBytes, 0, INT_LENG);

        this.dataLeng = ByteBuffer.wrap(lengBytes).getInt();
        this.type = this.ctx.get();
        this.maxLeng = this.dataLeng + INFO_LENG;

        this.collLeng = this.readLeng;

        return (this.hasHead = true);
    }

    public void fetch(SocketChannel socketChannel, Parser actParser) {
        if (this.ctx.remaining() != 0) {
            actParser.get(this);
        }
        if (this.hasFetchDone(socketChannel, actParser)) {
            return;
        }

        try {
            this.ctx.clear();

            while (socketChannel.read(this.ctx) > 0) {
                this.ctx.flip();
                this.collLeng += this.ctx.remaining();
                actParser.get(this);
                this.hasFetchDone(socketChannel, actParser);
                this.ctx.clear();
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private boolean hasFetchDone(SocketChannel socketChannel, Parser actParser) {
        if (this.hasDone()) {
            actParser.finish(this);

            if (this.hasOver()) {
                if (this.keep) {
                    this.setNext();
                    this.setHead();
                    this.fetch(socketChannel, actParser);
                }
                actParser.over(this);
            }
            this.hasHead = false; // 重置 head 狀態
            return true;
        }
        return false;
    }

    public void setNext() {
        this.collPOS += this.maxLeng;
        this.ctx.position(this.getOverPOS());
    }

    public int getDataRemaining() {
        int ctxCapacity = this.ctx.capacity();
        return this.dataLeng > ctxCapacity ? ctxCapacity : this.dataLeng;
    }

    public void setKeep(boolean keep) {
        this.keep = keep;
    }

    public boolean hasDone() {
        return this.collLeng >= this.maxLeng;
    }

    public int getOverLeng() {
        return this.collLeng - this.maxLeng;
    }

    public int getOverPOS() {
        int ctxCapacity = this.ctx.capacity();
        return this.collPOS > ctxCapacity ? 0 : this.collPOS;
    }

    public boolean hasOver() {
        return this.collLeng > this.maxLeng;
    }
}