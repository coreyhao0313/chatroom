package packager;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import base.packager.Head;

public class Parser {
    public ByteBuffer ctx;
    public int readableLeng = 0;
    public int dataLimit = 0;
    public int limit = 0;
    public int collLeng = 0;
    public int nextPosition = 0;
    public byte type = 0;
    public int leng = 0;
    private boolean keep = false;

    public Parser(int capacity) {
        if (capacity < Head.INFO.LENG) {
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

    public void next(Parser self) {
        ;
    }

    public void fetchHead(SocketChannel socketChannel) throws Exception {
        this.readableLeng = socketChannel.read(this.ctx);
        if (this.readableLeng > 0) {
            this.ctx.flip();
            this.getHead();
        }
    }

    public boolean getHead() {
        int remaining = this.ctx.remaining();

        if (remaining <= Head.INFO.LENG) {
            return false; // 小於可解析之長度
        }

        if (this.ctx.get() == Head.HEAD_1.CODE && this.ctx.get() == Head.HEAD_2.CODE) {
            this.ctx.put(this.ctx.position() - 1, (byte) 0);

            this.type = this.ctx.get();

            byte[] lengBytes = new byte[Head.SIZE.LENG];
            this.ctx.get(lengBytes, 0, Head.SIZE.LENG);
            this.dataLimit = ByteBuffer.wrap(lengBytes).getInt();

            this.limit = this.dataLimit + Head.INFO.LENG;
            this.nextPosition += this.limit;
            this.leng = 0;
            return true;
        }
        return false;
    }

    public void fetch(SocketChannel socketChannel, Parser actParser) {
        try {
            while (this.reachHandler(socketChannel, actParser)) {
                this.ctx.clear();
                this.readableLeng = socketChannel.read(this.ctx);
                this.ctx.flip();
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private boolean reachHandler(SocketChannel socketChannel, Parser actParser) {
        int originPosition = this.ctx.position();
        int originLimit = this.ctx.limit();
        // if(originLimit == 0){
        //     return false;
        // }

        this.collLeng += this.readableLeng;
        this.leng += this.ctx.remaining();

        int blockLimit = this.nextPosition > this.readableLeng ? this.readableLeng : this.nextPosition;
        // System.out.println("nextPosition >> " + this.nextPosition);
        // System.out.println("readableLeng >> " + readableLeng);
        // System.out.println("blockLimit >> " + blockLimit);
        this.ctx.limit(blockLimit);
        actParser.get(this);

        debug.packager.Parser pdebug = new debug.packager.Parser(this);
        pdebug.log();

        if (this.isDone()) {
            actParser.next(this);
            this.ctx.position(originPosition);

            this.ctx.limit(originLimit);
            if (this.tryNext(socketChannel, actParser)) {
                return false;
            }
            this.ctx.limit(blockLimit);
            actParser.finish(this);
            return false;
        }
        this.ctx.limit(originLimit);
        return true;
    }

    public boolean tryNext(SocketChannel socketChannel, Parser actParser) {
        int originPosition = this.ctx.position();
        if (this.nextPosition > this.readableLeng) {
            return false;
        }
        this.ctx.position(this.nextPosition);
        if (this.getHead()) {
            if (this.keep) {
                this.fetch(socketChannel, actParser);
            }
            return true;
        }

        this.ctx.position(originPosition);
        return false;
    }

    public void setKeep(boolean keep) {
        this.keep = keep;
    }

    public boolean isDone() {
        return this.leng >= this.dataLimit;
    }
}