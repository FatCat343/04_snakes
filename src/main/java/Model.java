import com.google.protobuf.Message;

import java.util.concurrent.ConcurrentHashMap;

public class Model {
    public int ox;
    public int oy;
    public int[] field;
    public int food;
    public static ConcurrentHashMap<String, State> states = new ConcurrentHashMap<String, State>();

    public static void Init(){
        parse("config.txt");
        GUI.Init(); //set params in init
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
