import me.ippolitov.fit.snakes.SnakesProto;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

import static java.time.temporal.ChronoUnit.SECONDS;

public class NetworkWriter implements Runnable {
    //waits on message queue or invokes to resend message
    //message -> if message send to all others except host (first check that it is not in the list) -> add to list with send time && origtime = sendtime
    //if reply -> send to destination host
    //compare time.now with InvokeTime -> sleep for diff millisec
    //invokes by time -> removes list[0] -> send list[0] && invoke time = 0 -> ads list[0] to the end of list with new send time
    //invoketime = =0 -> invoketime = list[0].time + 3000ms
    //private static ConcurrentLinkedQueue<MStruct> queue;
    public static LinkedBlockingDeque<Message> resend = new LinkedBlockingDeque<>();
    public static BlockingQueue<SnakesProto.GameMessage> queue = new LinkedBlockingQueue<>() ; //queue on output
    public static ConcurrentHashMap<SnakesProto.GamePlayer, LocalTime> lastSent = new ConcurrentHashMap<>();
    public LocalTime invoketime = null;
    public LocalTime checktime = null;
    public void run(){
        while (true) {
            SnakesProto.GameMessage message;
            try {
                message = queue.poll(Controller.ping_delay_ms, TimeUnit.MILLISECONDS);
                if (message != null) {
                    switch (message.getTypeCase()) {
                        case PING:{
                            send_single(message);
                        }
                        case STEER:{
                            send_single(message);
                        }
                        case ACK:{
                            send_ack(message);
                        }
                        case STATE:{
                            send_all(message);
                        }
                        case ANNOUNCEMENT:{
                            System.out.println("ERROR, wrong type of message");
                        }
                        case JOIN:{
                            send_single(message);
                        }
                        case ERROR:{
                            send_single(message);
                        }
                        case ROLE_CHANGE:{ //always have sender_id and receiver_id
                            if (message.getRoleChange().hasSenderRole() && message.getRoleChange().getSenderRole() == SnakesProto.NodeRole.MASTER) {
                                if (!message.getRoleChange().hasReceiverRole()){
                                    send_all(message);
                                }
                                else {
                                    send_single(message);
                                }
                            }
                            else {
                                send_single(message);
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("polling was interrupted");
                e.printStackTrace();
            }
            //TODO : changed seconds.between() on correct func ->isBefore, check
            while ((invoketime != null) && invoketime.isBefore(LocalTime.now())) {
                //resends oldest message
                Message msg = resend.poll(); //was id
                if (msg != null){
                    //TODO: change seconds.between - check
                    //timeout for message
                    if (msg.origtime.plusNanos(Controller.node_timeout_ms * 1000000).isBefore(LocalTime.now())) {
                        deleteclients(msg.branches);
                        //copied from end of while not to miss this part
                        Message next = resend.peek();
                        if (next != null) {
                            invoketime = next.sendtime.plusNanos(Controller.ping_delay_ms * 1000000);
                        }
                        else invoketime = null;
                        continue;
                    }
                    if (msg.branches.size() > 0) {
                        //TODO: check on importance of synchronization
                        Iterator<Map.Entry<Integer, SnakesProto.GamePlayer>> it = msg.branches.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry<Integer, SnakesProto.GamePlayer> pair = it.next();
                            if (Controller.players.contains(pair.getValue())) Network.send(msg.gm, Controller.players.get(msg.gm.getReceiverId()));
                        }
                        msg.sendtime = LocalTime.now();
                        resend.add(msg); //add to tail
                    }
                }
                Message next = resend.peek();
                if (next != null) {
                    invoketime = next.sendtime.plusSeconds(3);
                }
                else invoketime = null;
            }
            //check lastsent, if player is not in players - delete it, if localtime < now - 100ms -> send ping
            //TODO: iteration synchronization
            Iterator<Map.Entry<SnakesProto.GamePlayer, LocalTime>> it = lastSent.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<SnakesProto.GamePlayer, LocalTime> pair = it.next();
                if (!Controller.players.contains(pair.getKey())) {
                    it.remove();
                    continue;
                }
                if (pair.getValue().plusNanos(Controller.ping_delay_ms * 1000000).isBefore(LocalTime.now())){
                    send_ping(pair.getKey());
                }
            }
        }
    }
    private void send_ping(SnakesProto.GamePlayer player){
        SnakesProto.GameMessage.Builder gm = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.PingMsg.Builder ping = SnakesProto.GameMessage.PingMsg.newBuilder();
        gm.setPing(ping.build());
        gm.setReceiverId(player.getId());
        gm.setMsgSeq(Model.getMsgId());
        send_single(gm.build());
    }
    public static void Start(){
        Thread t = new Thread(new NetworkWriter());
        t.start();
    }
    private void send_all(SnakesProto.GameMessage message){
        Message mst = new Message();
        mst.gm = message;
        //TODO : check if conroller.players changes between init of branches and iteration(client leaves or comes)
        mst.branches = new ConcurrentHashMap<>(Controller.players);
        //TODO: mb sync branches iteration
        Iterator<Map.Entry<Integer, SnakesProto.GamePlayer>> it = mst.branches.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, SnakesProto.GamePlayer> pair = it.next();
            SnakesProto.GamePlayer pl = pair.getValue();
            if (pl.getId() == Controller.playerId) it.remove(); //our gameplayer, not resend to ourself
        }

        mst.origtime = LocalTime.now();
        mst.sendtime = mst.origtime;
        //all messages in resend of message.type need to be updated
        //TODO: make iteration synchronized
        Iterator<Message> iter = resend.iterator();
        while (iter.hasNext()) {
            if (iter.next().gm.getTypeCase() == message.getTypeCase()) resend.remove(iter.next());
        }
        boolean res = resend.add(mst);
        if (invoketime == null) {
            invoketime = mst.origtime.plusNanos(Controller.ping_delay_ms * 1000000);
        }
        //TODO: make iteration synchronized
        for (Map.Entry<Integer, SnakesProto.GamePlayer> pair : Controller.players.entrySet()) {
            SnakesProto.GamePlayer player = pair.getValue();
            Network.send(message, player);
        }
    }
    private void send_single(SnakesProto.GameMessage message){
        Message mst = new Message();
        mst.gm = message;
        mst.branches = new ConcurrentHashMap<>();
        mst.branches.put(message.getReceiverId(), Controller.players.get(message.getReceiverId()));
        mst.origtime = LocalTime.now();
        mst.sendtime = mst.origtime;
        Network.send(message, Controller.players.get(message.getReceiverId()));
        //check if we have older message of same type to same receiver
        //TODO: make iteration synchronized
        Iterator<Message> iter = resend.iterator();
        while (iter.hasNext()) {
            if ((iter.next().gm.getReceiverId() == message.getReceiverId()) && (iter.next().gm.getTypeCase() == message.getTypeCase())) {
                resend.remove(iter.next());
                break;
            }
        }
        boolean res = resend.add(mst);
        //System.out.println("result of offering = " + res + " tryed to offer " + message.packet.id.toString());
        if (invoketime == null) {
            invoketime = mst.origtime.plusNanos(Controller.ping_delay_ms * 1000000);
        }
    }
    private static void send_ack(SnakesProto.GameMessage gm){
        //same as send_single, but don't add to resend
        //TODO: make iteration synchronized
        Iterator<Map.Entry<Sender, SnakesProto.GameMessage>> it = NetworkReader.received.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Sender, SnakesProto.GameMessage> pair = it.next();
            //checks if in pair sender = receiver in gm and if msg_id's are equal
            if ((gm.getMsgSeq() == pair.getValue().getMsgSeq()) && (Controller.players.get(gm.getReceiverId()).getIpAddress().equals(pair.getKey().ip)) && (Controller.players.get(gm.getReceiverId()).getPort() == pair.getKey().port)) {
                Network.send(gm, Controller.players.get(gm.getReceiverId()));
                break;
            }
        }

    }
    public void deleteclients(ConcurrentHashMap<Integer, SnakesProto.GamePlayer> clients){
        //System.out.println("delete clients called");
        //TODO: check on importance of synchronization
        Iterator<Map.Entry<Integer, SnakesProto.GamePlayer>> it = clients.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, SnakesProto.GamePlayer> pair = it.next();
            Controller.players.remove(pair.getKey(), pair.getValue());
            //we are normal, delete master
            if ((pair.getValue().getRole().equals(SnakesProto.NodeRole.MASTER)) && (Objects.equals(Controller.getRole(Controller.playerId), SnakesProto.NodeRole.NORMAL))){
                Controller.changeMaster();
                continue;
            }
            //we are deputy, delete master
            if ((pair.getValue().getRole().equals(SnakesProto.NodeRole.MASTER)) && (Objects.equals(Controller.getRole(Controller.playerId), SnakesProto.NodeRole.DEPUTY))){
                Controller.becomeMaster();
                continue;
            }
            //we are master, delete deputy
            if ((pair.getValue().getRole().equals(SnakesProto.NodeRole.DEPUTY)) && (Objects.equals(Controller.getRole(Controller.playerId), SnakesProto.NodeRole.MASTER))){
                Controller.findDeputy();
                //continue;
            }
        }
    }
}

