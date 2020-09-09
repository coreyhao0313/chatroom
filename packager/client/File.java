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

            Packager pkgFileName = new Packager(512);
            byte[] fileNameBytes = fileName.getBytes("UTF-8");
            pkgFileName.setHead(State.FILE);
            pkgFileName.write(fileNameBytes);
            pkgFileName.sendTo(socketChannel);

            Packager pkgFileSize = new Packager(Long.BYTES + Head.INFO.LENG);
            byte[] fileSizeBytes = ByteBuffer.allocate(Long.BYTES).putLong(fileSize).array();
            pkgFileSize.setHead(State.FILE);
            pkgFileSize.write(fileSizeBytes);
            pkgFileSize.sendTo(socketChannel);

            Packager pkgFile = new Packager(4096);
            pkgFile.setHead(State.FILE, (int)fileSize);
            byte[] fileBytes = new byte[pkgFile.ctx.capacity() - Head.INFO.LENG];

            int fileByteLeng = BIS.read(fileBytes);
            
            do {
                if(fileBytes.length > fileByteLeng){
                    byte[] fileByteFinalLeng = new byte[fileByteLeng];
                    System.arraycopy(fileBytes, 0, fileByteFinalLeng, 0, fileByteLeng);
                    fileBytes = fileByteFinalLeng;
                }
                pkgFile.write(fileBytes);
                pkgFile.sendTo(socketChannel);

                this.uploadSize += fileByteLeng;
                out.println(this.uploadSize + " / " + this.uploadTotalSize);

                fileBytes = new byte[pkgFile.ctx.capacity()];
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

        pkg.setProceeding(false);
        pkg.fetch(socketChannel, new Parser() {
            @Override
            public void breakPoint(Parser self) {
                byte[] stuffBytes = self.getBytes();
                self.ctx.get(stuffBytes);
                
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
                    } catch (Exception err){
                        err.printStackTrace();
                    }
                }
            }

            @Override
            public void get(Parser self) {
                if(BOS != null){
                    byte[] fileBytes = self.getBytes();
                    self.ctx.get(fileBytes);

                    try {
                        BOS.write(fileBytes);
                        downloadSize += fileBytes.length;
                        out.println(downloadSize + " / " + downloadTotalSize);
                    } catch (Exception err){
                        err.printStackTrace();
                    }
                }
            }

            @Override
            public void finish(Parser self) {
                if(BOS != null){
                    try{
                        out.println("[下載完成]");
                        BOS.close();
                    } catch(Exception err){
                        err.printStackTrace();
                    }
                }
            }
        });
        return;
    }
}