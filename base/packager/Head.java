package base.packager;

public enum Head {
    SIZE(4, (byte) 0x00),
    PREFIX(3, (byte) 0x00),
    INFO(4 + 3, (byte) 0x00), // SIZE.CODE + PREFIX.CODE
    HEAD_1(0, (byte) 63),
    HEAD_2(0, (byte) 79);

    public final int LENG;
    public final byte CODE;

    Head(int leng, byte code){
        this.LENG = leng;
        this.CODE = code;
    }
}