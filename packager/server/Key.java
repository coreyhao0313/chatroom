package packager.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

import static java.lang.System.out;

public class Key {
    public Map<Integer, String> keys;
    public Map<String, ArrayList<SocketChannel>> groups;

    public Key() {
        this.keys = new HashMap<Integer, String>();
        this.groups = new HashMap<String, ArrayList<SocketChannel>>();
    }

    public void handle(ByteBuffer byteBuffer, SocketChannel socketChannel, int who) {
        int keyLeng = byteBuffer.remaining();
        byte[] keyByte = new byte[keyLeng];
        byteBuffer.get(keyByte, 0, keyLeng);
        String keyName = new String(keyByte);

        if(this.register(who, keyName, socketChannel) == 1){
            out.println("[Key 登入] " + keyName);
        } else {
            out.println("[Key 建立] " + keyName);
        }
    }

    public short register(int who, String keyName, SocketChannel socketChannel) {
        short state;
        try {
            this.groups.get(keyName).add(socketChannel);
            state = 1;
        } catch (NullPointerException err) {
            this.groups.put(keyName, new ArrayList<SocketChannel>());
            this.groups.get(keyName).add(socketChannel);
            state = 2;
        }
        this.keys.put(who, keyName);
        return state;
    }

    public void remove(int who, SocketChannel socketChannel){
        String keyName = this.keys.get(who);

        if (keyName != null) {
            ArrayList<SocketChannel> keySocketChannels = this.groups.get(keyName);
            if(keySocketChannels != null){
                keySocketChannels.remove(socketChannel);
                if (keySocketChannels.size() == 0) {
                    this.groups.remove(keyName);
                }
            }
            this.keys.remove(who);
        }
    }

    public String getName(int who){
        return this.keys.get(who);
    }
    public ArrayList<SocketChannel> getMembers(String keyName){
        return this.groups.get(keyName);
    }

    public void emitRun(SocketChannel socketChannel){
        ;
    }

    public int emitOther(String keyName, SocketChannel socketChannel, Key actKey){ // actKey for targer emitRun
        ArrayList<SocketChannel> socketChannels = this.getMembers(keyName);
        Iterator<SocketChannel> socketChannelsIterator = socketChannels.iterator();
        while (socketChannelsIterator.hasNext()) {
            SocketChannel targetSocketChannel = socketChannelsIterator.next();
            if (targetSocketChannel == socketChannel) {
                continue;
            }
            actKey.emitRun(targetSocketChannel);
        }

        return socketChannels.size() - 1;
    }
}