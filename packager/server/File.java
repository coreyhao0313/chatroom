package packager.server;

// import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static java.lang.System.out;

import base.State;
import packager.Parser;
import packager.Packager;

public class File {
    public static String downloadName = null;
    public static long downloadTotalSize = 0;

    public static void handle(Parser pkg, SocketChannel socketChannel, Integer targetKey, Key key) throws Exception {
        String keyName = key.getName(targetKey);

        // out.println("[" + clientRemoteAddress + "/" + key + "/傳輸檔案] ");

        Packager clientPkg = new Packager(4096);
        byte[] fileBytes = new byte[4096 - Packager.INFO_LENG];

        pkg.setKeep(true);
        pkg.fetch(socketChannel, new Parser() {
            @Override
            public void over(Parser self){
                // clientPkg.setHead(State.FILE.code, self.dataLeng);
            }

            @Override
            public void get(Parser self) {
                // key.emitOther(keyName, socketChannel, new Key() {
                // @Override
                // public void emitRun(SocketChannel targetSocketChannel) {
                // try {
                // byte[] fileBytes = new byte[self.dataLeng];
                // self.ctx.get(fileBytes);
                // clientPkg.setHead(State.FILE.code, fileBytes.length);
                // clientPkg.write(fileBytes);
                // clientPkg.sendTo(targetSocketChannel);
                // } catch (Exception err) {
                // out.println("[對象發送失敗]");
                // }
                // }
                // });

                try {
                    out.println("get >> collPOS >>" + self.collPOS);
                    out.println("get >> collLeng >>" + self.collLeng);
                    out.println("get >> maxLeng >> " + self.maxLeng);
                    out.println("get >> dataLeng >> " + self.dataLeng);
                    out.println("get >> getDataRemaining() >> " + self.getDataRemaining());
                    out.println("get >> hasOver() >> " + self.hasOver());
                    out.println("get >> hasDone() >> " + self.hasDone());

                    // out.println(self.dataLeng);
                    // self.ctx.get(fileBytes);
                    // clientPkg.write(fileBytes);
                    // clientPkg.sendTo(socketChannel);
                } catch (Exception err) {
                    // err.printStackTrace();
                    // out.println("[對象發送失敗]");
                }
            }
            
            @Override
            public void finish(Parser self){
                out.println("################################");
                out.println("finish >> dataLeng >>" + self.dataLeng);
                out.println("################################");
            }
        });

        // out.println("[發送對象數] " + emitCount);
    }
}