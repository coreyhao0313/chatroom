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
        if (originPosition > Head.SIZE.LENG + Head.PREFIX.LENG) {
            parser.ctx.position(originPosition - Head.SIZE.LENG + Head.PREFIX.LENG);
        }
        System.out.println("readableLeng > " + parser.readableLeng + ", ctx.remaining > " + parser.ctx.remaining());
        System.out.println(
                "collLeng > " + parser.collLeng + ", nextPosition > " + parser.nextPosition + ", limit > " + parser.limit);
        System.out.println("leng > " + parser.leng + ", dataLimit > " + parser.dataLimit);
        System.out.println("position() > " + parser.ctx.position() + ", limit() > " + parser.ctx.limit() + ", isDone > " + parser.isDone());
        byte[] fileBytes = new byte[parser.ctx.remaining()];
        System.out.println();
        parser.ctx.get(fileBytes);
        for (byte b : fileBytes) {
            System.out.print(b + " ");
        }
        System.out.println();
        parser.ctx.position(originPosition);
    }
}
