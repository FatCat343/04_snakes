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
    public static LinkedBlockingDeque<MessageCustom> resend = new LinkedBlockingDeque<>();
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
                //TODO: change resend to send old msg to new MASTER
                MessageCustom msg = resend.poll(); //was id
                if (msg != null){
                    //TODO: change seconds.between - check
                    //timeout for message
                    if (msg.origtime.plusNanos(Controller.config.getNodeTimeoutMs() * 1000000).isBefore(LocalTime.now())) {
                        deleteclients(msg.branches);
                        //copied from end of while not to miss this part
                        MessageCustom next = resend.peek();
                        if (next != null) {
                            invoketime = next.sendtime.plusNanos(Controller.config.getPingDelayMs() * 1000000);
                        }
                        else invoketime = null;
                        continue;
                    }
                    if (msg.branches.size() > 0) {
                        //TODO: check on importance of synchronization
                        Iterator<SnakesProto.GamePlayer> iter = msg.branches.iterator();
                        while (iter.hasNext()) {
                            if (Controller.players.contains(iter.next())) {
                                //if needed to be sent to MASTER, finds new MASTER
                                if (iter.next().getRole().equals(SnakesProto.NodeRole.MASTER)){
                                    SnakesProto.GamePlayer master = Controller.getPlayer(Controller.masterId);
                                    if (master != null) Network.send(msg.gm, master);
                                }
                                else Network.send(msg.gm, iter.next());
                            }
                            else iter.remove();
                        }
                        msg.sendtime = LocalTime.now();
                        resend.add(msg); //add to tail
                    }
                }
                MessageCustom next = resend.peek();
                if (next != null) {
                    invoketime = next.sendtime.plusNanos(Controller.config.getPingDelayMs() * 1000000);
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
                if (pair.getValue().plusNanos(Controller.config.getPingDelayMs() * 1000000).isBefore(LocalTime.now())){
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
        MessageCustom mst = new MessageCustom();
        mst.gm = message;
        //TODO : check if conroller.players changes between init of branches and iteration(client leaves or comes)
        mst.branches = new ArrayList<>(Controller.players);
        //TODO: mb sync branches iteration
        //our gameplayer, not resend to ourself
        mst.branches.removeIf(pl -> pl.getId() == Controller.playerId);

        mst.origtime = LocalTime.now();
        mst.sendtime = mst.origtime;
        //all messages in resend of message.type need to be updated
        //TODO: make iteration synchronized
        Iterator<MessageCustom> iter = resend.iterator();
        while (iter.hasNext()) {
            if (iter.next().gm.getTypeCase() == message.getTypeCase()) resend.remove(iter.next());
        }
        boolean res = resend.add(mst);
        if (invoketime == null) {
            invoketime = mst.origtime.plusNanos(Controller.config.getPingDelayMs() * 1000000);
        }
        //TODO: make iteration synchronized
        for (SnakesProto.GamePlayer gamePlayer : Controller.players) {
            Network.send(message, gamePlayer);
        }
    }
    private void send_single(SnakesProto.GameMessage message){
        MessageCustom mst = new MessageCustom();
        mst.gm = message;
        mst.branches = new ArrayList<>();
        SnakesProto.GamePlayer player = Controller.getPlayer(message.getReceiverId());
        if (player == null) return;
        mst.branches.add(player);
        mst.origtime = LocalTime.now();
        mst.sendtime = mst.origtime;
        Network.send(message, player);
        //check if we have older message of same type to same receiver
        //TODO: make iteration synchronized
        Iterator<MessageCustom> iter = resend.iterator();
        while (iter.hasNext()) {
            if ((iter.next().gm.getReceiverId() == message.getReceiverId()) && (iter.next().gm.getTypeCase() == message.getTypeCase())) {
                resend.remove(iter.next());
                break;
            }
        }
        boolean res = resend.add(mst);
        //System.out.println("result of offering = " + res + " tryed to offer " + message.packet.id.toString());
        if (invoketime == null) {
            invoketime = mst.origtime.plusNanos(Controller.config.getPingDelayMs() * 1000000);
        }
    }
    private static void send_ack(SnakesProto.GameMessage gm){
        //same as send_single, but don't add to resend
        SnakesProto.GamePlayer player = Controller.getPlayer(gm.getReceiverId());
        if (player == null) {
            return;
        }
        //TODO: make iteration synchronized
        Iterator<Map.Entry<Sender, SnakesProto.GameMessage>> it = NetworkReader.received.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Sender, SnakesProto.GameMessage> pair = it.next();
            //checks if in pair sender = receiver in gm and if msg_id's are equal
            if ((gm.getMsgSeq() == pair.getValue().getMsgSeq()) && (player.getIpAddress().equals(pair.getKey().ip)) && (player.getPort() == pair.getKey().port)) {
                Network.send(gm, Objects.requireNonNull(Controller.getPlayer(gm.getReceiverId())));
                break;
            }
        }

    }
    public void deleteclients(List<SnakesProto.GamePlayer> clients){
        //System.out.println("delete clients called");
        //TODO: check on importance of synchronization
        Iterator<SnakesProto.GamePlayer> it = clients.iterator();
        while (it.hasNext()) {
            //works with up-to-date player
            SnakesProto.GamePlayer player = Controller.getPlayer(it.next().getId());
            if (player == null) continue;
            Controller.players.remove(player);
            //we are normal, delete master
            if ((player.getRole().equals(SnakesProto.NodeRole.MASTER)) && (Objects.equals(Controller.getRole(Controller.playerId), SnakesProto.NodeRole.NORMAL))){
                Controller.changeMaster();
                continue;
            }
            //we are deputy, delete master
            if ((player.getRole().equals(SnakesProto.NodeRole.MASTER)) && (Objects.equals(Controller.getRole(Controller.playerId), SnakesProto.NodeRole.DEPUTY))){
                Controller.becomeMaster();
                continue;
            }
            //we are master, delete deputy
            if ((player.getRole().equals(SnakesProto.NodeRole.DEPUTY)) && (Objects.equals(Controller.getRole(Controller.playerId), SnakesProto.NodeRole.MASTER))){
                Controller.findDeputy();
                //continue;
            }
        }
    }
}

