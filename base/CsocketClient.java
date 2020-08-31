package base;

import java.nio.channels.SocketChannel;

public interface CsocketClient extends Csocket<SocketChannel> {
    public void createConnection(String address, int port);
    public void handler(SocketChannel socketChannel);
    public int dispatch(SocketChannel socketChannel) throws Exception;
}