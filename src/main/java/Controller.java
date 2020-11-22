import me.ippolitov.fit.snakes.SnakesProto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Controller {
    public static ConcurrentHashMap<Integer, SnakesProto.GamePlayer> players = new ConcurrentHashMap<>(); //list of ALL clients
    public static int playerId = 0;
    public static int masterId = 1;
    public static SnakesProto.NodeRole role = null;
    public static int ping_delay_ms = 100;
    public static int node_timeout_ms = 800;
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
    public static void steer(SnakesProto.Direction dir){
        System.out.println(dir);
        if (role.equals(SnakesProto.NodeRole.MASTER)) {
            Model.steer(dir, playerId);
        }
        else Model.sendSteer(dir);
    }
    public static void steer(SnakesProto.GameMessage gm, Sender sender){
        SnakesProto.Direction dir = gm.getSteer().getDirection();
        int id = getId(sender);
        if (id != -1) {
            Model.steer(dir, id);
            Model.sendAck(gm, id);
        }
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
    public static SnakesProto.NodeRole getRole(int searchId){
        //TODO: sync iteration
        for (Map.Entry<Integer, SnakesProto.GamePlayer> pair : Controller.players.entrySet()) {
            SnakesProto.GamePlayer player = pair.getValue();
            if (player.getId() == searchId) return player.getRole();
        }
        return null;
    }
    public static int getId(Sender sender){
        //TODO: sync iteration
        for (Map.Entry<Integer, SnakesProto.GamePlayer> pair : Controller.players.entrySet()) {
            SnakesProto.GamePlayer player = pair.getValue();
            if ((player.getIpAddress().equals(sender.ip)) && (player.getPort() == sender.port)) return pair.getKey();
        }
        return -1;
    }
    public static void changeMaster(){
        //find deputy, change info about master to deputy
    }
    public static void findDeputy(){
        //find new deputy in players map
    }
    public static void becomeMaster(){
        //взять управление игрой
        findDeputy();
        //send rolechange msg
    }
}
