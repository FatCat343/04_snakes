import me.ippolitov.fit.snakes.SnakesProto;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class Network {
    public static SnakesProto.GameMessage receive(){
        try {
            byte[] recvBuf = new byte[5000];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            //System.out.println("port = " +Client.port);

            Model.socket.receive(packet);
            //System.out.println("received");
            int byteCount = packet.getLength();
            ByteArrayInputStream byteStream = new ByteArrayInputStream(recvBuf);
            ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
            Object o = is.readObject();
            is.close();
            SnakesProto.GameMessage msg = SnakesProto.GameMessage.parseFrom(recvBuf);
            return msg;
        }
        catch (IOException e) {
                System.err.println("Exception:  " + e);
                e.printStackTrace();
            }
        catch (ClassNotFoundException e) { e.printStackTrace(); }
        return null;
    }
    public static void send(SnakesProto.GameMessage msg, SnakesProto.GamePlayer receiver) {
        try{
            byte[] sendBuf = msg.toByteArray();
            DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, InetAddress.getByName(receiver.getIpAddress()), receiver.getPort());
            //System.out.println("sends " + type + " with id = " + id + " to addr = " + cld.addr + " to port = " + cld.port);
            Model.socket.send(packet);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
