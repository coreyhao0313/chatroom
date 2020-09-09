package packager.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.lang.System.out;

import base.State;

public class Message {
    public static void handle(ByteBuffer byteBuffer, SocketChannel socketChannel, Integer targetKey, Key key,
            String clientRemoteAddress) throws Exception {
        byte[] ctxBytes = new byte[2048];
        byte[] OP_MESSAGE = { State.MESSAGE.CODE };

        // head
        System.arraycopy(OP_MESSAGE, 0, ctxBytes, 0, OP_MESSAGE.length);

        // info
        byte[] clientInfoBytes = clientRemoteAddress.getBytes("UTF-8");
        System.arraycopy(clientInfoBytes, 0, ctxBytes, OP_MESSAGE.length, clientInfoBytes.length);

        byte[] OPByte_SPLIT = { State.NOTHING.CODE };

        // split
        System.arraycopy(OPByte_SPLIT, 0, ctxBytes, OP_MESSAGE.length + clientInfoBytes.length, OPByte_SPLIT.length);

        // message
        int msgByteLeng = byteBuffer.remaining();
        byte[] msgBytes = new byte[msgByteLeng];
        byteBuffer.get(msgBytes, 0, msgByteLeng);

        System.arraycopy(msgBytes, 0, ctxBytes, OP_MESSAGE.length + clientInfoBytes.length + OPByte_SPLIT.length,
                msgBytes.length);

        String keyName = key.getName(targetKey);

        out.println("[" + new String(clientInfoBytes) + "/" + keyName + "/傳輸訊息] ");
        out.println(new String(msgBytes));

        int emitCount = key.emitOther(keyName, socketChannel, new Key() {
            @Override
            public void emitRun(SocketChannel targetSocketChannel) {
                try {
                    targetSocketChannel.write(ByteBuffer.wrap(ctxBytes));
                } catch (Exception err) {
                    out.println("[對象發送失敗]");
                }
            }
        });

        out.println("[發送對象數] " + emitCount);
    }
}