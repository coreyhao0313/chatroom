package debug.packager;

import base.packager.Head;

public class Parser {
    public packager.Parser parser;

    public Parser (packager.Parser parser){
        this.parser = parser;
    }

    public void log() {
        System.out.println();
        System.out.println();
        int originPosition = parser.ctx.position();
        if (originPosition >= Head.INFO.LENG) {
            parser.ctx.position(originPosition - Head.INFO.LENG);
        }
        System.out.println("readableLeng > " + parser.readableLeng + ", ctx.remaining > " + parser.ctx.remaining());
        System.out.println("nextPosition > " + parser.nextPosition + ", limit > " + parser.limit);
        System.out.println("leng > " + parser.leng + ", dataLimit > " + parser.dataLimit);
        System.out.println("position() > " + parser.ctx.position() + ", limit() > " + parser.ctx.limit() + ", isBreakPoint > " + parser.isBreakPoint());
        System.out.println("isFinish() > " + parser.isFinish());
        
        byte[] fileBytes = new byte[parser.ctx.remaining()];
        System.out.println();
        parser.ctx.get(fileBytes);
        for (byte b : fileBytes) {
            System.out.print(b + " ");
        }
        System.out.println();
        parser.ctx.position(originPosition);
    }

    public void log2(){
        System.out.println("position() >> " + parser.ctx.position());
        System.out.println("remaining() >> " + parser.ctx.remaining());
        System.out.println("limit() >> " + parser.ctx.limit());
        System.out.println("nextPosition >> " + parser.nextPosition);
        System.out.println("readableLeng >> " + parser.readableLeng);
        // System.out.println("blockLimit >> " + blockLimit);
        System.out.println("##################################");
    }
}
