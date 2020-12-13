import me.ippolitov.fit.snakes.SnakesProto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static me.ippolitov.fit.snakes.SnakesProto.NodeRole.MASTER;
import static me.ippolitov.fit.snakes.SnakesProto.NodeRole.VIEWER;

public class Controller {
    //public static List<SnakesProto.GamePlayer> players = new ArrayList<>(); //list of ALL clients
    public static int playerId = 0;
    public static int masterId = 1;
    public static String name;
    public static int port;
    public static SnakesProto.NodeRole role = VIEWER;
    public static List<Sender> neededsenders = new ArrayList<>();
    public static void main(String[] args) {
        parse("config.txt");
        name = args[0];
        port = Integer.parseInt(args[1]);
        //Model.Init(); // инициализирует необходимые структуры
        Model.Init();
        //Model.StartNew(); //запускает одиночную игру
        NetworkReader.start();
    }
    public static void setState(SnakesProto.GameMessage gm, Sender sender){
        //changes state of ours snake
        Model.config = gm.getState().getState().getConfig();
        //TODO: make change synchronized
        //players = gm.getState().getState().getPlayers().getPlayersList();
        Model.setState(gm.getState().getState());
        Model.sendAck(gm, getId(sender));
    }

    public static void parse(String filename){ //parse config file to get params

    }
    public static void connect(Sender sender){
        exit();
        Model.setState(null);
        SnakesProto.GameMessage.Builder gm = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.JoinMsg.Builder join = SnakesProto.GameMessage.JoinMsg.newBuilder();
        join.setName(Controller.name);
        gm.setJoin(join.build());
        gm.setMsgSeq(Model.getMsgId());
        neededsenders.remove(0);
        neededsenders.add(sender);
        Model.sendJoin(gm.build(), sender);
    }
    public static void steer(SnakesProto.Direction dir){
        System.out.println(role);
        if (role.equals(MASTER)) {
            System.out.println("steer " + dir);
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
    public static void error(SnakesProto.GameMessage gm, Sender sender){
        Model.error(gm, getPlayer(sender));
        Model.sendAck(gm, getId(sender));
    }
    public static void ack(SnakesProto.GameMessage gm, Sender sender){
        Model.getAck(gm, getPlayer(sender));
    }
    public static void join(SnakesProto.GameMessage gm, Sender sender){
        Model.join(gm, sender);
        //Model.sendAck(gm, getId(sender));
    }
    public static void roleChange(SnakesProto.GameMessage gm, Sender sender){
        if (gm.getRoleChange().hasSenderRole()){
            if (gm.getRoleChange().getSenderRole().equals(SnakesProto.NodeRole.VIEWER)){
                if (gm.getRoleChange().hasReceiverRole()){
                    if (gm.getRoleChange().getReceiverRole().equals(MASTER)){
                        becomeMaster();
                    }
                }
                else {
                    Model.makeViewer(gm.getSenderId());
                }
            }
            if (gm.getRoleChange().getSenderRole().equals(MASTER)){
                if (gm.getRoleChange().hasReceiverRole()){
                    if (gm.getRoleChange().getReceiverRole().equals(SnakesProto.NodeRole.DEPUTY)){
                        //become deputy
                        Controller.role = SnakesProto.NodeRole.DEPUTY;
                    }
                    if (gm.getRoleChange().getReceiverRole().equals(SnakesProto.NodeRole.VIEWER)){
                        Model.becomeViewer(Controller.playerId);
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
                    Controller.role = SnakesProto.NodeRole.DEPUTY;
                }
                if (gm.getRoleChange().getReceiverRole().equals(SnakesProto.NodeRole.VIEWER)){
                    Model.becomeViewer(Controller.playerId);
                }
            }

        }
        Model.sendAck(gm, getId(sender));

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
        Iterator<SnakesProto.GamePlayer> iter = Model.state.getPlayers().getPlayersList().iterator();
        while (iter.hasNext()) {
            if (iter.next().getId() == searchId) {
                return iter.next().getRole();
            }
        }
        return null;
    }
    public static int getId(Sender sender){
        //TODO: sync iteration
        Iterator<SnakesProto.GamePlayer> iter = Model.state.getPlayers().getPlayersList().iterator();
        while (iter.hasNext()) {
            if ((iter.next().getIpAddress().equals(sender.ip)) && (iter.next().getPort() == sender.port)) {
                return iter.next().getId();
            }
        }
        return -1;
    }
    public static SnakesProto.GamePlayer getPlayer(Sender sender){
        //TODO: make synchronized iteration
        Iterator<SnakesProto.GamePlayer> iter = Model.state.getPlayers().getPlayersList().iterator();
        while (iter.hasNext()) {
            if ((iter.next().getIpAddress().equals(sender.ip)) && (iter.next().getPort() == sender.port)) {
                return iter.next();
            }
        }
        return null;
    }
    public static SnakesProto.GamePlayer getPlayer(int searchId){
        //TODO: make synchronized iteration
        Iterator<SnakesProto.GamePlayer> iter = Model.state.getPlayers().getPlayersList().iterator();
        while (iter.hasNext()) {
            if (iter.next().getId() == searchId) {
                return iter.next();
            }
        }
        return null;
    }
    public static void changeMaster(){
        //find deputy, change info about master to deputy
        Iterator<SnakesProto.GamePlayer> iter = Model.state.getPlayers().getPlayersList().iterator();
        while (iter.hasNext()) {
            if (iter.next().getRole().equals(SnakesProto.NodeRole.DEPUTY)) {
                masterId = iter.next().getId();
            }
        }
    }
    public static void findDeputy(){
        //find new deputy in players map
        Iterator<SnakesProto.GamePlayer> iter = Model.state.getPlayers().getPlayersList().iterator();
        while (iter.hasNext()) {
            if (iter.next().getRole().equals(SnakesProto.NodeRole.NORMAL)) {
                Model.setDeputy(iter.next().getId());
                break;
            }
        }
    }
    public static int findRole(SnakesProto.NodeRole role){
        //int id = -1;
        Iterator<SnakesProto.GamePlayer> iter = Model.state.getPlayers().getPlayersList().iterator();
        while (iter.hasNext()) {
            if (iter.next().getRole().equals(role)) {
                return iter.next().getId();
            }
        }
        return -1;
    }
    public static void becomeMaster(){
        //взять управление игрой
        Model.continueGame();
        findDeputy();
        //send rolechange msg
        SnakesProto.GameMessage.RoleChangeMsg.Builder msg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder();
        msg.setSenderRole(MASTER);
        Model.sendRoleChange(msg.build(), -1);
    }
}
