import com.google.protobuf.Message;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;

public class Model {
    public int ox;
    public int oy;
    public int[] field;
    public int food;
    public static DatagramSocket socket;
    public static ConcurrentHashMap<String, State> states = new ConcurrentHashMap<String, State>();

    public static void Init(){
        try {
            socket = new DatagramSocket();
            parse("config.txt");
            GUI.Init(); //set params in init
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }


    public static void StartNew(){
        Snake MySnake = new Snake();
        placeSnake(MySnake);


        GUI.repaint();
    }


    public static void parse(String filename){ //parse config file to get params

    }
    public static void placeSnake(Snake s){

    }
}
