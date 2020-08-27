package packager.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.lang.System.out;

import packager.State;

public class Message {
    public static void handle(ByteBuffer byteBuffer, SocketChannel socketChannel, Integer targetKey, Key key,
            String clientRemoteAddress) throws Exception {
        byte[] ctxByte = new byte[2048];
        byte[] OP_MESSAGE = { State.MESSAGE.code };

        // head
        System.arraycopy(OP_MESSAGE, 0, ctxByte, 0, OP_MESSAGE.length);

        // info
        byte[] clientInfoByte = clientRemoteAddress.getBytes("UTF-8");
        System.arraycopy(clientInfoByte, 0, ctxByte, OP_MESSAGE.length, clientInfoByte.length);

        byte[] OPByte_SPLIT = { State.NOTHING.code };

        // split
        System.arraycopy(OPByte_SPLIT, 0, ctxByte, OP_MESSAGE.length + clientInfoByte.length, OPByte_SPLIT.length);

        // message
        int msgByteLeng = byteBuffer.remaining();
        byte[] msgByte = new byte[msgByteLeng];
        byteBuffer.get(msgByte, 0, msgByteLeng);

        System.arraycopy(msgByte, 0, ctxByte, OP_MESSAGE.length + clientInfoByte.length + OPByte_SPLIT.length,
                msgByte.length);

        String keyName = key.getName(targetKey);

        out.println("[" + new String(clientInfoByte) + "/" + keyName + "/傳輸訊息] ");
        out.println(new String(msgByte));

        int emitCount = key.emitOther(keyName, socketChannel, new Key() {
            @Override
            public void emitRun(SocketChannel targetSocketChannel) {
                try {
                    targetSocketChannel.write(ByteBuffer.wrap(ctxByte));
                } catch (Exception err) {
                    out.println("[對象發送失敗]");
                }
            }
        });

        out.println("[發送對象數] " + emitCount);
    }
}