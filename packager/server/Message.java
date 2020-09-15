package packager.server;

import java.nio.channels.SocketChannel;

import static java.lang.System.out;

import base.State;
import packager.Parser;
import packager.Packager;

public class Message {
    public static void handle(Parser pkg, SocketChannel socketChannel, Integer targetKey, Key key) throws Exception {
        String keyName = key.getName(targetKey);

        pkg.fetch(socketChannel);
        String userFromInfo = socketChannel.getRemoteAddress().toString();
        byte[] messageBytes = pkg.getBytes();

        out.println("[" + userFromInfo + "/" + keyName + "/傳輸訊息] ");
        out.println(new String(messageBytes));

        Packager cPkg = new Packager(2048);
        cPkg.bind(State.MESSAGE, 2);
        cPkg.write(userFromInfo.getBytes());
        cPkg.breakPoint();
        cPkg.write(messageBytes);

        if (messageBytes.length == 0) {
            return;
        }
        cPkg.ctx.flip();
        int originPosition = cPkg.ctx.position();
        int originLimit = cPkg.ctx.limit();

        int emitCount = key.emitOther(keyName, socketChannel, new Key() {
            @Override
            public void emitRun(SocketChannel targetSocketChannel) {
                try {
                    cPkg.ctx.position(originPosition);
                    cPkg.ctx.limit(originLimit);
                    targetSocketChannel.write(cPkg.ctx);
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        });

        cPkg.ctx.clear();
        out.println("[發送對象數] " + emitCount);
    }
}