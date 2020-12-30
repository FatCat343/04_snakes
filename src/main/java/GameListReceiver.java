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
        if (clients.containsKey(sender)) {
            clients.replace(sender, LocalTime.now());
            table.replace(sender, msg);
        }
        else {
            clients.put(sender, LocalTime.now());
            table.put(sender, msg);
        }
        Iterator<Map.Entry<Sender, LocalTime>> it = clients.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<Sender, LocalTime> pair = it.next();
            if (pair.getValue().plusSeconds(2).isBefore(LocalTime.now())) { //timeout
                Sender key = pair.getKey();
                table.remove(key);
                it.remove();
            }
        }
    }
    public void receiveUDPMessage(String ip, int port) throws IOException {
        InetAddress group = InetAddress.getByName(ip);
        socket.joinGroup(group);
        while (true) {
            Sender sender = new Sender();
            byte[] recvBuf = new byte[64000];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(packet);
            SnakesProto.GameMessage msg = SnakesProto.GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
            List<SnakesProto.GamePlayer> players = msg.getAnnouncement().getPlayers().getPlayersList();
            Iterator<SnakesProto.GamePlayer> iter = players.iterator();
            int j = 0;
            while (iter.hasNext()){
                if (iter.next().getRole().equals(SnakesProto.NodeRole.MASTER)) break;
                else j++;
            }
            if (j < msg.getAnnouncement().getPlayers().getPlayersList().size()) {
                sender.ip = packet.getAddress().toString().split("/")[1];
                sender.port = msg.getAnnouncement().getPlayers().getPlayers(j).getPort();
                UpdateTable(msg.getAnnouncement(), sender);
            }
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
