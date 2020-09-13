package packager.client;

import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import static java.lang.System.out;

import base.State;
import base.packager.Head;
import packager.Parser;
import packager.Packager;

public class File {
    public FileOutputStream FOS;
    public BufferedOutputStream BOS;

    public long uploadTotalSize = 0;
    public long uploadSize = 0;

    public String downloadName;
    public long downloadTotalSize;
    public long downloadSize;

    public void send(String path, SocketChannel socketChannel) {
        try {
            Path filePath = Paths.get(path).normalize();
            String fileName = filePath.getFileName().toString();
            long fileSize = Files.size(filePath);
            if (fileSize == 0) {
                out.println("[不可傳輸空檔案]");
                return;
            }
            this.uploadTotalSize = fileSize;
            out.println("[傳輸檔案名稱] " + fileName);

            FileInputStream FIS = new FileInputStream(path);
            BufferedInputStream BIS = new BufferedInputStream(FIS, 65536);

            Packager sPkg = new Packager(1024);
            sPkg.bind(State.FILE, 3);

            byte[] fileNameBytes = fileName.getBytes("UTF-8");
            sPkg.write(fileNameBytes);
            sPkg.sendTo(socketChannel);
            sPkg.proceed();

            byte[] fileSizeBytes = ByteBuffer.allocate(Long.BYTES).putLong(fileSize).array();
            sPkg.write(fileSizeBytes);
            sPkg.sendTo(socketChannel);
            sPkg.proceed();

            sPkg.setHead(State.FILE, (int) fileSize);
            byte[] fileBytes = new byte[sPkg.ctx.capacity() - Head.INFO.LENG];

            int fileByteLeng = BIS.read(fileBytes);

            do {
                if (fileBytes.length > fileByteLeng) {
                    byte[] fileByteFinalLeng = new byte[fileByteLeng];
                    System.arraycopy(fileBytes, 0, fileByteFinalLeng, 0, fileByteLeng);
                    fileBytes = fileByteFinalLeng;
                }
                sPkg.write(fileBytes);
                sPkg.sendTo(socketChannel);

                this.uploadSize += fileByteLeng;
                out.println(this.uploadSize + " / " + this.uploadTotalSize);

                fileBytes = new byte[sPkg.ctx.capacity()];
            } while ((fileByteLeng = BIS.read(fileBytes)) != -1);

            BIS.close();
            this.uploadSize = 0;
            this.uploadTotalSize = 0;

            out.println("[傳輸檔案完成]");
        } catch (Exception err) {
            err.printStackTrace();
            // out.println("[讀檔或傳輸階段失敗]");
        }
    }

    public void handle(Parser pkg, SocketChannel socketChannel) throws Exception {

        if (pkg.evtSelf == null) {
            System.out.println("file method created");

            pkg.setProceeding(true);

            Parser evt = new Parser() {
                @Override
                public void breakPoint(Parser self) {
                    if (downloadName != null && downloadTotalSize != 0) {
                        return;
                    }
                    byte[] stuffBytes = self.getBytes();

                    if (downloadName == null) {
                        downloadName = new String(stuffBytes);
                        out.println("[接收檔案名稱] " + downloadName);
                    } else if (downloadTotalSize == 0) {
                        
                        downloadTotalSize = ByteBuffer.wrap(stuffBytes).getLong();
                        out.println("[接收檔案大小] " + downloadTotalSize);
                        out.println("[建檔階段]");
                        try {
                            FOS = new FileOutputStream("./files/" + downloadName);
                            BOS = new BufferedOutputStream(FOS, 65536);
                        } catch (Exception err) {
                            err.printStackTrace();
                        }
                    }
                }

                @Override
                public void get(Parser self) {
                    if (BOS != null) {
                        byte[] fileBytes = self.getBytes();

                        try {
                            BOS.write(fileBytes);
                            downloadSize += fileBytes.length;
                            out.println(downloadSize + " / " + downloadTotalSize);
                        } catch (Exception err) {
                            err.printStackTrace();
                        }
                    }
                }

                @Override
                public void finish(Parser self) {
                    if (BOS != null) {
                        try {
                            out.println("[下載完成]");
                            BOS.close();
                        } catch (Exception err) {
                            err.printStackTrace();
                        } finally {
                            downloadName = null;
                            downloadSize = 0;
                            downloadTotalSize = 0;
                        }
                    }
                }

                // @Override
                // public void get(Parser self) {
                //     // byte[] stuffBytes = self.getBytes();

                //     // System.out.println();
                //     // System.out.println();
                //     // for (byte t : stuffBytes) {
                //     //     System.out.print(t + " ");
                //     // }
                //     // System.out.println();
                //     // System.out.println();

                //     // debug.packager.Parser p = new debug.packager.Parser(self);
                //     // p.log();
                // }
            };

            pkg.fetch(socketChannel, evt);
        } else {
            System.out.println("file method ...");
            pkg.fetch(socketChannel);
        }
        return;
    }
}