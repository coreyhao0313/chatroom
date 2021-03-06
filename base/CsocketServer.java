package base;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public interface CsocketServer extends Csocket<SelectionKey> {
    public void createConnection(int port);
    public void handler(SocketChannel socketChannel, Integer targetKey);
    public int dispatch(SocketChannel socketChannel, Integer targetKey) throws Exception;
}