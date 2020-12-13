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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.time.temporal.ChronoUnit.SECONDS;

public class GameListReceiver implements Runnable {
    MulticastSocket socket;
    public static ConcurrentHashMap<GameListMessage, LocalTime> table = new ConcurrentHashMap<>();
    public static void StartClient(MulticastSocket socket){
        Thread t = new Thread(new GameListReceiver(socket));
        t.start();
    }
    GameListReceiver(MulticastSocket s){
        socket = s;
    }
    public void UpdateTable(GameListMessage msg){
        if (table.containsKey(msg)) table.replace(msg, LocalTime.now());
        else table.put(msg, LocalTime.now());
        Iterator<Map.Entry<GameListMessage, LocalTime>> it = table.entrySet().iterator();
        while(it.hasNext()){
            //System.out.println("1");
            Map.Entry<GameListMessage, LocalTime> pair = it.next();
            //System.out.println(pair.getKey());
            if (pair.getValue().plusNanos(Model.config.getNodeTimeoutMs() * 1000000).isBefore(LocalTime.now())) { //timeout
                //System.out.println(pair.getKey() + "  " + pair.getValue() + "   " + LocalTime.now());
                it.remove();
            }
        }
    }
    public void receiveUDPMessage(String ip, int port) throws IOException {
        //byte[] buffer = new byte[1024];
        InetAddress group = InetAddress.getByName(ip);
        socket.joinGroup(group);
        while (true) {
            //System.out.println("Waiting for multicast message...");
            Sender sender = new Sender();
            byte[] recvBuf = new byte[64000];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);

            socket.receive(packet);
            sender.ip = packet.getAddress().toString();
            sender.port = packet.getPort();
            //int byteCount = packet.getLength();
//            ByteArrayInputStream byteStream = new ByteArrayInputStream(recvBuf);
//            ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
//            try {
//                Object o = is.readObject();
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            }
//            is.close();
            //recvBuf = packet.getData();
            //System.out.println();
            SnakesProto.GameMessage msg = SnakesProto.GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));

            GameListMessage message = new GameListMessage();
            message.sender = sender;
            message.announce = msg.getAnnouncement();

            //System.out.println("[Multicast UDP message received] >> " + msg.toString());
            UpdateTable(message);
            //System.out.println("copies launched : " + table.size());
            //GUI.repaintGameList(table);
        }
    }

    @Override
    public void run() {
        try {
            receiveUDPMessage("239.192.0.4", 9192);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
