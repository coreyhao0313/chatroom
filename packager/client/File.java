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

import packager.State;

public class File {
    public FileOutputStream FOS;
    public BufferedOutputStream BOS;

    public long uploadByteFullSize = 0;
    public long uploadByteSize = 0;

    public long downloadByteFullSize = 0;
    public long downloadByteSize = 0;

    
    public void send(String path, SocketChannel socketChannel) {
        try {
            Path filePath = Paths.get(path).normalize();
            String fileName = filePath.getFileName().toString();
            long fileSize = Files.size(filePath);
            this.uploadByteFullSize = fileSize;
            String fileSizeString = String.valueOf(fileSize);
            out.println("[傳輸檔案] " + fileName);

            FileInputStream FIS = new FileInputStream(path);
            BufferedInputStream BIS = new BufferedInputStream(FIS, 65536);

            byte[] OPByte = { State.FILE.code, 1 };
            byte[] fileNameByte = fileName.getBytes("UTF-8");
            byte[] OPByte_SPLIT = { State.NOTHING.code };
            byte[] fileByte = new byte[2046];
            ByteBuffer ctx = ByteBuffer.allocate(2048);

            ctx.put(OPByte);
            ctx.put(fileNameByte);
            ctx.put(OPByte_SPLIT);
            ctx.put(fileSizeString.getBytes());
            ctx.put(OPByte_SPLIT);

            int fileByteRemaining = ctx.remaining();
            int fileByteLeng = BIS.read(fileByte, 0, fileByteRemaining);

            ctx.put(fileByte, 0, fileByteLeng);
            this.uploadByteSize = fileByteLeng;

            BIS.mark(0);
            BIS.reset();

            ctx.flip();
            socketChannel.write(ctx);
            out.println(this.uploadByteSize + " / " + this.uploadByteFullSize);

            OPByte[1] = 0;
            ctx.clear();
            while ((fileByteLeng = BIS.read(fileByte)) != -1) {
                ctx.put(OPByte);
                ctx.put(fileByte);
                ctx.flip();

                socketChannel.write(ctx);
                this.uploadByteSize += fileByteLeng;
                out.println(this.uploadByteSize + " / " + this.uploadByteFullSize);

                ctx.clear();
            }

            BIS.close();
            this.uploadByteSize = 0;
            this.uploadByteFullSize = 0;

            out.println("[傳輸完成]");
        } catch (Exception err) {
            out.println("[讀檔或傳輸階段失敗]");
        }
    }

    public int handle(ByteBuffer byteBuffer, SocketChannel socketChannel) throws Exception {
        if (byteBuffer.get() == 1) {
            while (byteBuffer.get() != State.NOTHING.code)
                ;
            int fileNameByteLimit = byteBuffer.position();
            while (byteBuffer.get() != State.NOTHING.code)
                ;
            int fileSizeByteLimit = byteBuffer.position();

            int START_POS = 2;
            byteBuffer.position(START_POS);

            int fileNameByteLeng = fileNameByteLimit - 1 - START_POS;
            byte[] fileNameByte = new byte[fileNameByteLeng];
            byteBuffer.get(fileNameByte, 0, fileNameByteLeng);
            String fileName = new String(fileNameByte);

            byteBuffer.position(fileNameByteLimit);

            int fileSizeByteLeng = fileSizeByteLimit - 1 - fileNameByteLimit;
            byte[] fileSizeByte = new byte[fileSizeByteLeng];
            byteBuffer.get(fileSizeByte, 0, fileSizeByteLeng);
            String fileSize = new String(fileSizeByte);
            this.downloadByteFullSize = Long.parseLong(fileSize);
            out.println("[接收檔案] " + fileName);

            this.FOS = new FileOutputStream("./files/" + fileName);
            this.BOS = new BufferedOutputStream(FOS, 65536);

            byteBuffer.position(fileSizeByteLimit);
        }
        byte[] fileByte = new byte[2046];
        int fileByteBufferRemaining = byteBuffer.remaining();
        int fileSizeRemaining = (int) this.downloadByteFullSize - (int) this.downloadByteSize;
        if (fileSizeRemaining < fileByteBufferRemaining) {
            fileByteBufferRemaining = fileSizeRemaining;
        }
        byteBuffer.get(fileByte, 0, fileByteBufferRemaining);
        this.downloadByteSize += fileByteBufferRemaining;

        this.BOS.write(fileByte, 0, fileByteBufferRemaining);
        out.println(this.downloadByteSize + " / " + this.downloadByteFullSize);

        byteBuffer.clear();
        int curBufferLeng;
        while ((curBufferLeng = socketChannel.read(byteBuffer)) > 0) {
            byteBuffer.position(2);

            fileByteBufferRemaining = byteBuffer.remaining();
            fileSizeRemaining = (int) this.downloadByteFullSize - (int) this.downloadByteSize;

            if (fileSizeRemaining < fileByteBufferRemaining) {
                fileByteBufferRemaining = fileSizeRemaining;
            }
            byteBuffer.get(fileByte, 0, fileByteBufferRemaining);
            this.downloadByteSize += fileByteBufferRemaining;

            this.BOS.write(fileByte, 0, fileByteBufferRemaining);
            out.println(this.downloadByteSize + " / " + this.downloadByteFullSize);

            byteBuffer.clear();
        }

        if (this.downloadByteSize >= this.downloadByteFullSize) {
            this.BOS.close();
            this.downloadByteSize = 0;
            this.downloadByteFullSize = 0;

            out.println("[接收完成]");
        }

        return curBufferLeng;
    }
}