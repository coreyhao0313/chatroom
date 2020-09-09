package packager.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.lang.System.out;

import base.State;
import packager.Parser;
import packager.Packager;

public class File {
    // public static String fileName = null;
    // public static long fileSize = 0;


    public static void handle(Parser pkg, SocketChannel socketChannel, Integer targetKey, Key key) throws Exception {
        pkg.setProceeding(true);

        Parser evtPkg = new Parser() {
            public String fileName = null;
            public long fileSize;
            public Packager cPkg = new Packager(1024);
            public String keyName = key.getName(targetKey);

            @Override
            public void breakPoint(Parser self) {
                try {
                    debug.packager.Parser p = new debug.packager.Parser(self);
                    p.log();
                    if (fileName != null && fileSize != 0) {
                        return;
                    }
                    // byte[] stuffBytes = self.getBytes();

                    // cPkg.ctx.clear();
                    // cPkg.setHead(State.FILE);
                    // cPkg.write(stuffBytes);

                    if (fileName == null) {
                        fileName = "!!!";
                    //     fileName = new String(stuffBytes);

                    //     out.print("[" + socketChannel.getRemoteAddress().toString() + "] ");
                    //     out.println("[檔案名稱] " + fileName);
                    System.out.println(fileName);
                        return;
                    }

                    // key.emitOther(keyName, socketChannel, new Key() {
                    //     @Override
                    //     public void emitRun(SocketChannel targetSocketChannel) {
                    //         try {
                    //             cPkg.ctx.position(0);
                    //             targetSocketChannel.write(cPkg.ctx);
                    //         } catch (Exception err) {
                    //             err.printStackTrace();
                    //         }
                    //     }
                    // });

                    if (fileSize == 0) {
                        fileSize = 1;
                    //     fileSize = ByteBuffer.wrap(stuffBytes).getLong();

                    //     out.print("[" + socketChannel.getRemoteAddress().toString() + "] ");
                    //     out.println("[檔案大小] " + fileSize);

                    //     // cPkg.proceed();
                    //     // cPkg.setHead(State.FILE, (int) fileSize);
                    System.out.println(fileSize);
                    }
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }

            @Override
            public void get(Parser self) {
                // if (fileSize != 0) {
                //     try {
                //         byte[] stuffBytes = self.getBytes();
                //         cPkg.write(stuffBytes);

                //         key.emitOther(keyName, socketChannel, new Key() {
                //             @Override
                //             public void emitRun(SocketChannel targetSocketChannel) {
                //                 try {
                //                     cPkg.ctx.position(0);
                //                     targetSocketChannel.write(cPkg.ctx);
                //                 } catch (Exception err) {
                //                     err.printStackTrace();
                //                 }
                //             }
                //         });

                //         cPkg.ctx.clear();
                //     } catch (Exception err) {
                //         err.printStackTrace();
                //     }
                // }
            }
        };
        pkg.fetch(socketChannel, evtPkg);

        // out.println("[發送對象數] " + emitCount);
    }
}