import me.ippolitov.fit.snakes.SnakesProto;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Controller {
    public static ConcurrentHashMap<Integer, SnakesProto.GamePlayer> players = new ConcurrentHashMap<>(); //list of clients
    //public static ConcurrentHashMap<String, State> states = new ConcurrentHashMap<String, State>();
    public static void main(String[] args) {
        parse("config.txt");
        //Model.Init(); // инициализирует необходимые структуры
        Model.Init();
        //Model.StartNew(); //запускает одиночную игру
        NetworkReader.start();
        GameListSender.start();
    }
    public static void setState(SnakesProto.GameMessage gm){
        //changes state of ours snake
    }



    public static void parse(String filename){ //parse config file to get params

    }
    public static void connect(){

    }
    public static void steer(String dir){
        System.out.println(dir);
    }
    public static void steer(SnakesProto.GameMessage gm){

    }
    public static void error(SnakesProto.GameMessage gm){

    }
    public static void ack(SnakesProto.GameMessage gm){

    }
    public static void join(SnakesProto.GameMessage gm){

    }
    public static void roleChange(SnakesProto.GameMessage gm){

    }
    public static void pingAnswer(SnakesProto.GameMessage gm){

    }
    public static void exit() {
        Model.exit();
    }
    public static void newgame(){
        Model.StartNew();
    }
}
