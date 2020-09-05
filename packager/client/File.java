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
            out.println("[傳輸檔案] " + fileName);

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

            Packager pkgFile = new Packager(128);
            pkgFile.setHead(State.FILE, (int)fileSize);
            byte[] fileBytes = new byte[pkgFile.ctx.capacity() - Head.INFO.LENG];

            int fileByteLeng = BIS.read(fileBytes);
            
            do {
                pkgFile.write(fileBytes);
                pkgFile.sendTo(socketChannel);

                this.uploadSize += fileByteLeng;
                out.println(this.uploadSize + " / " + this.uploadTotalSize);

                int remaining = fileBytes.length >= fileByteLeng ? fileByteLeng : pkgFile.ctx.capacity();
                System.out.println(remaining);
                fileBytes = new byte[remaining];
            } while ((fileByteLeng = BIS.read(fileBytes)) != -1);

            BIS.close();
            this.uploadSize = 0;
            this.uploadTotalSize = 0;

            out.println("[傳輸完成]");
        } catch (Exception err) {
            err.printStackTrace();
            // out.println("[讀檔或傳輸階段失敗]");
        }
    }

    public void handle(Parser pkg, SocketChannel socketChannel) throws Exception {

        pkg.setKeep(false);
        pkg.fetch(socketChannel, new Parser() {
            @Override
            public void get(Parser self) {
                // out.println(self.getDataRemaining());
                // byte[] stuffBytes = new byte[self.getDataRemaining()];

                // if (downloadName == null) {
                //     byte[] fileNameBytes = stuffBytes;
                //     self.ctx.get(fileNameBytes);
                //     downloadName = new String(fileNameBytes);
                //     createFile(downloadName);

                //     out.println(downloadName);
                //     return;
                // }
                // if (downloadTotalSize == 0) {
                //     byte[] fileSizeBytes = stuffBytes;
                //     self.ctx.get(fileSizeBytes);
                //     downloadTotalSize = ByteBuffer.wrap(fileSizeBytes).getLong();

                //     out.println(downloadTotalSize);
                //     return;
                // }

                // if(self.hasOver()){
                //     self.ctx.get(stuffBytes, 0, self.getOverPOS());
                // }else{
                //     self.ctx.get(stuffBytes);
                // }
                // try {
                //     downloadSize += self.collectedLeng;
                //     out.println(downloadSize + " / " + downloadTotalSize);

                //     BOS.write(stuffBytes);
                // } catch (Exception err){
                //     err.printStackTrace();
                // }
            }

            // @Override
            // public void next(Parser self) {
            // }
        });

        // if (this.downloadSize >= this.downloadTotalSize) {
        //     this.BOS.close();
        //     this.downloadSize = 0;
        //     this.downloadTotalSize = 0;

        //     out.println("[接收完成]");
        // }
        return;
    }

    public boolean createFile (String fileName){
        try {
            this.FOS = new FileOutputStream("./files/" + fileName);
            this.BOS = new BufferedOutputStream(FOS, 65536);
        } catch(Exception err){
            return false;
        }
        return true;
    }
}