import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import static java.lang.Thread.sleep;

public class GameListSender implements Runnable {
    public static void start(){
        Thread t = new Thread(new GameListSender());
        t.start();
    }
    public static void sendUDPMessage(String message, String ipAddress, int port, MulticastSocket socket) throws IOException {
        InetAddress group = InetAddress.getByName(ipAddress);
        byte[] msg = {1};
        DatagramPacket packet = new DatagramPacket(msg, 1 , group, port);
        socket.send(packet);
    }

    public void run() {
        try (MulticastSocket socket = new MulticastSocket(4321);){
            GameListReceiver.StartClient(socket);
            while (true) {
                sendUDPMessage("This is a multicast message", "239.192.0.4", 9192, socket);
                try {
                    sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
