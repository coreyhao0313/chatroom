package packager.client;

import java.nio.channels.SocketChannel;

import static java.lang.System.out;

import base.State;
import packager.Parser;
import packager.Packager;

public class Message {

    public static void send(SocketChannel socketChannel, String inputText) throws Exception {
        byte[] textBytes = inputText.getBytes("UTF-8");

        Packager sPkg = new Packager(1024);
        sPkg.setHead(State.MESSAGE);
        sPkg.write(textBytes);
        sPkg.sendTo(socketChannel);
    }

    public static void handle(Parser pkg, SocketChannel socketChannel) {
        if (pkg.evtSelf == null) {
            pkg.setProceeding(true);
            Parser evt = new Parser() {
                public String user;
                public String message;

                @Override
                public void breakPoint(Parser parser) {
                    if (this.user != null && this.message != null) {
                        return;
                    }
                    byte[] stuffBytes = parser.getBytes();
                    String stuffString = new String(stuffBytes);

                    if (this.user == null) {
                        this.user = stuffString;
                    } else if (this.message == null) {
                        this.message = stuffString;
                    }
                }

                @Override
                public void finish(Parser parser) {
                    out.print("[" + this.user + "] ");
                    out.println(this.message);
                }
            };
            pkg.fetch(socketChannel, evt);
        } else {
            pkg.fetch(socketChannel);
        }
    }
}