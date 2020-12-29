import me.ippolitov.fit.snakes.SnakesProto;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class Network {
    public static SnakesProto.GameMessage receive(Sender sender){
        try {
            byte[] recvBuf = new byte[64000];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            //System.out.println("port = " +Client.port);

            Model.socket.receive(packet);
            sender.ip = packet.getAddress().toString().split("/")[1];
            sender.port = packet.getPort();
            //System.out.println("received");
            //int byteCount = packet.getLength();
            //ByteArrayInputStream byteStream = new ByteArrayInputStream(recvBuf);
            //ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
            //Object o = is.readObject();
            //is.close();
            SnakesProto.GameMessage msg = SnakesProto.GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
            System.out.println("received msg with id = " + msg.getMsgSeq() + " from ip = " + sender.ip + " , port = " + sender.port
                                + " of type = " + msg.getTypeCase());
            return msg;
        }
        catch (IOException e) {
                System.err.println("Exception:  " + e);
                e.printStackTrace();
            }
        //catch (ClassNotFoundException e) { e.printStackTrace(); }
        return null;
    }
    public static void send(SnakesProto.GameMessage msg, SnakesProto.GamePlayer receiver) {
        try{
            byte[] sendBuf = msg.toByteArray();
            DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, InetAddress.getByName(receiver.getIpAddress()), receiver.getPort());
            System.out.println("sends msg with id = " + msg.getMsgSeq() + " to addr = " + receiver.getIpAddress() +
                                " to port = " + receiver.getPort() + "of type = " + msg.getTypeCase());
            Model.socket.send(packet);
            //TODO: make sync iter
            NetworkWriter.lastSent.put(receiver, LocalTime.now());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void send(SnakesProto.GameMessage msg, Sender receiver) {
        try{
            byte[] sendBuf = msg.toByteArray();
            DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, InetAddress.getByName(receiver.ip), receiver.port);
            System.out.println("sends msg with id = " + msg.getMsgSeq() + " to addr = " + receiver.ip +
                    " to port = " + receiver.port + "of type = " + msg.getTypeCase());
            Model.socket.send(packet);
            //TODO: make sync iter
            //NetworkWriter.lastSent.put(receiver, LocalTime.now());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
