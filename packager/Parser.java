package packager;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import base.packager.Head;

public class Parser {
    public ByteBuffer ctx;
    private ByteBuffer headCtx;
    private boolean bindingSkipped = true;
    private boolean verifySameType = false;
    // todo
    public int breakPointCount = 0;
    public boolean proceeding = false;
    public int nextPosition = 0;
    public boolean isFinish = false;
    // private boolean proceeding = false;
    // private int nextPosition = 0;
    // private boolean isFinish = false;
    public int readableLeng = 0;
    public int dataLimit = 0;
    public int limit = 0;
    public byte type = 0;
    public int leng = 0;
    public Parser evtSelf = null;

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
            this.ctx.clear();
            this.readableLeng = socketChannel.read(this.ctx);
            System.out.println("this.readableLeng >> " + this.readableLeng);
            if (this.readableLeng > 0) {
                this.ctx.flip();
                this.nextPosition = 0;
                return this.getHead(socketChannel);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return false;
    }

    private boolean partOfHeadHelper(SocketChannel socketChannel) {
        while (this.headCtx.hasRemaining() && this.ctx.hasRemaining()) {
            this.headCtx.put(this.ctx.get());
        }
        if (!this.ctx.hasRemaining()) {
            System.out.println("fetchHead");
            return this.fetchHead(socketChannel);
        }
        if (!this.headCtx.hasRemaining()) {
            System.out.println("headCtx");
            this.headCtx.flip();
            boolean state = this.getHead(this.headCtx);
            this.headCtx.clear();
            return state;
        }
        return false;
    }

    private boolean getHead(SocketChannel socketChannel) {
        if (this.ctx.remaining() <= Head.INFO.LENG || this.headCtx.position() != 0) {
            System.out.println("get Head by split way");
            return this.partOfHeadHelper(socketChannel); // 小於可解析之長度
        }
        System.out.println("get Head");
        return this.getHead(this.ctx);
    }

    public boolean getHead(ByteBuffer targetCtx) {
        System.out.println("ps >> " + targetCtx.position());
        if (targetCtx.get() == Head.HEAD_1.CODE && targetCtx.get() == Head.HEAD_2.CODE) {
            byte unknownPrefix = targetCtx.get(); // maybe Head.HEAD_NULL.CODE
            targetCtx.put(targetCtx.position() - 2, (byte) -1);

            byte type = targetCtx.get();

            if (unknownPrefix == Head.HEAD_3.CODE) {
                this.type = type;
                this.breakPointCount = (int) targetCtx.get();
                // this.breakPointCount++; // self count
                this.dataLimit = 0;
                this.limit = this.dataLimit + Head.COUNT_INFO.LENG;
                this.leng = 0;
                this.nextPosition += this.limit;
                this.bindingSkipped = false;
                return true;
            }

            if (this.verifySameType && this.type != 0 && type != this.type) {
                this.type = type;
                return false;
            }
            this.type = type;
            byte[] lengBytes = new byte[Head.SIZE.LENG];
            targetCtx.get(lengBytes, 0, Head.SIZE.LENG);
            this.dataLimit = ByteBuffer.wrap(lengBytes).getInt();

            this.limit = this.dataLimit + Head.INFO.LENG;
            this.leng = 0;
            this.nextPosition += this.limit;
            return true;
        }
        return false;
    }

    public void fetch(SocketChannel socketChannel, Parser evtSelf) {
        this.evtSelf = evtSelf;
        this.fetch(socketChannel);
    }

    public void fetch(SocketChannel socketChannel) {
        try {
            while (this.reachHandler(socketChannel, this.evtSelf == null ? this : this.evtSelf)) {
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

    private boolean reachHandler(SocketChannel socketChannel, Parser evtSelf) {
        if (this.readableLeng == 0) {
            return true;
        }
        int originPosition = this.ctx.position();
        int originLimit = this.ctx.limit();
        this.leng += this.ctx.remaining();

        if (!this.bindingSkipped) {
            this.bindingSkipped = true;
            return !this.tryNext(socketChannel);
        }

        int blockLimit = this.nextPosition > this.readableLeng ? this.readableLeng : this.nextPosition;
        this.ctx.limit(blockLimit);
        //todo
                debug.packager.Parser p = new debug.packager.Parser(this);
                p.log();
                p.log2();
        evtSelf.get(this);

        if (this.isBreakPoint()) {
            this.ctx.position(originPosition);
            evtSelf.breakPoint(this);

            this.ctx.position(originPosition);
            this.ctx.limit(originLimit);
            if (this.tryNext(socketChannel)) {
                return false;
            }
            this.isFinish = true;
            if (this.isFinish()) {
                this.ctx.limit(blockLimit);
                evtSelf.finish(this);
                return false;
            }
        }

        this.ctx.position(originPosition);
        this.ctx.limit(originLimit);
        return true;
    }

    private boolean tryNext(SocketChannel socketChannel) {
        int originPosition = this.ctx.position();
        System.out.println(this.nextPosition > this.readableLeng);
        if (this.nextPosition > this.readableLeng) {
            this.ctx.position(this.ctx.limit());
        }else{
            this.ctx.position(this.nextPosition);
        }
        if (this.getHead(socketChannel)) {
            if (this.proceeding) {
                this.breakPointCount--;
                this.fetch(socketChannel);
            }
            return true;
        }

        this.ctx.position(originPosition);
        return false;
    }

    public void setProceeding(boolean proceeding) {
        this.proceeding = proceeding;
    }

    public void setVerifySameType(boolean verifySameType) {
        this.verifySameType = verifySameType;
    }

    public boolean isBreakPoint() {
        return this.leng >= this.dataLimit;
    }

    public boolean isFinish() {
        return this.isFinish && this.breakPointCount <= 0;
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