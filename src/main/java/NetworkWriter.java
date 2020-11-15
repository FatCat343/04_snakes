import me.ippolitov.fit.snakes.SnakesProto;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;

public class NetworkWriter implements Runnable {
    //waits on message queue or invokes to resend message
    //message -> if message send to all others except host (first check that it is not in the list) -> add to list with send time && origtime = sendtime
    //if reply -> send to destination host
    //compare time.now with InvokeTime -> sleep for diff millisec
    //invokes by time -> removes list[0] -> send list[0] && invoke time = 0 -> ads list[0] to the end of list with new send time
    //invoketime = =0 -> invoketime = list[0].time + 3000ms
    //private static ConcurrentLinkedQueue<MStruct> queue;
    public static PriorityQueue<Message> resend = new PriorityQueue<>();
    public static ConcurrentHashMap<Integer, Integer> lastMessage = new ConcurrentHashMap<>();
    public static BlockingQueue<SnakesProto.GameMessage> queue = new LinkedBlockingQueue<>() ; //queue on output
    public static ConcurrentHashMap<Integer, SnakesProto.GamePlayer> players = new ConcurrentHashMap<>(); //list of clients
    public LocalTime invoketime = null;
    public LocalTime checktime = null;
    public void run(){
        while (true) {
            SnakesProto.GameMessage message;
            try {
                message = queue.poll(3L, TimeUnit.SECONDS);
                //if (message == null) System.out.println("polled null message");
                if (message != null) {
                    //System.out.println("msg type = " +message.packet.type);
                    switch (message.getTypeCase()) {
                        case PING:{
                            send_single(message);
                        }
                        case STEER:{
                            send_single(message);
                        }
                        case ACK:{
                            send_single(message);
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

            //if (invoketime == null) //System.out.println("null");
            //else //System.out.println("seconds == "+SECONDS.between(LocalTime.now(), invoketime));

            while ((invoketime != null) && (SECONDS.between(LocalTime.now(), invoketime) <= 0)) {
                //resends oldest message
                UUID id =resend.poll();
                //System.out.println("resend old message, id = " + id + ", resend size = " + resend.size());
                if (id != null){
                    MStruct tmp = Client.list.get(id);
                    //System.out.println("tbs = "+tmp.branches.size());
                    if (SECONDS.between(LocalTime.now(), tmp.origtime.plusSeconds(30)) < 0) {
                        deleteclients(tmp.branches);
                        Client.list.remove(id);
                        //copied from end of while not to miss this part
                        UUID next = resend.peek();
                        if (next != null) {
                            MStruct tmp1 = Client.list.get(next);
                            invoketime = tmp1.sendtime.plusSeconds(3);
                            //System.out.println(invoketime.getSecond() - tmp1.sendtime.getSecond());
                        }
                        else invoketime = null;
                        continue;
                    }
                    //System.out.println("tbs = "+tmp.branches.size());
                    if (tmp.branches.size() > 0) {
                        for (int i = 0; i < tmp.branches.size(); i++) {
                            if (Client.clients.contains(tmp.branches.elementAt(i))) tmp.message.packet.send(tmp.branches.elementAt(i));
                        }
                        tmp.sendtime = LocalTime.now();
                        if (tmp.message.packet.type == MType.check) checktime = checktime.plusSeconds(3);
                        //System.out.println("id was added back");
                        resend.add(id); //add to tail
                    }
                    else {
                        Client.list.remove(id);
                        //System.out.println("removes from list id = " + id);
                    }
                }
                UUID next = resend.peek();
                if (next != null) {
                    MStruct tmp = Client.list.get(next);
                    invoketime = tmp.sendtime.plusSeconds(3);
                    //System.out.println(invoketime.getSecond() - tmp.sendtime.getSecond());
                }
                else invoketime = null;
            }

            //check secroot on availability by sending our secroot to him
            //once in 30 sec sends test msg to secroot waiting for reply
            //also removes all old messages from messagemap
            if (Client.secroot != null) {
                if (checktime == null) checktime = LocalTime.now();
                if (SECONDS.between(LocalTime.now(), checktime.plusSeconds(30)) < 0) {
                    Message tmp = new Message();
                    msg tmpacket = new msg();
                    tmpacket.id = UUID.randomUUID();
                    tmpacket.text = Client.secroot.addr + " " + Client.secroot.port;
                    tmpacket.type = MType.check;
                    tmpacket.cl = new ClientData();
                    tmpacket.cl = Client.secroot;
                    tmp.type = MType.single;
                    tmp.packet = tmpacket;
                    Client.queue.add(tmp);
                    checktime = LocalTime.now();
                    //System.out.println("Checking secroot was added on sending with id = " + tmp.packet.id);
                    //now remove messages
                    Iterator<Map.Entry<UUID, LocalTime>> it = Client.messages.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<UUID, LocalTime> pair = it.next();
                        LocalTime lastseen = pair.getValue();
                        //System.out.println("message with id = " + pair.getKey() + " was last seen at " + lastseen.toString());
                        if (lastseen.plusSeconds(15).isBefore(LocalTime.now())) { //timeout
                            it.remove();
                        }
                    }

                }
            }

        }
    }
    public static void Start(){
        Thread t = new Thread(new NetworkWriter());
        t.start();
    }
    public void send_all(SnakesProto.GameMessage message){
        Message mst = new Message();
        mst.gm = message;
        //mst.branches = Client.clients;
        mst.branches = new ConcurrentHashMap<>(players);
        //mst.branches.setSize(players.size());
        //Collections.copy(mst.branches, players);
        //System.out.println("branches size = " + mst.branches.size());
        //mst.branches.remove(mst.message.packet.cl); //remove host of message
        mst.origtime = LocalTime.now();
        mst.sendtime = mst.origtime;
        ///////lastMessage.put(message.packet.id, mst); same:
        //lastMessage.replaceAll( (k,v)->v=MY_VALUE );
        //System.out.println("puts in list new mess with id = " + message.packet.id);
        boolean res = resend.add(mst);
        //System.out.println("result of offering = " + res + " tryed to offer " + message.packet.id.toString());
        if (invoketime == null) {
            //System.out.println("adding 3 sec");
            invoketime = mst.origtime.plusSeconds(3);
            //System.out.println(invoketime.getSecond() - mst.origtime.getSecond());
        }
        for (int i = 0; i < Client.clients.size(); i++) { //iterate through hashmap
            if (!message.packet.cl.equals(Client.clients.elementAt(i))) {
                //System.out.println("NOT EQUALS at i = "+ i);
                Network.send(message, Client.clients.elementAt(i));
            }
            //else System.out.println("EQUALS at i = "+ i);
        }
    }
    public void send_single(SnakesProto.GameMessage message){
        Message mst = new Message();
        mst.gm = message;
        //mst.branches = Client.clients;
        mst.branches = new Vector<>();
        //Collections.copy(mst.branches, Client.clients);
        boolean res1 = mst.branches.add(players.get(message.getReceiverId()));
        //System.out.println("sending check msg to "+message.packet.cl.port+ " status - " + res1);
        //System.out.println("branches size = " + mst.branches.size());
        mst.origtime = LocalTime.now();
        mst.sendtime = mst.origtime;
        Client.list.put(message.packet.id, mst);
        Network.send(message, players.get(message.getReceiverId()));
        boolean res = resend.add(message.packet.id);
        //System.out.println("result of offering = " + res + " tryed to offer " + message.packet.id.toString());
        if (invoketime == null) {
            //System.out.println("adding 3 sec");
            invoketime = mst.origtime.plusSeconds(3);
            //System.out.println(invoketime.getSecond() - mst.origtime.getSecond());
        }
    }

    public void deleteclients(Vector<ClientData> clients){
        //System.out.println("delete clients called");
        for (int i = 0; i < clients.size(); i++){
            Client.clients.remove(clients.elementAt(i));
            if (clients.elementAt(i).equals(Client.secroot)) {
                //System.out.println("Secroot in unreacheble, self = " + Client.self.addr + Client.self.port + ", thirdroot = " + Client.thirdroot.addr + Client.thirdroot.port);
                if (!Client.thirdroot.equals(Client.self)) {
                    Client.secroot.addr = Client.thirdroot.addr;
                    Client.secroot.port = Client.thirdroot.port;
                    //System.out.println("connects to thirdroot");
                    Client.connect(Client.secroot);
                }
                else {
                    if (Client.clients.size() == 0) {
                        Client.secroot = null;
                        //System.out.println("we were alone && thirdroot, do nothing");
                    }
                    else {
                        Client.secroot.addr = Client.clients.get(0).addr;
                        Client.secroot.port = Client.clients.get(0).port;
                        //System.out.println("connects to child, addr = " + Client.secroot.addr + ", port = " + Client.secroot.port);
                        Client.connect(Client.secroot);
                    }
                }
            }
        }
    }
}

