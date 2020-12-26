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
    public static LocalTime invoketime = null;
    public LocalTime checktime = null;
    public static void start(){
        Thread t = new Thread(new NetworkWriter());
        t.start();
    }
    public void run(){
        while (true) {
            SnakesProto.GameMessage message;
            try {
                //System.out.println("waiting on new messages to send");
                message = queue.poll(Model.config.getPingDelayMs(), TimeUnit.MILLISECONDS);
                if (message != null) {
                    //System.out.println("polled message of type " + message.getTypeCase());
                    switch (message.getTypeCase()) {
                        case PING:
                        case ERROR:
                        case STEER:
                        case JOIN: {
                            send_single(message);
                            break;
                        }
                        case ACK:{
                            send_ack(message);
                            break;
                        }
                        case STATE:{
                            send_all(message);
                            break;
                        }
                        case ANNOUNCEMENT:{
                            System.out.println("ERROR, wrong type of message");
                            break;
                        }//wont be in queue
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
                            break;
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
                    //TODO: add special rules to resend error messages (shouldn't compare receiver to players)
                    //timeout for message
                    if (msg.origtime.plusNanos(Model.config.getNodeTimeoutMs() * 1000000).isBefore(LocalTime.now())) {
                        deleteclients(msg.branches);
                        //copied from end of while not to miss this part
                        MessageCustom next = resend.peek();
                        if (next != null) {
                            invoketime = next.sendtime.plusNanos(Model.config.getPingDelayMs() * 1000000);
                        }
                        else invoketime = null;
                        continue;
                    }
                    if (msg.branches.size() > 0) {
                        //TODO: check on importance of synchronization
                        synchronized (msg.branches) {
                            Iterator<SnakesProto.GamePlayer> iter = msg.branches.iterator();
                            while (iter.hasNext()) {
                                SnakesProto.GamePlayer player = iter.next();
                                Sender sender = new Sender();
                                sender.ip = player.getIpAddress();
                                sender.port = player.getPort();
                                if (!Controller.neededsenders.contains(sender)) {
                                    if (Model.state.getPlayers().getPlayersList().contains(player)) {
                                        //if needed to be sent to MASTER, finds new MASTER
                                        if (player.getRole().equals(SnakesProto.NodeRole.MASTER)) {
                                            SnakesProto.GamePlayer master = Controller.getPlayer(Controller.masterId);
                                            if (master != null) Network.send(msg.gm, master);
                                        } else Network.send(msg.gm, player);
                                    } else iter.remove();
                                } else Network.send(msg.gm, player);
                            }
                        }
                        msg.sendtime = LocalTime.now();
                        resend.add(msg); //add to tail
                    }
                }
                MessageCustom next = resend.peek();
                if (next != null) {
                    invoketime = next.sendtime.plusNanos(Model.config.getPingDelayMs() * 1000000);
                }
                else invoketime = null;
            }
            //check lastsent, if player is not in players - delete it, if localtime < now - 100ms -> send ping
            if (Model.state != null) {
                //TODO: iteration synchronization - v
                synchronized (lastSent) {
                    Iterator<Map.Entry<SnakesProto.GamePlayer, LocalTime>> it = lastSent.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<SnakesProto.GamePlayer, LocalTime> pair = it.next();
                        if (!Model.state.getPlayers().getPlayersList().contains(pair.getKey())) {
                            it.remove();
                            continue;
                        }
                        if (pair.getValue().plusNanos(Model.config.getPingDelayMs() * 1000000).isBefore(LocalTime.now())) {
                            send_ping(pair.getKey());
                        }
                    }
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
    public static void sendError(SnakesProto.GameMessage message, SnakesProto.GamePlayer receiver){
        //TODO: ?????????
        //System.out.println("sends error to " + receiver.getIpAddress()+":"+receiver.getPort());
        MessageCustom mst = new MessageCustom();
        mst.gm = message;
        mst.branches = new ArrayList<>();
        mst.branches.add(receiver);
        mst.origtime = LocalTime.now();
        mst.sendtime = mst.origtime;
        Network.send(message, receiver);
        //check if we have older message of same type to same receiver
        //TODO: make iteration synchronized - do we need it here?
        Iterator<MessageCustom> iter = resend.iterator();
        while (iter.hasNext()) {
            MessageCustom msg = iter.next();
            if ((msg.gm.getReceiverId() == message.getReceiverId()) && (msg.gm.getTypeCase() == message.getTypeCase())) {
                //resend.remove(iter.next());
                iter.remove();
                break;
            }
        }
        boolean res = resend.add(mst);
        //System.out.println("result of offering = " + res + " tryed to offer " + message.packet.id.toString());
        if (invoketime == null) {
            invoketime = mst.origtime.plusNanos(Model.config.getPingDelayMs() * 1000000);
        }
    }
    public static void Start(){
        Thread t = new Thread(new NetworkWriter());
        t.start();
    }
    private void send_all(SnakesProto.GameMessage message){
        //System.out.println("sends message to all");
        MessageCustom mst = new MessageCustom();
        mst.gm = message;
        //TODO : check if conroller.players changes between init of branches and iteration(client leaves or comes)
        mst.branches = new ArrayList<>(Model.state.getPlayers().getPlayersList());
        //TODO: mb sync branches iteration
        //our gameplayer, not resend to ourself
        //System.out.println("our playerid = " + Controller.playerId);
        mst.branches.removeIf(pl -> pl.getId() == Controller.playerId);

        mst.origtime = LocalTime.now();
        mst.sendtime = mst.origtime;
        //all messages in resend of message.type need to be updated
        //TODO: make iteration synchronized - v
        synchronized (resend) {
            Iterator<MessageCustom> iter = resend.iterator();
            while (iter.hasNext()) {
                MessageCustom msg = iter.next();
                if (msg.gm.getTypeCase() == message.getTypeCase()) resend.remove(msg);
            }
        }
        boolean res = resend.add(mst);
        if (invoketime == null) {
            invoketime = mst.origtime.plusNanos(Model.config.getPingDelayMs() * 1000000);
        }
        //TODO: make iteration synchronized
        synchronized (mst.branches) {
            for (SnakesProto.GamePlayer gamePlayer : mst.branches) {
                Network.send(message, gamePlayer);
            }
        }
    }
    private void send_single(SnakesProto.GameMessage message){
       // System.out.println("send msg to single receiver, id = " + message.getReceiverId());
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
        //TODO: make iteration synchronized - v
        synchronized (resend) {
            Iterator<MessageCustom> iter = resend.iterator();
            while (iter.hasNext()) {
                MessageCustom msg = iter.next();
                if ((msg.gm.getReceiverId() == message.getReceiverId()) && (msg.gm.getTypeCase() == message.getTypeCase())) {
                    resend.remove(msg);
                    break;
                }
            }
        }
        boolean res = resend.add(mst);
        //System.out.println("result of offering = " + res + " tryed to offer " + message.packet.id.toString());
        if (invoketime == null) {
            invoketime = mst.origtime.plusNanos(Model.config.getPingDelayMs() * 1000000);
        }
    }
    private static void send_ack(SnakesProto.GameMessage gm){
       // System.out.println("send ack to id = " + gm.getReceiverId());
        //same as send_single, but don't add to resend
        SnakesProto.GamePlayer player = Controller.getPlayer(gm.getReceiverId());
        if ((player == null)) {
            System.out.println("player == null");
            return;
        }
        //TODO: make iteration synchronized - v
        synchronized (NetworkReader.received) {
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

    }
    public void deleteclients(List<SnakesProto.GamePlayer> clients){
        System.out.println("delete clients called");
        if (Model.state != null) {
            //TODO: check on importance of synchronization
            Iterator<SnakesProto.GamePlayer> it = clients.iterator();
            while (it.hasNext()) {
                //works with up-to-date player
                SnakesProto.GamePlayer pl = it.next();
                SnakesProto.GamePlayer player = Controller.getPlayer(pl.getId());
                if (player == null) continue;
                //Model.state.getPlayers().getPlayersList().remove(player);
                Model.disconnect(player.getId());
                //we are normal, delete master
                if ((player.getRole().equals(SnakesProto.NodeRole.MASTER)) && (Objects.equals(Controller.getRole(Controller.playerId), SnakesProto.NodeRole.NORMAL))) {
                    Controller.changeMaster();
                    continue;
                }
                //we are deputy, delete master
                if ((player.getRole().equals(SnakesProto.NodeRole.MASTER)) && (Objects.equals(Controller.getRole(Controller.playerId), SnakesProto.NodeRole.DEPUTY))) {
                    Controller.becomeMaster();
                    continue;
                }
                //we are master, delete deputy
                if ((player.getRole().equals(SnakesProto.NodeRole.DEPUTY)) && (Objects.equals(Controller.getRole(Controller.playerId), SnakesProto.NodeRole.MASTER))) {
                    Controller.findDeputy();
                    //continue;
                }
            }
        }
    }
}

