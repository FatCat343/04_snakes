import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Message;
import me.ippolitov.fit.snakes.SnakesProto;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class Model {
    public int ox;
    public int oy;
    public int[] field;
    public int food;
    public static DatagramSocket socket;
    //public static ConcurrentHashMap<String, State> states = new ConcurrentHashMap<String, State>();
    public static SnakesProto.GameState state;
    public static SnakesProto.GameConfig config;
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



        GUI.repaint(state);
    }


    public static void parse(String filename){ //parse config file to get params

    }
    public static void placeSnake(SnakesProto.GameState.Snake s){

    }
    public static void exit() {

    }
    public static int getMsgId(){
        int id = 0;

        return id;
    }
    public static void join(SnakesProto.GameMessage gm, Sender sender){
        GameProcess.newPlayer(gm, sender);

    }
    public static void error(SnakesProto.GameMessage gm){
        GUI.error(gm.getError().getErrorMessage());
    }
    public static void setState(SnakesProto.GameState state1){
        state = state1;
        //update controller.players
        GUI.repaint(state);
    }
    public static void setDeputy(int deputyId){

    }
    public static void sendError(SnakesProto.GameMessage gm, Sender sender, String erMsg){
        SnakesProto.GameMessage.Builder message = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.ErrorMsg.Builder error = SnakesProto.GameMessage.ErrorMsg.newBuilder();
        error.setErrorMessage(erMsg);
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
        GUI.repaint(state);
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
    public static void getAck(SnakesProto.GameMessage gm, SnakesProto.GamePlayer player){
        //TODO: make iteration synchronized
        Iterator<MessageCustom> iter = NetworkWriter.resend.iterator();
        while (iter.hasNext()) {
            if (iter.next().gm.getMsgSeq() == gm.getMsgSeq()) {
                //delete sender id from branches
                iter.next().branches.remove(player);
            }
        }
    }
    public static void deletePlayer(int id){
        //TODO: sync
        GameProcess.aliveSnakes--;
    }
    public static void dead(){

    }
}
