import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.sql.Time;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.time.temporal.ChronoUnit.SECONDS;

public class GameListReceiver implements Runnable {
    MulticastSocket socket;
    ConcurrentHashMap<String, LocalTime> table = new ConcurrentHashMap<>();
    public static void StartClient(MulticastSocket socket){
        Thread t = new Thread(new GameListReceiver(socket));
        t.start();
    }
    GameListReceiver(MulticastSocket s){
        socket = s;
    }
    public void UpdateTable(String msg){
        if (table.containsKey(msg)) table.replace(msg, LocalTime.now());
        else table.put(msg, LocalTime.now());
        Iterator<Map.Entry<String, LocalTime>> it = table.entrySet().iterator();
        while(it.hasNext()){
            //System.out.println("1");
            Map.Entry<String, LocalTime> pair = it.next();
            //System.out.println(pair.getKey());
            //TODO: change second.between
            if (pair.getValue().plusNanos(Model.config.getNodeTimeoutMs() * 1000000).isBefore(LocalTime.now())) { //timeout
                //System.out.println(pair.getKey() + "  " + pair.getValue() + "   " + LocalTime.now());
                it.remove();
            }
        }
    }
    public void receiveUDPMessage(String ip, int port) throws IOException {
        byte[] buffer = new byte[1024];
        InetAddress group = InetAddress.getByName("239.192.0.4");
        socket.joinGroup(group);
        while (true) {
            System.out.println("Waiting for multicast message...");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            System.out.println(packet.getSocketAddress().toString().split(":")[0]);
            String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
            System.out.println("[Multicast UDP message received] >> " + msg);
            UpdateTable(packet.getSocketAddress().toString().split(":")[0]);
            System.out.println("copies launched : " + table.size());
            GUI.repaintGameList(table);
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
