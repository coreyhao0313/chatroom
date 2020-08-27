package packager.client;

import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;

import static java.lang.System.out;

import packager.State;

public class Message {

    public static void send(SocketChannel socketChannel, String inputText) throws Exception {
        byte[] OPByte = { State.MESSAGE.code };
        byte[] inputTextByte = inputText.getBytes("UTF-8");
        byte[] ctx = new byte[inputTextByte.length + OPByte.length];

        System.arraycopy(OPByte, 0, ctx, 0, OPByte.length);
        System.arraycopy(inputTextByte, 0, ctx, OPByte.length, inputTextByte.length);

        socketChannel.write(ByteBuffer.wrap(ctx));
    }

    public static void handle(ByteBuffer byteBuffer){
        while (byteBuffer.get() != State.NOTHING.code)
            ;
        int breakpointOffset = byteBuffer.position();

        byte[] clientInfoByte = new byte[breakpointOffset - 1];
        byteBuffer.position(1);
        byteBuffer.get(clientInfoByte, 0, breakpointOffset - 1);
        String clientInfo = new String(clientInfoByte);
        out.print("[" + clientInfo + "] ");

        int messageByteLeng = byteBuffer.remaining();
        byte[] clientMessageByte = new byte[messageByteLeng];
        byteBuffer.get(clientMessageByte, 0, messageByteLeng);
        out.print(new String(clientMessageByte));
        out.println();
    }
}