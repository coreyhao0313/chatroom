package base.packager;

public interface ParserEvent {
    public void get(packager.Parser self);
    public void finish(packager.Parser self);
    public void breakPoint(packager.Parser self);
}