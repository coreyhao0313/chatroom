package packager.client;

import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.awt.Robot;

import static java.lang.System.out;

import base.State;
import client.Keyboard;

public class Remote {
    public Robot robot;

    public void setOutputController(){
        try {
            this.robot = new Robot();
            out.println("[允許作為受控端]");
        } catch (Exception Err) {
            Err.printStackTrace();
        }
    }

    public void setKeyboardSender(SocketChannel socketChannel) {
        byte[] OPBytes = { State.REMOTE.CODE };
        ByteBuffer ctx = ByteBuffer.allocate(8);
        byte[] keyBoardTypeBytes = new byte[1];
        byte[] keyboardByteLeng = new byte[1];

        Keyboard keyboard = new Keyboard() {
            public void keyHandler(java.awt.event.KeyEvent evt, byte keyboardType) {
                int keyboardInt = evt.getKeyCode();
                evt.consume();
                byte[] keyboardBytes = String.valueOf(keyboardInt).getBytes();
                this.text.setText("");

                ctx.put(OPBytes);

                keyBoardTypeBytes[0] = keyboardType;
                ctx.put(keyBoardTypeBytes);

                keyboardByteLeng[0] = (byte) keyboardBytes.length;
                ctx.put(keyboardByteLeng);

                ctx.put(keyboardBytes);

                ctx.flip();
                try {
                    socketChannel.write(ctx);
                } catch (Exception err) {
                    out.println("[按鍵傳輸失敗]");
                } finally {
                    ctx.clear();
                }
            }

            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                this.keyHandler(evt, (byte) 1);
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                this.keyHandler(evt, (byte) 2);
            }
        };
        keyboard.setInputListener();
    }

    public void handle(ByteBuffer byteBuffer) {
        if (this.robot == null) {
            out.println("[未被允許操控之傳輸]");
            return;
        }
        byte keyboardType = byteBuffer.get();
        byte keyboardByteLeng = byteBuffer.get();
        byte[] remoteKeyStr = new byte[keyboardByteLeng];

        byteBuffer.get(remoteKeyStr, 0, keyboardByteLeng);
        int keyCode = Integer.parseInt(new String(remoteKeyStr));
        // out.println("[keyCode] " + keyCode);

        if (keyCode == 0) {
            return;
        }
        switch (keyboardType) {
            case 1:
                this.robot.keyPress(keyCode);
                out.println("keyPress: " + keyCode);
                break;

            case 2:
                this.robot.keyRelease(keyCode);
                out.println("keyRelease: " + keyCode);
                break;
        }
    }
}