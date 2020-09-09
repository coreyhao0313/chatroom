package packager.client;

import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;

import static java.lang.System.out;

import base.State;

public class Message {

    public static void send(SocketChannel socketChannel, String inputText) throws Exception {
        byte[] OPBytes = { State.MESSAGE.CODE };
        byte[] inputTextBytes = inputText.getBytes("UTF-8");
        byte[] ctx = new byte[inputTextBytes.length + OPBytes.length];

        System.arraycopy(OPBytes, 0, ctx, 0, OPBytes.length);
        System.arraycopy(inputTextBytes, 0, ctx, OPBytes.length, inputTextBytes.length);

        socketChannel.write(ByteBuffer.wrap(ctx));
    }

    public static void handle(ByteBuffer byteBuffer){
        while (byteBuffer.get() != State.NOTHING.CODE)
            ;
        int breakpointOffset = byteBuffer.position();

        byte[] clientInfoBytes = new byte[breakpointOffset - 1];
        byteBuffer.position(1);
        byteBuffer.get(clientInfoBytes, 0, breakpointOffset - 1);
        String clientInfo = new String(clientInfoBytes);
        out.print("[" + clientInfo + "] ");

        int messageByteLeng = byteBuffer.remaining();
        byte[] clientMessageBytes = new byte[messageByteLeng];
        byteBuffer.get(clientMessageBytes, 0, messageByteLeng);
        out.print(new String(clientMessageBytes));
        out.println();
    }
}