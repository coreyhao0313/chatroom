package client;

import java.awt.*;
import java.awt.event.*;

public class Keyboard extends KeyAdapter {
    public Frame frame;
    public TextArea text;
    public KeyEvent KeyEvent;

    public void setInputListener(){
        this.frame = new Frame("ChatRemote");

        this.frame.setFocusTraversalKeysEnabled(false);

        this.frame.addWindowListener(new Window());
        this.frame.setSize(500, 500);

        this.text = new TextArea();
        this.text.setEditable(false);
        this.text.addKeyListener(this);

        this.frame.add(text);
        this.frame.setVisible(true);
    }

    public void keyPressed(KeyEvent evt) {
        ;
    }

    public void keyReleased(KeyEvent evt) {
        ;
    }
}

class Window extends WindowAdapter {
    public void windowClosing(WindowEvent evt) {
        System.exit(0);
    }
}