package base;

public interface Csocket<S> {
    public void setMainHandler();
    public void setConnectHandler(S arg);
}