package packager.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.lang.System.out;

import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import base.State;
import packager.Parser;
import packager.Packager;

public class File {
    public static String downloadName = null;
    public static long downloadTotalSize = 0;
    public static FileOutputStream FOS;
    public static BufferedOutputStream BOS;

    public static void handle(Parser pkg, SocketChannel socketChannel, Integer targetKey, Key key) throws Exception {
        String keyName = key.getName(targetKey);

        // out.println("[" + clientRemoteAddress + "/" + key + "/傳輸檔案] ");

        Packager clientPkg = new Packager(4096);
        // byte[] fileBytes = new byte[4096 - Packager.INFO_LENG];

        pkg.setKeep(true);
        pkg.fetch(socketChannel, new Parser() {
            @Override
            public void next(Parser self) {
                byte[] stuffBytes = new byte[self.dataLimit];

                if (downloadName == null) {
                    byte[] fileNameBytes = stuffBytes;
                    self.ctx.get(fileNameBytes, 0, self.dataLimit);
                    downloadName = new String(fileNameBytes);

                    out.println(downloadName);
                    return;
                }
                if (downloadTotalSize == 0) {
                    byte[] fileSizeBytes = stuffBytes;
                    self.ctx.get(fileSizeBytes, 0, self.dataLimit);
                    downloadTotalSize = ByteBuffer.wrap(fileSizeBytes).getLong();

                    out.println(downloadTotalSize);

                    out.println("建檔階段");
                    try {
                        FOS = new FileOutputStream("./files/" + downloadName);
                        BOS = new BufferedOutputStream(FOS, 65536);
                    } catch (Exception err){
                        out.println("建檔失敗");
                    }
                }
            }

            @Override
            public void get(Parser self) {
                if(BOS != null){
                    out.println("寫檔 > " + self.ctx.remaining());
                    // out.printf("nextPosition >> %s, collLeng >> %s, leng >> %s, dataLimit >> %s\n", self.nextPosition, self.collLeng, self.leng, self.dataLimit);
                    // out.println("collLeng >> "+ self.collLeng);
                    
                    byte[] fileBytes = new byte[self.ctx.remaining()];
                    
                    self.ctx.get(fileBytes);
                    // for(byte b : fileBytes){
                    //     out.print(b + " ");
                    // }
                    // out.println();
                    // out.println();
                    try {
                        BOS.write(fileBytes);
                    } catch (Exception err){
                        out.println("寫檔失敗");
                    }

                }

                // key.emitOther(keyName, socketChannel, new Key() {
                //     @Override
                //     public void emitRun(SocketChannel targetSocketChannel) {
                //         try {
                //             byte[] fileBytes = new byte[self.dataLeng];
                //             self.ctx.get(fileBytes);
                //             clientPkg.setHead(State.FILE.code, fileBytes.length);
                //             clientPkg.write(fileBytes);
                //             clientPkg.sendTo(targetSocketChannel);
                //         } catch (Exception err) {
                //             out.println("[對象發送失敗]");
                //         }
                //     }
                // });

                // try {
                //     self.ctx.get(fileBytes);
                //     clientPkg.write(fileBytes);
                //     clientPkg.sendTo(socketChannel);
                // } catch (Exception err) {
                //     err.printStackTrace();
                //     // out.println("[對象發送失敗]");
                // }
            }

            @Override
            public void finish(Parser self) {
                out.println(">>> ok");
                try{
                    BOS.close();
                } catch(Exception err){
                    out.println("關檔失敗");
                }
            }
        });

        // out.println("[發送對象數] " + emitCount);
    }
}