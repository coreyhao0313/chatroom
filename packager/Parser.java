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
    // private boolean proceeding = false;
    // private int nextPosition = 0;
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
            if (this.readableLeng > 0) {
                this.ctx.flip();
                return this.getHead();
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return false;
    }

    private boolean getHead() {
        return this.getHead(this.ctx);
    }

    public boolean getHead(ByteBuffer targetCtx) {
        if (targetCtx.get() == Head.HEAD_1.CODE && targetCtx.get() == Head.HEAD_2.CODE) {
            byte unknownPrefix = targetCtx.get(); // maybe Head.HEAD_NULL.CODE
            targetCtx.put(targetCtx.position() - 2, (byte) -1);

            byte type = targetCtx.get();

            if (unknownPrefix == Head.HEAD_3.CODE) {
                this.type = type;
                this.breakPointCount = (int) targetCtx.get();
                // this.breakPointCount++; // self count
                this.dataLimit = 0;
                this.limit = this.dataLimit + Head.BINDING_INFO.LENG;
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
        Parser evtSelf = this.evtSelf == null ? this : this.evtSelf;
        try {
            while (true) {
                if (this.readableLeng != 0) {
                    if (this.reachHandler(evtSelf)) {
                        if (this.proceeding && this.tryNext(socketChannel)) {
                            this.breakPointCount--;
                            continue;
                        }
    
                        if (this.isFinish()) {
                            evtSelf.finish(this);
                            break;
                        }
                    }
                }

                this.ctx.clear();
                this.readableLeng = socketChannel.read(this.ctx);
                if (this.readableLeng == -1 || this.readableLeng == 0) {
                    break;
                }
                this.ctx.flip();
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private boolean reachHandler(Parser evtSelf) {
        int originPosition = this.ctx.position();
        int originLimit = this.ctx.limit();

        this.leng += this.ctx.remaining();

        boolean isBreakPoint = this.isBreakPoint();

        if (!this.bindingSkipped) {
            this.bindingSkipped = true;
            return isBreakPoint;
        }

        int blockLimit = this.nextPosition > this.readableLeng ? this.readableLeng : this.nextPosition;
        this.ctx.limit(blockLimit);
        evtSelf.get(this);

        if (isBreakPoint) {
            this.ctx.position(originPosition);
            this.ctx.limit(blockLimit);
            evtSelf.breakPoint(this);
        }

        this.ctx.position(originPosition);
        this.ctx.limit(originLimit);
        return isBreakPoint;
    }

    private boolean tryNext(SocketChannel socketChannel) {
        if (this.nextPosition > this.readableLeng) {
            this.nextPosition = this.ctx.limit();
        }
        this.ctx.position(this.nextPosition);
        if (this.ctx.remaining() <= Head.INFO.LENG) { // 小於可解析之長度
            this.nextPosition = 0;
            return this.fetchHeadBySplitWay(socketChannel);
        }

        if (this.getHead()) {
            return true;
        }
        return false;
    }

    private boolean fetchHeadBySplitWay(SocketChannel socketChannel) {
        while (this.headCtx.hasRemaining() && this.ctx.hasRemaining()) {
            this.headCtx.put(this.ctx.get());
        }

        if (!this.ctx.hasRemaining()) {
            try {
                this.ctx.clear();
                this.readableLeng = socketChannel.read(this.ctx);
                if (this.readableLeng > 0) {
                    this.ctx.flip();
                    if (this.headCtx.remaining() != 0 && this.readableLeng != -1) {
                        return this.fetchHeadBySplitWay(socketChannel);
                    }
                } else {
                    return false;
                }
            } catch (Exception err) {
                err.printStackTrace();
                return false;
            }
        }
        if (!this.headCtx.hasRemaining()) {
            this.headCtx.flip();
            boolean state = this.getHead(this.headCtx);
            this.headCtx.clear();
            return state;
        }
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
        return this.isBreakPoint() && this.breakPointCount <= 0;
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