package packager.server;

import java.nio.channels.SocketChannel;

import static java.lang.System.out;

import base.State;
import base.packager.server.KeyEvent;
import packager.Parser;
import packager.Packager;

public class Message implements KeyEvent {
    public Packager cPkg;
    public int originPosition;
    public int originLimit;

    public Message(byte[] remoteAddressBytes, byte[] messageBytes) {
        try {
            this.cPkg = new Packager(2048);
            this.cPkg.bind(State.MESSAGE, 2);

            this.cPkg.write(remoteAddressBytes);
            this.cPkg.breakPoint();
            this.cPkg.write(messageBytes);

            this.cPkg.ctx.flip();

            this.originPosition = cPkg.ctx.position();
            this.originLimit = cPkg.ctx.limit();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public static void handle(Parser pkg, SocketChannel socketChannel, Integer targetKey, Key key) throws Exception {
        String keyName = key.getName(targetKey);

        pkg.fetch(socketChannel);
        String remoteAddressString = socketChannel.getRemoteAddress().toString();
        byte[] messageBytes = pkg.getBytes();

        if (messageBytes.length == 0) {
            return;
        }

        out.println("[" + remoteAddressString + "/" + keyName + "/傳輸訊息] ");
        out.println(new String(messageBytes));

        Message sender = new Message(remoteAddressString.getBytes(), messageBytes);
        int emitCount = key.emitOther(keyName, socketChannel, sender);

        out.println("[發送對象數] " + emitCount);
    }

    @Override
    public void everyOther(SocketChannel targetSocketChannel) {
        try {
            this.cPkg.ctx.position(this.originPosition);
            this.cPkg.ctx.limit(this.originLimit);
            targetSocketChannel.write(this.cPkg.ctx);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
}