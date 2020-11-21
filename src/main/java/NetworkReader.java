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
            //TODO: make iteration synchronised
            //check whether we have msg of same type from same sender
            received.entrySet().removeIf(pair -> ((pair.getKey().equals(sender)) && (pair.getValue().getTypeCase().equals(gm.getTypeCase()))));
            //now we dont have msg of same type from same sender
            received.put(sender, gm);
            switch (gm.getTypeCase()) {
                case PING:{
                    Controller.pingAnswer(gm);
                }
                case STEER:{
                    Controller.steer(gm);
                }
                case ACK:{
                    Controller.ack(gm);
                }
                case STATE:{
                    Controller.setState(gm);
                }
                case ANNOUNCEMENT:{
                   System.out.println("ERROR, multicast received by wrong socket");
                }
                case JOIN:{
                    Controller.join(gm);
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
