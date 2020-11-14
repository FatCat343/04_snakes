import java.util.concurrent.ConcurrentHashMap;

public class Controller {

    //public static ConcurrentHashMap<String, State> states = new ConcurrentHashMap<String, State>();
    public static void main(String[] args) {
        parse("config.txt");
        Model.Init(); // инициализирует необходимые структуры
        Model.StartNew(); //запускает одиночную игру
        NetworkReader.start();
        GameListSender.start();
    }
    public static void setState(){
        //changes state of ours snake
    }



    public static void parse(String filename){ //parse config file to get params

    }
    public static void connect(){

    }
    public static void steer(String dir){
        System.out.println(dir);
    }
    public static void exit() {

    }
    public static void newgame(){

    }
}
