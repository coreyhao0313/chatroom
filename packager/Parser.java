package packager;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import base.packager.Head;

public class Parser {
    public ByteBuffer ctx;
    private ByteBuffer headCtx;
    //todo
    public boolean proceeding = false;
    public int nextPosition = 0;
    // private boolean proceeding = false;
    // private int nextPosition = 0;
    private boolean isFinish = false;
    public int readableLeng = 0;
    public int dataLimit = 0;
    public int limit = 0;
    public byte type = 0;
    public int leng = 0;

    public Parser(int capacity) {
        if (capacity < Head.INFO.LENG) {
            throw new Error("不可小於辨識用之長度");
        }
        this.ctx = ByteBuffer.allocate(capacity);
        this.headCtx = ByteBuffer.allocate(Head.INFO.LENG);
    }

    public Parser() {
        this.ctx = ByteBuffer.allocate(4096);
        this.headCtx = ByteBuffer.allocate(Head.INFO.LENG);
    }

    public void get(Parser self) {
        ;
    }

    public void finish(Parser self) {
        ;
    }

    public void breakPoint(Parser self) {
        ;
    }

    public boolean fetchHead(SocketChannel socketChannel) {
        try {
            this.readableLeng = socketChannel.read(this.ctx);
            if (this.readableLeng > 0) {
                this.ctx.flip();
                return this.getHead(socketChannel);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return false;
    }

    private boolean getHead(SocketChannel socketChannel) {
        if (this.ctx.remaining() <= Head.INFO.LENG || this.headCtx.position() != 0) {
            return this.partOfHeadHelper(socketChannel); // 小於可解析之長度
        }
        return this.getHead(this.ctx);
    }

    // private boolean getHead(SocketChannel socketChannel) {
    //     if (this.ctx.remaining() <= Head.INFO.LENG) {
    //         return false; // 小於可解析之長度
    //     }
    //     return this.getHead(this.ctx);
    // }


    public boolean getHead(ByteBuffer targetCtx){
        if (targetCtx.get() == Head.HEAD_1.CODE && targetCtx.get() == Head.HEAD_2.CODE) {
            targetCtx.put(targetCtx.position() - 1, (byte) 0);

            this.type = targetCtx.get();

            byte[] lengBytes = new byte[Head.SIZE.LENG];
            targetCtx.get(lengBytes, 0, Head.SIZE.LENG);
            this.dataLimit = ByteBuffer.wrap(lengBytes).getInt();

            this.limit = this.dataLimit + Head.INFO.LENG;
            this.nextPosition += this.limit;
            this.leng = 0;
            return true;
        }
        return false;
    }

    private boolean partOfHeadHelper(SocketChannel socketChannel) {
        while(this.headCtx.hasRemaining() && this.ctx.hasRemaining()){
            this.headCtx.put(this.ctx.get());
        }
        if (!this.ctx.hasRemaining()) {
           return this.fetchHead(socketChannel);
        }
        if(!this.headCtx.hasRemaining()){
            this.headCtx.flip();
            boolean state = this.getHead(this.headCtx);
            this.headCtx.clear();
            return state;
        }
        return false;
    }

    public void fetch(SocketChannel socketChannel){
        this.fetch(socketChannel, this);
    }

    public void fetch(SocketChannel socketChannel, Parser actParser) {
        try {
            while (this.reachHandler(socketChannel, actParser)) {
                this.ctx.clear();
                this.readableLeng = socketChannel.read(this.ctx);
                if (this.readableLeng == -1) {
                    break;
                }
                this.ctx.flip();
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private boolean reachHandler(SocketChannel socketChannel, Parser actParser) {
        if (this.readableLeng == 0) {
            return true;
        }
        int originPosition = this.ctx.position();
        int originLimit = this.ctx.limit();
        this.leng += this.ctx.remaining();

        int blockLimit = this.nextPosition > this.readableLeng ? this.readableLeng : this.nextPosition;
        this.ctx.limit(blockLimit);
        // debug.packager.Parser pdebug = new debug.packager.Parser(this);
        // pdebug.log();
        actParser.get(this);

        if (this.isBreakPoint()) {
            this.ctx.position(originPosition);
            actParser.breakPoint(this);

            this.ctx.position(originPosition);
            this.ctx.limit(originLimit);
            if (this.tryNext(socketChannel, actParser)) {
                return false;
            }
            this.isFinish = true;
            this.ctx.limit(blockLimit);
            actParser.finish(this);
            return false;
        }

        this.ctx.position(originPosition);
        this.ctx.limit(originLimit);
        return true;
    }

    private boolean tryNext(SocketChannel socketChannel, Parser actParser) {
        int originPosition = this.ctx.position();
        if (this.nextPosition > this.readableLeng) {
            return false;
        }
        this.ctx.position(this.nextPosition);
        if (this.getHead(socketChannel)) {
            if (this.proceeding) {
                this.fetch(socketChannel, actParser);
            }
            return true;
        }

        this.ctx.position(originPosition);
        return false;
    }

    public void setProceeding(boolean proceeding) {
        this.proceeding = proceeding;
    }

    public boolean isBreakPoint() {
        return this.leng >= this.dataLimit;
    }

    public boolean isFinish() {
        return this.isFinish;
    }

    public byte[] getBytes() {
        int originPosition = this.ctx.position();
        int remaining = this.ctx.remaining();
        byte[] ctxBytes = new byte[remaining];
        this.ctx.get(ctxBytes);
        this.ctx.position(originPosition);
        return ctxBytes;
    }
}