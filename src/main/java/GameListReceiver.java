import me.ippolitov.fit.snakes.SnakesProto;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.sql.Time;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.time.temporal.ChronoUnit.SECONDS;

public class GameListReceiver implements Runnable {
    public static MulticastSocket socket;
    public static ConcurrentHashMap<Sender, SnakesProto.GameMessage.AnnouncementMsg> table = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Sender, LocalTime> clients = new ConcurrentHashMap<>();
    public static void start(){
        Thread t = new Thread(new GameListReceiver());
        t.start();
    }
    GameListReceiver(){}
    public void UpdateTable(SnakesProto.GameMessage.AnnouncementMsg msg, Sender sender){
        //String senderstring = sender.ip + " " + sender.port;
        if (clients.containsKey(sender)) {
            //System.out.println("replace, amount = " + clients.size());
            clients.replace(sender, LocalTime.now());
            table.replace(sender, msg);
        }
        else {
            //System.out.println("put, amount = " + clients.size());
            clients.put(sender, LocalTime.now());
            table.put(sender, msg);
        }
        Iterator<Map.Entry<Sender, LocalTime>> it = clients.entrySet().iterator();
        while(it.hasNext()){
            //System.out.println("1");
            Map.Entry<Sender, LocalTime> pair = it.next();
            //System.out.println(pair.getKey());
            if (pair.getValue().plusSeconds(2).isBefore(LocalTime.now())) { //timeout
                //System.out.println(pair.getKey() + "  " + pair.getValue() + "   " + LocalTime.now());
                Sender key = pair.getKey();

                System.out.println("remove " + key.port + "  " + key.ip);
                table.remove(key);
                it.remove();
            }
        }
    }
    public void receiveUDPMessage(String ip, int port) throws IOException {
        //byte[] buffer = new byte[1024];
        InetAddress group = InetAddress.getByName(ip);
        socket.joinGroup(group);
        while (true) {
            Sender sender = new Sender();
            byte[] recvBuf = new byte[64000];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);

            socket.receive(packet);

            //System.out.println("received from ip = " + sender.ip + " port = ");

            //sender.port = packet.getPort();
            SnakesProto.GameMessage msg = SnakesProto.GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
            List<SnakesProto.GamePlayer> players = msg.getAnnouncement().getPlayers().getPlayersList();
            Iterator<SnakesProto.GamePlayer> iter = players.iterator();
            int j = 0;
            while (iter.hasNext()){
                if (iter.next().getRole().equals(SnakesProto.NodeRole.MASTER)) break;
                else j++;
            }
            sender.ip = packet.getAddress().toString().split("/")[1];
            sender.port = msg.getAnnouncement().getPlayers().getPlayers(j).getPort();
            System.out.println("received packet from ip = " + sender.ip + "  port = " + sender.port);
            //GameListMessage message = new GameListMessage();
            //message.sender = sender;
            //message.announce = msg.getAnnouncement();
            UpdateTable(msg.getAnnouncement(), sender);
            System.out.println("servers available: "+clients.size() + "  table size = " + table.size());
        }
    }

    @Override
    public void run() {
        try (MulticastSocket sock = new MulticastSocket(9192);){
            socket = sock;
            receiveUDPMessage("239.192.0.4", 9192);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
