package packager.client;

import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static java.lang.System.out;

import base.State;

public class Key {
    public String value = null;

    public void send(SocketChannel socketChannel, String inputText) throws Exception {
        Pattern pattern = Pattern.compile("\\w{6,12}");
        Matcher matcher = pattern.matcher(inputText);

        if (matcher.matches()) {
            this.value = inputText;

            byte[] OPBytes = { State.KEY.code };
            byte[] inputKeyBytes = inputText.getBytes("UTF-8");
            ByteBuffer ctx = ByteBuffer.allocate(inputKeyBytes.length + OPBytes.length);

            ctx.put(OPBytes);
            ctx.put(inputKeyBytes);
            ctx.flip();

            socketChannel.write(ctx);
            return ;
        }
        out.println("[格式錯誤] Key 必須為 6-12 個字元間");
    }
}