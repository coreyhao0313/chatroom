package packager;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import base.State;
import base.packager.Head;

public class Packager {
    public ByteBuffer ctx;
    public byte[] prefixBytes = new byte[Head.PREFIX.LENG];
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

    public void setHead(State state) {
        this.prefixBytes[0] = Head.HEAD_1.CODE;
        this.prefixBytes[1] = Head.HEAD_2.CODE;
        this.prefixBytes[2] = state.code;

        this.ctx.put(this.prefixBytes);
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

    public void write(byte[] data) throws Exception {
        if (!this.wroteHead) {
            if (this.ctx.position() < Head.PREFIX.LENG) {
                throw new Error("必須先定義前綴");
            }
            this.setHead(data.length);
            this.wroteHead = true;
        }
        this.wroteHead = true;
        this.ctx.put(data);
    }

    public void sendTo(SocketChannel socketChannel) throws Exception {
        this.ctx.flip();
        // socketChannel.write(this.ctx);

        debug.packager.Packager pdebug = new debug.packager.Packager(this);
        pdebug.log();

        this.ctx.clear();
    }

    // public static void sendEnd(SocketChannel socketChannel) throws Exception {
    // byte[] endBytes = new byte[1];
    // endBytes[0] = (byte)0xFF;

    // socketChannel.write(ByteBuffer.wrap(endBytes));
    // }
}