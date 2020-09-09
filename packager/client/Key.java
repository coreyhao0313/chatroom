package packager.client;

import java.nio.channels.SocketChannel;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static java.lang.System.out;

import base.State;
import packager.Packager;

public class Key {
    public String value = null;

    public void send(SocketChannel socketChannel, String inputText) throws Exception {
        Pattern pattern = Pattern.compile("\\w{6,12}");
        Matcher matcher = pattern.matcher(inputText);

        if (matcher.matches()) {
            this.value = inputText;
            byte[] inputKeyBytes = inputText.getBytes("UTF-8");

            Packager sPkg = new Packager(128);
            sPkg.setHead(State.KEY);
            sPkg.write(inputKeyBytes);
            sPkg.sendTo(socketChannel);
            return ;
        }
        out.println("[格式錯誤] Key 必須為 6-12 個字元間");
    }
}