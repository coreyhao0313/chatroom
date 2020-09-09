package debug.packager;

import base.packager.Head;

public class Packager {
    public packager.Packager packager;

    public Packager (packager.Packager packager){
        this.packager = packager;
    }

    public void log() {
        System.out.println();
        System.out.println();
        int originPosition = packager.ctx.position();
        if (originPosition >= Head.INFO.LENG) {
            packager.ctx.position(originPosition - Head.INFO.LENG);
        }
        byte[] fileBytes = new byte[packager.ctx.remaining()];
        System.out.println();
        packager.ctx.get(fileBytes);
        for (byte b : fileBytes) {
            System.out.print(b + " ");
        }
        System.out.println();
        packager.ctx.position(originPosition);
    }
}
