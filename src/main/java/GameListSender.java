import me.ippolitov.fit.snakes.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.time.LocalTime;

import static java.lang.Thread.sleep;

public class GameListSender implements Runnable {
    public static MulticastSocket socket;
    public static boolean running;
    public static void start(){
        Thread t = new Thread(new GameListSender());
        running = true;
        t.start();
    }
    public static void sendUDPMessage(SnakesProto.GameMessage msg, String ipAddress, int port, MulticastSocket socket) throws IOException {
        try{
            InetAddress group = InetAddress.getByName(ipAddress);
            byte[] sendBuf = msg.toByteArray();
            DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, group, port);
            //System.out.println("sends " + type + " with id = " + id + " to addr = " + cld.addr + " to port = " + cld.port);
            System.out.println("sends UDP");
            socket.send(packet);

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            socket = GameListReceiver.socket;
            //GameListReceiver.StartClient(socket);
            SnakesProto.GameMessage.Builder msg = SnakesProto.GameMessage.newBuilder();
            SnakesProto.GameMessage.AnnouncementMsg.Builder ann = SnakesProto.GameMessage.AnnouncementMsg.newBuilder();
            ann.setConfig(Model.config);
            while (true) {
                ann.setPlayers(Model.state.getPlayers());
                msg.setAnnouncement(ann.build());
                msg.setMsgSeq(Model.getMsgId());
                if (running) sendUDPMessage(msg.build(), "239.192.0.4", 9192, socket);
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
