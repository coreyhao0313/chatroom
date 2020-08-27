package packager.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.lang.System.out;

public class Remote {
    public final static short REMOTE_LENG = 8;
    public final static short OFFSET_LENG = 3;
    public final static short OFFSET_INDEX = 2;
    public static void handle(ByteBuffer byteBuffer, SocketChannel socketChannel, Integer targetKey, Key key)
            throws Exception {
        short keycodeLeng = byteBuffer.get(OFFSET_INDEX);
        int remoteRemaining = byteBuffer.remaining();
        byteBuffer.position(0);

        while ((remoteRemaining = byteBuffer.remaining()) >= keycodeLeng + OFFSET_LENG) {
            byte[] remoteByte = new byte[REMOTE_LENG];
            byteBuffer.get(remoteByte, 0, OFFSET_LENG + keycodeLeng);
            ByteBuffer ctx = ByteBuffer.wrap(remoteByte);

            String keyName = key.getName(targetKey);

            // out.println("[" + clientRemoteAddress + "/" + key + "/傳輸控制訊息] ");

            key.emitOther(keyName, socketChannel, new Key() {
                @Override
                public void emitRun(SocketChannel targetSocketChannel) {
                    try {
                        targetSocketChannel.write(ctx);
                    } catch (Exception err) {
                        out.println("[對象發送失敗]");
                    }
                }
            });
            // out.println("[發送對象數] " + (keySocketChannels.size() - 1));

            byteBuffer.compact();
            byteBuffer.flip();
            if (byteBuffer.remaining() > OFFSET_LENG) {
                keycodeLeng = byteBuffer.get(OFFSET_INDEX);
            }
        }
    }
}