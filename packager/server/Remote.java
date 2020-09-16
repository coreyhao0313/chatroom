package packager.server;

import java.nio.channels.SocketChannel;

import static java.lang.System.out;

import base.State;
import packager.Parser;
import packager.Packager;

public class Remote {
    public static void handle(Parser pkg, SocketChannel socketChannel, Integer targetKey, Key key) throws Exception {

        if (pkg.evtSelf == null) {
            Packager cPkg = new Packager(1024);
            cPkg.bind(State.REMOTE, 2);

            pkg.setProceeding(true);
            Parser evt = new Parser() {
                public Integer keyboardCode;
                public byte keyboardState;
                public String keyName = key.getName(targetKey);
                public String userFromInfo = socketChannel.getRemoteAddress().toString();

                @Override
                public void breakPoint(Parser self) {
                    byte[] stuffBytes = self.getBytes();
                    try {
                        cPkg.write(stuffBytes);

                        if (this.keyboardCode == null) {
                            cPkg.breakPoint();
                            String stuffString = new String(stuffBytes);
                            this.keyboardCode = Integer.parseInt(stuffString);
                        } else if (this.keyboardState == 0) {
                            this.keyboardState = stuffBytes[0];
                        }
                    } catch(Exception err){
                        err.printStackTrace();
                    }
                }

                public void finish(Parser self) {
                    cPkg.ctx.flip();
                    int originPosition = cPkg.ctx.position();
                    int originLimit = cPkg.ctx.limit();
                    int emitCount = key.emitOther(this.keyName, socketChannel, new Key() {
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
                    out.println("[" + this.userFromInfo + "/傳輸控制訊號] " + keyboardState + " >> " + this.keyboardCode);
                    out.println("[發送對象數] " + emitCount);
                }
            };
            pkg.fetch(socketChannel, evt);
        } else {
            pkg.fetch(socketChannel);
        }
    }
}