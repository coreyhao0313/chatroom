import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Online {
    private String type;
    private int port;
    private String address;

    public Online(){
        this.type = "client";
        this.port = 3000;
        this.address = "127.0.0.1";
    }

    public void setPort(String str){
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(str);
        if(matcher.matches()){
            this.port = Integer.parseInt(str);
        }
    }

    public void setAddress(String str){
        Pattern pattern = Pattern.compile("(\\w+\\.)+\\w+$");
        Matcher matcher = pattern.matcher(str);
        if(matcher.matches()){
            this.address = str;
        }
    }

    public void getInfo(){
        System.out.println(this.type);
        System.out.println(this.address);
        System.out.println(this.port);
    }

    public void getRunning(){
        if(this.type.equals("server")){
            server.Center center = new server.Center(this.port);
        } else {
            client.Chat chat = new client.Chat(this.address, this.port);
        }

    }
    public static void main(String[] args){
        Online online = new Online();

        if(args.length > 0){
            
            switch (args[0]){
                case "-s":
                case "--server":
                online.type = "server";

                if(args.length > 1){
                    online.setPort(args[1]);
                    online.setAddress(args[1]);
                }
                if(args.length > 2){
                    online.setPort(args[2]);
                    online.setAddress(args[2]);
                }
                break;

                case "-c":
                case "--client":
                online.type = "client";

                if(args.length > 1){
                    online.setPort(args[1]);
                    online.setAddress(args[1]);
                }
                if(args.length > 2){
                    online.setPort(args[2]);
                    online.setAddress(args[2]);
                }
                break;

                default:
                online.type = "client";

                online.setPort(args[0]);
                online.setAddress(args[0]);
                if(args.length > 1){
                    online.setPort(args[1]);
                    online.setAddress(args[1]);
                }
                break;
            }
        }

        online.getInfo();
        online.getRunning();
    }
}