package packager.server;

import java.nio.channels.SocketChannel;

import static java.lang.System.out;

import base.State;
import base.packager.ParserEvent;
import base.packager.server.KeyEvent;
import packager.Parser;
import packager.Packager;

public class Remote implements ParserEvent, KeyEvent {
    public Integer keyboardCode;
    public byte keyboardState;
    public String remoteAddressString;
    public Key key;
    public String keyName;
    public Packager cPkg;
    public int originPosition;
    public int originLimit;
    public SocketChannel socketChannel;

    public Remote(SocketChannel socketChannel, Integer targetKey, Key key) {
        try {
            this.key = key;
            this.keyName = this.key.getName(targetKey);
            this.socketChannel = socketChannel;
            this.remoteAddressString = socketChannel.getRemoteAddress().toString();

            this.cPkg = new Packager(1024);
            this.cPkg.bind(State.REMOTE, 2);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public static void handle(Parser pkg, SocketChannel socketChannel, Integer targetKey, Key key) throws Exception {
        if (pkg.parserEvent == null) {
            pkg.setProceeding(true);

            Remote receiver = new Remote(socketChannel, targetKey, key);
            pkg.fetch(socketChannel, receiver);
        } else {
            pkg.fetch(socketChannel);
        }
    }

    @Override
    public void get(Parser self) {
        ;
    }

    @Override
    public void breakPoint(Parser self) {
        byte[] stuffBytes = self.getBytes();
        try {
            this.cPkg.write(stuffBytes);

            if (this.keyboardCode == null) {
                this.cPkg.breakPoint();
                String stuffString = new String(stuffBytes);
                this.keyboardCode = Integer.parseInt(stuffString);
            } else if (this.keyboardState == 0) {
                this.keyboardState = stuffBytes[0];
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public void finish(Parser self) {
        this.cPkg.ctx.flip();

        this.originPosition = this.cPkg.ctx.position();
        this.originLimit = this.cPkg.ctx.limit();

        Remote sender = this;
        int emitCount = this.key.emitOther(this.keyName, this.socketChannel, sender);
        out.printf("[%s/%s/傳控制鍵] %x >> %d\n", this.remoteAddressString, this.keyName, this.keyboardState, this.keyboardCode);
        out.println("[發送對象數] " + emitCount);
    }

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