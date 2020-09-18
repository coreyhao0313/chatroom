package packager.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.lang.System.out;

import base.State;
import base.packager.ParserEvent;
import base.packager.server.KeyEvent;
import packager.Parser;
import packager.Packager;

public class File implements ParserEvent, KeyEvent {
    public Packager cPkg;
    public SocketChannel socketChannel;
    public Key key;
    public String keyName;
    public String remoteAddressString;
    public String fileName;
    public long fileSize;
    public int originPosition;
    public int originLimit;

    public File(SocketChannel socketChannel, Integer targetKey, Key key) {
        try {
            this.cPkg = new Packager(4096);
            this.cPkg.bind(State.FILE, 3);

            this.key = key;
            this.keyName = key.getName(targetKey);
            this.socketChannel = socketChannel;
            this.remoteAddressString = socketChannel.getRemoteAddress().toString();
            this.fileName = null;
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public static void handle(Parser pkg, SocketChannel socketChannel, Integer targetKey, Key key) throws Exception {
        if (pkg.parserEvent == null) {
            System.out.println("file method created");
            pkg.setProceeding(true);

            File receiver = new File(socketChannel, targetKey, key);
            pkg.fetch(socketChannel, receiver);
        } else {
            System.out.println("file method ...");
            pkg.fetch(socketChannel);
        }
        // out.println("[發送對象數] " + emitCount);
    }

    @Override
    public void breakPoint(Parser self) {
        try {
            if (this.fileName != null && this.fileSize != 0) {
                return;
            }
            byte[] stuffBytes = self.getBytes();

            out.print("[" + this.remoteAddressString + "] ");

            if (this.fileName == null) {
                this.fileName = new String(stuffBytes);
                out.println("[檔案名稱] " + this.fileName);

                this.cPkg.write(stuffBytes);

            } else if (this.fileSize == 0) {
                this.fileSize = ByteBuffer.wrap(stuffBytes).getLong();
                out.println("[檔案大小] " + this.fileSize);

                this.cPkg.proceed();
                this.cPkg.write(stuffBytes);
            }

            this.cPkg.ctx.flip();
            this.originPosition = this.cPkg.ctx.position();
            this.originLimit = this.cPkg.ctx.limit();

            File sender = this;
            this.key.emitOther(this.keyName, socketChannel, sender);

            this.cPkg.ctx.clear();

            if (this.fileName != null && this.fileSize != 0) {
                this.cPkg.proceed();
                this.cPkg.setHead(State.FILE, (int) this.fileSize);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    // @Override
    // public void get(Parser self) {
    // debug.packager.Parser p = new debug.packager.Parser(self);
    // p.log();
    // }

    @Override
    public void get(Parser self) {
        if (this.fileSize != 0) {
            try {
                byte[] stuffBytes = self.getBytes();

                this.cPkg.write(stuffBytes);
                this.cPkg.ctx.flip();
                this.originPosition = this.cPkg.ctx.position();
                this.originLimit = this.cPkg.ctx.limit();

                File sender = this;
                this.key.emitOther(this.keyName, socketChannel, sender);

                this.cPkg.ctx.clear();
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    @Override
    public void finish(Parser self) {
        System.out.println("file method is going to destroy");
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