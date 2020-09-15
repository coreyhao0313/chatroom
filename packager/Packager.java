package packager;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import base.State;
import base.packager.Head;

public class Packager {
    public ByteBuffer ctx;
    public byte[] prefixBytes = new byte[Head.PREFIX.LENG];
    public byte[] typeBytes = new byte[Head.TYPE.LENG];
    public boolean wroteHead = false;

    public Packager(int capacity) {
        if (capacity < Head.INFO.LENG) {
            throw new Error("不可小於解析用之長度");
        }
        this.ctx = ByteBuffer.allocate(capacity);
    }

    public Packager() {
        this.ctx = ByteBuffer.allocate(4096);
    }

    public void setHead(Head head, State state) {
        this.prefixBytes[0] = Head.HEAD_1.CODE;
        this.prefixBytes[1] = Head.HEAD_2.CODE;
        this.prefixBytes[2] = head.CODE;

        this.typeBytes[0] = state.CODE;

        this.ctx.put(this.prefixBytes);
        this.ctx.put(this.typeBytes);
    }

    public void setHead(State state) {
        this.setHead(Head.HEAD_NULL, state);
    }

    public void setHead(int length) {
        ByteBuffer lengCtx = ByteBuffer.allocate(Head.SIZE.LENG).putInt(length);

        this.ctx.put(lengCtx.array());
    }

    public void setHead(State state, int length) {
        this.setHead(state);
        this.setHead(length);
        this.wroteHead = true;
    }

    private void putDefaultHead (){
        this.prefixBytes[2] = Head.HEAD_NULL.CODE;
        this.ctx.put(this.prefixBytes);
        this.ctx.put(this.typeBytes);
    }

    public void write(byte[] data) throws Exception {
        if (!this.wroteHead) {
            if (this.ctx.position() < Head.PREFIX.LENG + Head.TYPE.LENG) {
                this.putDefaultHead();
            }
            this.setHead(data.length);
            this.wroteHead = true;
        }
        this.wroteHead = true;
        this.ctx.put(data);
    }

    public void sendTo(SocketChannel socketChannel) throws Exception {
        this.ctx.flip();
        socketChannel.write(this.ctx);
        // debug.packager.Packager pdebug = new debug.packager.Packager(this);
        // pdebug.log();
        this.ctx.clear();
    }

    public void sendTo2(SocketChannel socketChannel) throws Exception {
        this.ctx.flip();
        // socketChannel.write(this.ctx);
        debug.packager.Packager pdebug = new debug.packager.Packager(this);
        pdebug.log();
        this.ctx.clear();
    }

    public void proceed(){
        this.wroteHead = false;
    }
    
    public void breakPoint(){
        this.putDefaultHead();
        this.proceed();
    }

    public void bind(State state, int count){
        this.setHead(Head.HEAD_3, state);

        byte[] bindingInfoBytes = new byte[Head.BINDING.LENG];
        bindingInfoBytes[0] = (byte)count;
        this.ctx.put(bindingInfoBytes);

        this.setHead(state);
    }

    // public static void sendEnd(SocketChannel socketChannel) throws Exception {
    // byte[] endBytes = new byte[1];
    // endBytes[0] = (byte)0xFF;

    // socketChannel.write(ByteBuffer.wrap(endBytes));
    // }
}