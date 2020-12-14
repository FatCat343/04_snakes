import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Message;
import me.ippolitov.fit.snakes.SnakesProto;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import static me.ippolitov.fit.snakes.SnakesProto.NodeRole.*;

public class Model {
    public int ox;
    public int oy;
    public int[] field;
    public int food;
    public static DatagramSocket socket;
    //public static ConcurrentHashMap<String, State> states = new ConcurrentHashMap<String, State>();
    public static SnakesProto.GameState state;
    public static SnakesProto.GameConfig config;
    public static int msgNum = 0;
    public static void Init(){
        try {
            socket = new DatagramSocket(Controller.port);
            parse("config.txt");
            GameListReceiver.start();
            ///
            GUI.init(config); //set params in init
            GameProcess.start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    public static void continueGame(){
        GameProcess.continueGame(state);
    }



    public static void StartNew(){
        exit();
        GameProcess.start();
        Controller.playerId = 0;
        Controller.masterId = 0;
        Controller.role = MASTER;
        //adding is in start
        //GameProcess.newPlayer(null, null);


        //GUI.repaint(state, GameListReceiver.table);
    }


    public static void parse(String filename){ //parse config file to get params
        SnakesProto.GameConfig.Builder tmp = SnakesProto.GameConfig.newBuilder();
        //tmp.setHeight(7);
        //tmp.setWidth(7);

        config = tmp.build();
    }
//    public static void placeSnake(SnakesProto.GameState.Snake s){
//
//    }
    public static void exit() {
        //send changeState to viewer
        if (Controller.playerId == Controller.masterId){
            //stop game -->give it to deputy
            GameProcess.running = false;
            //GameProcess.changeState(Controller.playerId, VIEWER);

            if (Controller.findRole(DEPUTY) < 0){
                Controller.findDeputy();
            }
            int dest = Controller.findRole(DEPUTY);
            if (dest < 0){
                //no players left
            }
            else {
                SnakesProto.GameMessage.RoleChangeMsg.Builder msg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder();
                msg.setSenderRole(VIEWER);
                msg.setReceiverRole(MASTER);
                Model.sendRoleChange(msg.build(), Controller.masterId);
            }
        }
        else {
            //exit as player --> send VIEWER rolechange
            if (Controller.role != VIEWER){
                SnakesProto.GameMessage.RoleChangeMsg.Builder msg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder();
                msg.setSenderRole(VIEWER);
                Model.sendRoleChange(msg.build(), Controller.masterId);
            }
        }

    }
    public static int getMsgId(){
        //TODO: sync
        msgNum++;
        return msgNum;
    }
    public static void join(SnakesProto.GameMessage gm, Sender sender){
        GameProcess.newPlayer(gm, sender);

    }
//    public static void deleteSnake(int id){
//        //player needs become viewer
//
//        //GameProcess.aliveSnakes--;
//    }
    public static void error(SnakesProto.GameMessage gm, SnakesProto.GamePlayer player){
        //exit();
        GUI.error(gm.getError().getErrorMessage());
        //TODO: make iteration synchronized
        Iterator<MessageCustom> iter = NetworkWriter.resend.iterator();
        while (iter.hasNext()) {
            if (iter.next().gm.getMsgSeq() == gm.getMsgSeq()) {
                //if ack on join - set playerId
                Controller.playerId = gm.getReceiverId();
                Controller.masterId = gm.getSenderId(); //??
                //delete sender id from branches
                iter.next().branches.remove(player);
            }
        }
    }
    public static void setState(SnakesProto.GameState state1){
        //TODO : sync
        state = state1;
        //update controller.players
        GUI.repaint(state, GameListReceiver.table);
    }
    public static void setDeputy(int deputyId){
        //change deputy id state to deputy
        GameProcess.changeState(deputyId, DEPUTY);
        SnakesProto.GameMessage.RoleChangeMsg.Builder msg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder();
        msg.setSenderRole(MASTER);
        msg.setReceiverRole(DEPUTY);
        sendRoleChange(msg.build(), deputyId);
    }
    public static void sendError(SnakesProto.GameMessage gm, Sender sender, String erMsg){
        SnakesProto.GameMessage.Builder message = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.ErrorMsg.Builder error = SnakesProto.GameMessage.ErrorMsg.newBuilder();
        error.setErrorMessage(erMsg);
        message.setError(error);
        message.setMsgSeq(gm.getMsgSeq());

        SnakesProto.GamePlayer.Builder p = SnakesProto.GamePlayer.newBuilder();
        p.setId(0);
        p.setName("");
        p.setIpAddress(sender.ip);
        p.setPort(sender.port);
        p.setRole(SnakesProto.NodeRole.NORMAL);
        p.setScore(0);
        NetworkWriter.sendError(message.build(), p.build());
    }
    public static void sendJoin(SnakesProto.GameMessage gm, Sender sender){
        SnakesProto.GameMessage.Builder message = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.ErrorMsg.Builder error = SnakesProto.GameMessage.ErrorMsg.newBuilder();
        message.setError(error);
        message.setMsgSeq(getMsgId());

        SnakesProto.GamePlayer.Builder p = SnakesProto.GamePlayer.newBuilder();
        p.setId(0);
        p.setName("");
        p.setIpAddress(sender.ip);
        p.setPort(sender.port);
        p.setRole(SnakesProto.NodeRole.NORMAL);
        p.setScore(0);
        NetworkWriter.sendError(message.build(), p.build());
    }
    public static void sendSteer(SnakesProto.Direction dir){
        SnakesProto.GameMessage.Builder gm = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.SteerMsg.Builder steer = SnakesProto.GameMessage.SteerMsg.newBuilder();
        steer.setDirection(dir);
        gm.setSteer(steer.build());
        gm.setReceiverId(Controller.masterId);
        gm.setMsgSeq(getMsgId());
        NetworkWriter.queue.add(gm.build());
        System.out.println(dir);
    }
    public static void sendState(SnakesProto.GameState state){
        SnakesProto.GameMessage.Builder gm = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.StateMsg.Builder gm1 = SnakesProto.GameMessage.StateMsg.newBuilder();
        gm1.setState(state);
        gm.setState(gm1.build());
        gm.setMsgSeq(getMsgId());
        NetworkWriter.queue.add(gm.build());
    }
    public static void showState(SnakesProto.GameState state){
        GUI.repaint(state, GameListReceiver.table);
    }
    public static void steer(SnakesProto.Direction dir, int playerId){
        GameProcess.setSteer(dir, playerId);
    }
    public static void sendAck(SnakesProto.GameMessage message, int receiverId){
        SnakesProto.GameMessage.Builder gm = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.AckMsg.Builder ack = SnakesProto.GameMessage.AckMsg.newBuilder();
        gm.setAck(ack.build());
        gm.setReceiverId(receiverId);
        gm.setMsgSeq(message.getMsgSeq());
        NetworkWriter.queue.add(gm.build());
    }
    public static void sendRoleChange(SnakesProto.GameMessage.RoleChangeMsg msg, int id){
        if (id == -1){
            //send all from master
            SnakesProto.GameMessage.Builder gm = SnakesProto.GameMessage.newBuilder();
            gm.setRoleChange(msg);
            gm.setSenderId(Controller.playerId);
            gm.setMsgSeq(getMsgId());
            NetworkWriter.queue.add(gm.build());
        }
        else {
            SnakesProto.GameMessage.Builder gm = SnakesProto.GameMessage.newBuilder();
            gm.setRoleChange(msg);
            gm.setMsgSeq(getMsgId());
            gm.setSenderId(Controller.playerId);
            gm.setReceiverId(id);
            NetworkWriter.queue.add(gm.build());
        }
    }
    public static void getAck(SnakesProto.GameMessage gm, SnakesProto.GamePlayer player){
        //TODO: make iteration synchronized
        Iterator<MessageCustom> iter = NetworkWriter.resend.iterator();
        while (iter.hasNext()) {
            if (iter.next().gm.getMsgSeq() == gm.getMsgSeq()) {
                //if ack on join - set playerId
                Controller.playerId = gm.getReceiverId();
                Controller.masterId = gm.getSenderId(); //??
                //delete sender id from branches
                iter.next().branches.remove(player);
            }
        }
    }
    public static void makeViewer(int id){
        //TODO: sync
        //player needs become viewer
        if (id != Controller.playerId){
            //another player become Viewer -> change his role (mb set snake to !ALIVE)
            GameProcess.changeState(id, VIEWER);
            GameProcess.makeZombie(id);
            SnakesProto.GameMessage.RoleChangeMsg.Builder msg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder();
            msg.setReceiverRole(VIEWER);
            msg.setSenderRole(MASTER);
            sendRoleChange(msg.build(), id);
        }
        else {
            //we become viewer
            //Controller.role = SnakesProto.NodeRole.VIEWER;
            //GameProcess.changeState(id, VIEWER);
            //GameProcess.makeZombie(id);
        }
        //GameProcess.aliveSnakes--;

    }
    public static void becomeViewer(int playerId){
        //?
        Controller.role = VIEWER;
    }
//    public static void dead(){
//
//    }
    public static void disconnect(int id) {
        if (Controller.role.equals(MASTER)) {
            //find snake, set to !ALIVE, set new playerid (<0)
            if (Controller.getPlayer(id).getRole() != VIEWER) GameProcess.makeZombie(id);
            //delete from players
            GameProcess.deletePlayer(id);
        }
        else {
            GUI.error("connection lost");
        }
    }
}
