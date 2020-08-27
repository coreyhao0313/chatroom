package packager;

public interface Csocket<S> {
    public void setMainHandler();
    public void setConnectHandler(S arg);
}