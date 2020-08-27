import java.util.regex.Matcher;
import java.util.regex.Pattern;

import server.Center;
import client.Chat;
import packager.Csocket;
import packager.CsocketServer;
import packager.CsocketClient;

import static java.lang.System.out;

public class Online {
    private String type;
    private int port;
    private String address;

    public Online() {
        this.type = "client";
        this.port = 3000;
        this.address = "127.0.0.1";
    }

    public void setPort(String str) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(str);
        if (matcher.matches()) {
            this.port = Integer.parseInt(str);
        }
    }

    public void setAddress(String str) {
        Pattern pattern = Pattern.compile("(\\w+\\.)+\\w+$");
        Matcher matcher = pattern.matcher(str);
        if (matcher.matches()) {
            this.address = str;
        }
    }

    public void run() {
        Csocket<?> process;
        if (this.type.equals("server")) {
            CsocketServer center = new Center();
            center.createConnection(this.port);
            process = center;
        } else {
            CsocketClient chat = new Chat();
            chat.createConnection(this.address, this.port);
            process = chat;
        }
        
        out.println(this.type);
        out.println(this.address);
        out.println(this.port);

        process.setMainHandler();
    }

    public static void main(String[] args) {
        Online online = new Online();

        if (args.length > 0) {

            switch (args[0]) {
                case "-s":
                case "--server":
                    online.type = "server";

                    if (args.length > 1) {
                        online.setPort(args[1]);
                        online.setAddress(args[1]);
                    }
                    if (args.length > 2) {
                        online.setPort(args[2]);
                        online.setAddress(args[2]);
                    }
                    break;

                case "-c":
                case "--client":
                    online.type = "client";

                    if (args.length > 1) {
                        online.setPort(args[1]);
                        online.setAddress(args[1]);
                    }
                    if (args.length > 2) {
                        online.setPort(args[2]);
                        online.setAddress(args[2]);
                    }
                    break;

                default:
                    online.type = "client";

                    online.setPort(args[0]);
                    online.setAddress(args[0]);
                    if (args.length > 1) {
                        online.setPort(args[1]);
                        online.setAddress(args[1]);
                    }
                    break;
            }
        }

        online.run();
    }
}