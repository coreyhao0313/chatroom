package packager.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

import static java.lang.System.out;

public class File {
    public static void handle(ByteBuffer byteBuffer, SocketChannel socketChannel, Integer selectionKeyHashCode, Key key)
            throws Exception {
        String keyName = key.getName(selectionKeyHashCode);

        // out.println("[" + clientRemoteAddress + "/" + key + "/傳輸檔案] ");

        int emitCount = 0;
        do {
            emitCount = key.emitOther(keyName, socketChannel, new Key() {
                @Override
                public void emitRun(SocketChannel targetSocketChannel) {
                    try {
                        byteBuffer.position(0);
                        targetSocketChannel.write(byteBuffer);
                    } catch (Exception err) {
                        out.println("[對象發送失敗]");
                    }
                }
            });

            byteBuffer.clear();
        } while (socketChannel.read(byteBuffer) > 0);

        // out.println("[發送對象數] " + emitCount);
    }
}