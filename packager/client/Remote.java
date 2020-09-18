package packager.client;

import java.nio.channels.SocketChannel;
import java.awt.Robot;

import static java.lang.System.out;

import base.State;
import base.packager.ParserEvent;
import client.Keyboard;
import packager.Parser;
import packager.Packager;

public class Remote implements ParserEvent {
    public Robot robot;
    public Integer keyboardCode;
    public byte keyboardState;

    public void setOutputController() {
        try {
            this.robot = new Robot();
            out.println("[允許作為受控端]");
        } catch (Exception Err) {
            Err.printStackTrace();
        }
    }

    public void setKeyboardSender(SocketChannel socketChannel) {
        Packager sPkg = new Packager(1024);
        sPkg.bind(State.REMOTE, 2);

        Keyboard keyboard = new Keyboard() {
            public byte[] keyboardStateBytes = new byte[1];

            public void keyHandler(java.awt.event.KeyEvent evt, byte keyboardState) {
                int keyboardInt = evt.getKeyCode();
                evt.consume();
                byte[] keyboardStrBytes = String.valueOf(keyboardInt).getBytes();
                keyboardStateBytes[0] = keyboardState;
                this.text.setText("");

                try {
                    sPkg.write(keyboardStrBytes);
                    sPkg.breakPoint();
                    sPkg.write(keyboardStateBytes);
                    sPkg.sendTo(socketChannel);
                    sPkg.proceed();
                } catch (Exception err) {
                    err.printStackTrace();
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

    public void handle(Parser pkg, SocketChannel socketChannel) {
        if (this.robot == null) {
            return;
        }
        if (pkg.parserEvent == null) {
            pkg.setProceeding(true);

            Remote receiver = this;
            receiver.resetRemote();
            pkg.fetch(socketChannel, receiver);
        } else {
            pkg.fetch(socketChannel);
        }
    }

    @Override
    public void get(Parser self) {
        ;
    }

    @Override
    public void breakPoint(Parser self) {
        if (this.keyboardCode == null) {
            byte[] stuffBytes = self.getBytes();
            String stuffString = new String(stuffBytes);
            this.keyboardCode = Integer.parseInt(stuffString);
        } else if (this.keyboardState == 0) {
            this.keyboardState = self.ctx.get();
        }
    }

    @Override
    public void finish(Parser self) {
        if (this.keyboardCode == 0 || this.keyboardCode == null) {
            return;
        }
        try {
            switch (this.keyboardState) {
                case 1:
                    this.robot.keyPress(this.keyboardCode);
                    out.println("keyPress: " + this.keyboardCode);
                    break;

                case 2:
                    this.robot.keyRelease(this.keyboardCode);
                    out.println("keyRelease: " + this.keyboardCode);
                    break;
            }
        } catch (Exception err) {
            out.println("不支援的 Keycode");
        }
    }

    public void resetRemote() {
        this.keyboardCode = null;
        this.keyboardState = 0;
    }
}