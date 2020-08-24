package server;

public enum Control {
    UNDEFINED((byte)0x00, "未定義"),
    NOTHING((byte)0x01, "-"),
    KEY((byte)0x0A, "KEY 輸入"),
    MESSAGE((byte)0x0B, "訊息輸入"),
    FILE((byte)0x0C, "檔案流"),
    REMOTE((byte)0x0D, "遠端流");

    public final byte code;
    public final String desc;

    Control(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public String getDesc(){
        return this.desc;
    }
}