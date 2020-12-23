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
    public static Sender masterSender;
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
        NetworkWriter.start();
    }
    public static void setState(SnakesProto.GameMessage gm, Sender sender){
        //changes state of ours snake
        Model.config = gm.getState().getState().getConfig();
        //TODO: make change synchronized
        //players = gm.getState().getState().getPlayers().getPlayersList();
        if (neededsenders.size() > 0) Controller.neededsenders.remove(0);
        Model.setState(gm.getState().getState(), sender);
        Model.sendAck(gm, getId(sender));
    }

    public static void parse(String filename){ //parse config file to get params

    }
    public static void connect(Sender sender){
        exit();
        Model.state = null;
        if (Model.state == null) {
            System.out.println("setted state to null");
        }
        //Model.setState(null);
        SnakesProto.GameMessage.Builder gm = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.JoinMsg.Builder join = SnakesProto.GameMessage.JoinMsg.newBuilder();
        join.setName(Controller.name);
        gm.setJoin(join.build());
        gm.setMsgSeq(Model.getMsgId());
        if (neededsenders.size() >= 1) neededsenders.remove(0);
        if (neededsenders.add(sender)) {
            System.out.println("wait ack from ip = " + sender.ip + ":" + sender.port);
        };
        Model.sendJoin(gm.build(), sender);
    }
    public static void steer(SnakesProto.Direction dir){
        System.out.println("steer " + dir + "role = " + role);
        if (role.equals(MASTER)) {
            System.out.println("steer " + dir);
            Model.steer(dir, playerId);
        }
        else Model.sendSteer(dir);
    }
    public static void steer(SnakesProto.GameMessage gm, Sender sender){
        SnakesProto.Direction dir = gm.getSteer().getDirection();
        System.out.println("steer " + dir);
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
        if (neededsenders.size() == 0) Model.getAck(gm, getPlayer(sender));
        else {
            Sender sender1 = neededsenders.get(0);
            if (sender.equals(sender1)){
                System.out.println("got ack from neededsender");

                Controller.masterSender = sender;
                Controller.playerId = gm.getReceiverId();
                SnakesProto.GamePlayer.Builder p = SnakesProto.GamePlayer.newBuilder();
                p.setId(0);
                p.setName("");
                p.setIpAddress(sender.ip);
                p.setPort(sender.port);
                p.setRole(SnakesProto.NodeRole.MASTER);
                p.setScore(0);

                Model.getAck(gm, p.build());
            }

        }
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
                        System.out.println("become master");
                        becomeMaster();
                    }
                }
                else {
                    System.out.println("make smn viewer");
                    Model.makeViewer(gm.getSenderId());
                }
            }
            if (gm.getRoleChange().getSenderRole().equals(MASTER)){
                if (gm.getRoleChange().hasReceiverRole()){
                    if (gm.getRoleChange().getReceiverRole().equals(SnakesProto.NodeRole.DEPUTY)){
                        //become deputy
                        System.out.println("become deputy");
                        Controller.role = SnakesProto.NodeRole.DEPUTY;
                    }
                    if (gm.getRoleChange().getReceiverRole().equals(SnakesProto.NodeRole.VIEWER)){
                        System.out.println("become viewer");
                        Model.becomeViewer(Controller.playerId);
                    }
                }
                else {
                    System.out.println("new master with id =" + gm.getSenderId());
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
        if (neededsenders.size() == 0) Model.sendAck(gm, getId(sender));
        else {
            Model.sendAck(gm, 0);
        }
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
            SnakesProto.GamePlayer player = iter.next();
            if (player.getId() == searchId) {
                return player.getRole();
            }
        }
        return null;
    }
    public static int getId(Sender sender){
        //TODO: sync iteration
        Iterator<SnakesProto.GamePlayer> iter = Model.state.getPlayers().getPlayersList().iterator();
        while (iter.hasNext()) {
            SnakesProto.GamePlayer player = iter.next();
            if ((player.getIpAddress().equals(sender.ip)) && (player.getPort() == sender.port)) {
                return player.getId();
            }
        }
        return -1;
    }
    public static SnakesProto.GamePlayer getPlayer(Sender sender){
        //TODO: make synchronized iteration
        Iterator<SnakesProto.GamePlayer> iter = Model.state.getPlayers().getPlayersList().iterator();
        while (iter.hasNext()) {
            SnakesProto.GamePlayer player = iter.next();
            if ((player.getIpAddress().equals(sender.ip)) && (player.getPort() == sender.port)) {
                return player;
            }
        }
        return null;
    }
    public static SnakesProto.GamePlayer getPlayer(int searchId){
        //TODO: make synchronized iteration
        Iterator<SnakesProto.GamePlayer> iter = Model.state.getPlayers().getPlayersList().iterator();
        while (iter.hasNext()) {
            SnakesProto.GamePlayer player = iter.next();
            if (player.getId() == searchId) {
                return player;
            }
        }
        return null;
    }
    public static void changeMaster(){
        //find deputy, change info about master to deputy
        Iterator<SnakesProto.GamePlayer> iter = Model.state.getPlayers().getPlayersList().iterator();
        while (iter.hasNext()) {
            SnakesProto.GamePlayer player = iter.next();
            if (player.getRole().equals(SnakesProto.NodeRole.DEPUTY)) {
                masterId = player.getId();
            }
        }
    }
    public static void findDeputy(){
        //find new deputy in players map
        Iterator<SnakesProto.GamePlayer> iter = Model.state.getPlayers().getPlayersList().iterator();
        while (iter.hasNext()) {
            SnakesProto.GamePlayer player = iter.next();
            if (player.getRole().equals(SnakesProto.NodeRole.NORMAL)) {
                Model.setDeputy(player.getId());
                break;
            }
        }
    }
    public static int findRole(SnakesProto.NodeRole role){
        //int id = -1;
        Iterator<SnakesProto.GamePlayer> iter = Model.state.getPlayers().getPlayersList().iterator();
        while (iter.hasNext()) {
            SnakesProto.GamePlayer player = iter.next();
            if (player.getRole().equals(role)) {
                return player.getId();
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
