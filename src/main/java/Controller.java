import me.ippolitov.fit.snakes.SnakesProto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Controller {
    public static List<SnakesProto.GamePlayer> players = new ArrayList<>(); //list of ALL clients
    public static int playerId = 0;
    public static int masterId = 1;
    public static SnakesProto.NodeRole role = null;
    //TODO: some fields are in game config
    public static int ping_delay_ms = 100;
    public static int node_timeout_ms = 800;
    //public static SnakesProto.GameConfig config;
    //public static ConcurrentHashMap<String, State> states = new ConcurrentHashMap<String, State>();
    public static void main(String[] args) {
        parse("config.txt");
        //Model.Init(); // инициализирует необходимые структуры
        Model.Init();
        //Model.StartNew(); //запускает одиночную игру
        NetworkReader.start();
        GameListSender.start();
    }
    public static void setState(SnakesProto.GameMessage gm, Sender sender){
        //changes state of ours snake
        Model.config = gm.getState().getState().getConfig();
        //TODO: make change synchronized
        players = gm.getState().getState().getPlayers().getPlayersList();
        Model.setState(gm.getState().getState());
        Model.sendAck(gm, getId(sender));
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
        Model.error(gm);
    }
    public static void ack(SnakesProto.GameMessage gm, Sender sender){
        Model.getAck(gm, getPlayer(sender));
    }
    public static void join(SnakesProto.GameMessage gm, Sender sender){
        Model.join(gm, sender);
        Model.sendAck(gm, getId(sender));
    }
    public static void roleChange(SnakesProto.GameMessage gm, Sender sender){
        if (gm.getRoleChange().hasSenderRole()){
            if (gm.getRoleChange().getSenderRole().equals(SnakesProto.NodeRole.VIEWER)){
                if (gm.getRoleChange().hasReceiverRole()){
                    if (gm.getRoleChange().getReceiverRole().equals(SnakesProto.NodeRole.MASTER)){
                        becomeMaster();
                    }
                }
                else {
                    Model.deletePlayer(gm.getSenderId());
                }
            }
            if (gm.getRoleChange().getSenderRole().equals(SnakesProto.NodeRole.MASTER)){
                if (gm.getRoleChange().hasReceiverRole()){
                    if (gm.getRoleChange().getReceiverRole().equals(SnakesProto.NodeRole.DEPUTY)){
                        findDeputy();
                    }
                    if (gm.getRoleChange().getReceiverRole().equals(SnakesProto.NodeRole.VIEWER)){
                        Model.dead();
                    }
                }
                else {
                    masterId = gm.getSenderId();
                }
            }
        }
        else {
            if (gm.getRoleChange().hasReceiverRole()){
                if (gm.getRoleChange().getReceiverRole().equals(SnakesProto.NodeRole.DEPUTY)){
                    //become deputy
                }
                if (gm.getRoleChange().getReceiverRole().equals(SnakesProto.NodeRole.VIEWER)){
                    Model.dead();
                }
            }

        }

    }
    public static void pingAnswer(SnakesProto.GameMessage gm, Sender sender){
        Model.sendAck(gm, getId(sender));
    }
    public static void exit() {
        Model.exit();
    }
    public static void newgame(){
        Model.StartNew();
    }
    public static SnakesProto.NodeRole getRole(int searchId){
        //TODO: sync iteration
        Iterator<SnakesProto.GamePlayer> iter = players.iterator();
        while (iter.hasNext()) {
            if (iter.next().getId() == searchId) {
                return iter.next().getRole();
            }
        }
        return null;
    }
    public static int getId(Sender sender){
        //TODO: sync iteration
        Iterator<SnakesProto.GamePlayer> iter = players.iterator();
        while (iter.hasNext()) {
            if ((iter.next().getIpAddress().equals(sender.ip)) && (iter.next().getPort() == sender.port)) {
                return iter.next().getId();
            }
        }
        return -1;
    }
    public static SnakesProto.GamePlayer getPlayer(Sender sender){
        //TODO: make synchronized iteration
        Iterator<SnakesProto.GamePlayer> iter = players.iterator();
        while (iter.hasNext()) {
            if ((iter.next().getIpAddress().equals(sender.ip)) && (iter.next().getPort() == sender.port)) {
                return iter.next();
            }
        }
        return null;
    }
    public static SnakesProto.GamePlayer getPlayer(int searchId){
        //TODO: make synchronized iteration
        Iterator<SnakesProto.GamePlayer> iter = players.iterator();
        while (iter.hasNext()) {
            if (iter.next().getId() == searchId) {
                return iter.next();
            }
        }
        return null;
    }
    public static void changeMaster(){
        //find deputy, change info about master to deputy
        Iterator<SnakesProto.GamePlayer> iter = players.iterator();
        while (iter.hasNext()) {
            if (iter.next().getRole().equals(SnakesProto.NodeRole.DEPUTY)) {
                masterId = iter.next().getId();
            }
        }
    }
    public static void findDeputy(){
        //find new deputy in players map
        Iterator<SnakesProto.GamePlayer> iter = players.iterator();
        while (iter.hasNext()) {
            if (iter.next().getRole().equals(SnakesProto.NodeRole.NORMAL)) {
                Model.setDeputy(iter.next().getId());
                break;
            }
        }
    }
    public static void becomeMaster(){
        //взять управление игрой
        findDeputy();
        //send rolechange msg
    }
}
