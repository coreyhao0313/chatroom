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
import base.packager.ParserEvent;
import packager.Parser;
import packager.Packager;

public class File implements ParserEvent {
    public FileInputStream FIS;
    public BufferedInputStream BIS;

    public String uploadName;
    public long uploadTotalSize;
    public long uploadSize;

    public FileOutputStream FOS;
    public BufferedOutputStream BOS;

    public String downloadName;
    public long downloadTotalSize;
    public long downloadSize;

    public void send(String path, SocketChannel socketChannel) {
        try {
            Path filePath = Paths.get(path).normalize();
            String fileName = filePath.getFileName().toString();
            this.uploadName = fileName;
            long fileSize = Files.size(filePath);
            if (fileSize == 0) {
                out.println("[不可傳輸空檔案]");
                return;
            }
            this.uploadTotalSize = fileSize;
            out.println("[傳輸檔案名稱] " + fileName);

            this.FIS = new FileInputStream(path);
            this.BIS = new BufferedInputStream(this.FIS, 65536);

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
            int capacity = sPkg.ctx.capacity();
            byte[] fileBytes = new byte[capacity - Head.INFO.LENG];

            int fileByteLeng = this.BIS.read(fileBytes);

            do {
                if (fileBytes.length > fileByteLeng) {
                    byte[] fileFinalBytes = new byte[fileByteLeng];
                    System.arraycopy(fileBytes, 0, fileFinalBytes, 0, fileByteLeng);
                    fileBytes = fileFinalBytes;
                }
                sPkg.write(fileBytes);
                sPkg.sendTo(socketChannel);

                this.uploadSize += fileByteLeng;
                out.println(this.uploadSize + " / " + this.uploadTotalSize);

                fileBytes = new byte[capacity];
            } while ((fileByteLeng = this.BIS.read(fileBytes)) != -1);

            this.BIS.close();

            out.println("[傳輸檔案完成]");
        } catch (Exception err) {
            err.printStackTrace();
            // out.println("[讀檔或傳輸階段失敗]");
        } finally{
            this.resetUpload();
        }
    }

    public void handle(Parser pkg, SocketChannel socketChannel) throws Exception {
        if (pkg.parserEvent == null) {
            pkg.setProceeding(true);

            File receiver = this;
            receiver.resetDownload();
            pkg.fetch(socketChannel, receiver);
        } else {
            pkg.fetch(socketChannel);
        }
        return;
    }

    @Override
    public void breakPoint(Parser self) {
        if (this.downloadName != null && this.downloadTotalSize != 0) {
            return;
        }
        byte[] stuffBytes = self.getBytes();

        if (this.downloadName == null) {
            this.downloadName = new String(stuffBytes);
            out.println("[接收檔案名稱] " + this.downloadName);
        } else if (this.downloadTotalSize == 0) {
            
            this.downloadTotalSize = ByteBuffer.wrap(stuffBytes).getLong();
            out.println("[接收檔案大小] " + this.downloadTotalSize);
            out.println("[建檔階段]");
            try {
                this.FOS = new FileOutputStream("./files/" + this.downloadName);
                this.BOS = new BufferedOutputStream(this.FOS, 65536);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    @Override
    public void get(Parser self) {
        if (this.BOS != null) {
            byte[] fileBytes = self.getBytes();

            try {
                this.BOS.write(fileBytes);
                this.downloadSize += fileBytes.length;
                out.println(this.downloadSize + " / " + this.downloadTotalSize);
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    @Override
    public void finish(Parser self) {
        if (this.BOS != null) {
            try {
                this.BOS.close();
                out.println("[下載完成]");
            } catch (Exception err) {
                err.printStackTrace();
            } finally {
                this.resetDownload();
            }
        }
    }

    public void resetDownload(){
        this.BOS = null;
        this.FOS = null;
        this.downloadName = null;
        this.downloadSize = 0;
        this.downloadTotalSize = 0;
    }

    public void resetUpload(){
        this.BIS = null;
        this.FIS = null;
        this.uploadName = null;
        this.uploadSize = 0;
        this.uploadTotalSize = 0;
    }
}