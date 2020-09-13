package packager.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.lang.System.out;

import base.State;
import packager.Parser;
import packager.Packager;

public class File {
    public static void handle(Parser pkg, SocketChannel socketChannel, Integer targetKey, Key key) throws Exception {

        if (pkg.evtSelf == null) {
            System.out.println("file method created");
            Packager cPkg = new Packager(4096);
            cPkg.bind(State.FILE, 3);

            pkg.setProceeding(true);
            Parser evt = new Parser() {
                public String fileName = null;
                public long fileSize;
                public String keyName = key.getName(targetKey);

                @Override
                public void breakPoint(Parser self) {
                    try {
                        if (fileName != null && fileSize != 0) {
                            return;
                        }
                        byte[] stuffBytes = self.getBytes();

                        out.print("[" + socketChannel.getRemoteAddress().toString() + "] ");

                        if (fileName == null) {
                            fileName = new String(stuffBytes);
                            out.println("[檔案名稱] " + fileName);

                            cPkg.write(stuffBytes);

                        } else if (fileSize == 0) {
                            fileSize = ByteBuffer.wrap(stuffBytes).getLong();
                            out.println("[檔案大小] " + fileSize);

                            cPkg.proceed();
                            cPkg.write(stuffBytes);
                        }

                        cPkg.ctx.flip();
                        int originPosition = cPkg.ctx.position();
                        int originLimit = cPkg.ctx.limit();
                        key.emitOther(keyName, socketChannel, new Key() {
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

                        if(fileName != null && fileSize != 0){
                            cPkg.proceed();
                            cPkg.setHead(State.FILE, (int) fileSize);
                        }
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }

                @Override
                public void get(Parser self) {
                    if (fileSize != 0) {
                        try {
                            byte[] stuffBytes = self.getBytes();

                            cPkg.write(stuffBytes);
                            cPkg.ctx.flip();
                            int originPosition = cPkg.ctx.position();
                            int originLimit = cPkg.ctx.limit();
                            key.emitOther(keyName, socketChannel, new Key() {
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
                        } catch (Exception err) {
                            err.printStackTrace();
                        }
                    }
                }

                @Override
                public void finish(Parser self) {
                    System.out.println("file method is going to destroy");
                }

                // @Override
                // public void get(Parser self){
                //     debug.packager.Parser p = new debug.packager.Parser(self);
                //     p.log();
                // }
            };

            pkg.fetch(socketChannel, evt);
        } else {
            System.out.println("file method ...");
            pkg.fetch(socketChannel);
        }

        // out.println("[發送對象數] " + emitCount);
    }
}