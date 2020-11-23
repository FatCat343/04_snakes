import me.ippolitov.fit.snakes.SnakesProto;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkReader implements Runnable{
//listens on all messages except multicast
    public static ConcurrentHashMap<Sender, SnakesProto.GameMessage> received = new ConcurrentHashMap<>();



    public static void start(){
        Thread t = new Thread(new NetworkReader());
        t.start();
    }

    @Override
    public void run() {
        while (true) {
            Sender sender = new Sender();
            SnakesProto.GameMessage gm = Network.receive(sender);
            if (gm == null) continue;
            System.out.println("sender ip = " + sender.ip);
            int alreadyReceived = 0;
            //TODO: make iteration synchronised
            for (Map.Entry<Sender, SnakesProto.GameMessage> pair : received.entrySet()) {
                SnakesProto.GameMessage message = pair.getValue();
                Sender sender1 = pair.getKey();
                if ((message.getMsgSeq() == gm.getMsgSeq()) && (sender1.equals(sender))) {
                    alreadyReceived = 1;
                    break;
                }
            }
            if (alreadyReceived == 1) continue;
            //check whether we have msg of same type from same sender
            //TODO: make iteration synchronised
            received.entrySet().removeIf(pair -> ((pair.getKey().equals(sender)) && (pair.getValue().getTypeCase().equals(gm.getTypeCase()))));
            //now we dont have msg of same type from same sender
            received.put(sender, gm);
            switch (gm.getTypeCase()) {
                case PING:{
                    Controller.pingAnswer(gm, sender);
                }
                case STEER:{
                    Controller.steer(gm, sender);
                }
                case ACK:{
                    Controller.ack(gm, sender);
                }
                case STATE:{
                    Controller.setState(gm, sender);
                }
                case ANNOUNCEMENT:{
                   System.out.println("ERROR, multicast received by wrong socket");
                }
                case JOIN:{
                    Controller.join(gm, sender);
                }
                case ERROR:{
                    Controller.error(gm);
                }
                case ROLE_CHANGE:{
                    Controller.roleChange(gm);
                }
            }
        }
    }

}
