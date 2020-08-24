package client;

import java.awt.*;
import java.awt.event.*;

public class Keyboard extends KeyAdapter {
    
    public Frame frame;
    public TextArea text;
    public KeyEvent KeyEvent;
    public Robot robot;

    public void addOutputController(){
        try {
            robot = new Robot();
        } catch(Exception Err){
            Err.printStackTrace();
        }
    }

    public void addInputListener(){
        this.frame = new Frame("ChatRemote");

        this.frame.setFocusTraversalKeysEnabled(false);

        this.frame.addWindowListener(new Remote());
        this.frame.setSize(500, 500);

        this.text = new TextArea();
        this.text.setEditable(false);
        this.text.addKeyListener(this);

        frame.add(text);
        frame.setVisible(true);
    }

    public void keyPressed(KeyEvent evt) {
        ;
    }

    public void keyReleased(KeyEvent evt) {
        ;
    }
}

class Remote extends WindowAdapter {
    public void windowClosing(WindowEvent evt) {
        System.exit(0);
    }
}