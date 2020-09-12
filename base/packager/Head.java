package base.packager;

public enum Head {
    PREFIX(3, (byte) 0x00),
    TYPE(1, (byte) 0x00),
    SIZE(4, (byte) 0x00),
    INFO(3 + 1 + 4, (byte) 0x00), // PREFIX.LENG(HEAD_1 + HEAD_2 + NULL) + TYPE.LENG + SIZE.LENG
    BINDING(1, (byte) 0x00), // COUNT
    BINDING_INFO(3 + 1 + 1, (byte) 0x00), // PREFIX.LENG(HEAD_1 + HEAD_2 + HEAD_3) + TYPE.LENG + BINDING.LENG(COUNT)

    HEAD_NULL(0, (byte) 0),
    HEAD_1(0, (byte) 63),
    HEAD_2(0, (byte) 79),
    HEAD_3(0, (byte) 62);

    public final int LENG;
    public final byte CODE;

    Head(int leng, byte code){
        this.LENG = leng;
        this.CODE = code;
    }
}