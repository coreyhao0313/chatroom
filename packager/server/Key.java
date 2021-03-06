package packager.server;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

import static java.lang.System.out;

import base.packager.server.KeyEvent;

import packager.Parser;
public class Key implements KeyEvent{
    public Map<Integer, String> keys;
    public Map<String, ArrayList<SocketChannel>> groups;

    public Key() {
        this.keys = new HashMap<Integer, String>();
        this.groups = new HashMap<String, ArrayList<SocketChannel>>();
    }

    public void handle(Parser pkg, SocketChannel socketChannel, int who) {
        pkg.fetch(socketChannel);
        String keyName = new String(pkg.getBytes());
        
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

    public void everyOther(SocketChannel targetSocketChannel){
        ;
    }

    public int emitOther(String keyName, SocketChannel socketChannel, KeyEvent keyEvent){
        ArrayList<SocketChannel> socketChannels = this.getMembers(keyName);
        Iterator<SocketChannel> socketChannelsIterator = socketChannels.iterator();
        while (socketChannelsIterator.hasNext()) {
            SocketChannel targetSocketChannel = socketChannelsIterator.next();
            if (targetSocketChannel == socketChannel) {
                continue;
            }
            keyEvent.everyOther(targetSocketChannel);
        }

        return socketChannels.size() - 1;
    }
}