package packager;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
public class Packager {
    private static final int INT_LENG = 4;
    public static final int INFO_LENG = INT_LENG + 1;

    public ByteBuffer ctx;
    public byte[] tBytes;

    public Packager(int capacity) {
        this.tBytes = new byte[1];
        if(capacity < INFO_LENG){
            throw new Error("不可小於辨識用之長度");
        }
        this.ctx = ByteBuffer.allocate(capacity);
    }

    public Packager() {
        this.tBytes = new byte[1];
        this.ctx = ByteBuffer.allocate(4096);
    }

    public void setHead(byte type, int length) {
        this.tBytes[0] = type;
        ByteBuffer lengCtx = ByteBuffer.allocate(INT_LENG).putInt(length);

        this.ctx.put(lengCtx.array());
        this.ctx.put(this.tBytes);
    }

    public void write(byte[] data) throws Exception {
        this.ctx.put(data);
    }

    public void sendTo(SocketChannel socketChannel) throws Exception {
        this.ctx.flip();
        socketChannel.write(this.ctx);
        this.ctx.clear();
    }
}