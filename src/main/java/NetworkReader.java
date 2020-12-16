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
            if (gm == null) {
                System.out.println("received null message");
                continue;
            }
            //System.out.println("sender ip = " + sender.ip);
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
            if (alreadyReceived == 1) {
                System.out.println("already received this message");
                continue;
            }
            //TODO: check if we need messages from this sender
            if (Model.state == null){
                if (!Controller.neededsenders.contains(sender)) {
                    System.out.println("dont wait messages from this sender, list size = " + Controller.neededsenders.size());
                    continue;
                }
            }
            else {
                if (Controller.getId(sender) == -1){
                    if (!gm.getTypeCase().equals(SnakesProto.GameMessage.TypeCase.JOIN)) {
                        System.out.println("dont wait messages from this sender");
                        continue;
                    }
                }
            }

            //check whether we have msg of same type from same sender
            //TODO: make iteration synchronised
            received.entrySet().removeIf(pair -> ((pair.getKey().equals(sender)) && (pair.getValue().getTypeCase().equals(gm.getTypeCase()))));
            //now we dont have msg of same type from same sender
            received.put(sender, gm);
            switch (gm.getTypeCase()) {
                case PING:{
                    System.out.println("received ping");
                    Controller.pingAnswer(gm, sender);
                }
                case STEER:{
                    System.out.println("received steer");
                    Controller.steer(gm, sender);
                    break;
                }
                case ACK:{
                    System.out.println("received ack");
                    Controller.ack(gm, sender);
                    break;
                }
                case STATE:{
                    System.out.println("received state");
                    Controller.setState(gm, sender);
                    break;
                }
                case ANNOUNCEMENT:{
                   System.out.println("ERROR, multicast received by wrong socket");
                   break;
                }
                case JOIN:{
                    System.out.println("received join");
                    Controller.join(gm, sender);
                    break;
                }
                case ERROR:{
                    System.out.println("received error");
                    Controller.error(gm, sender);
                    break;
                }
                case ROLE_CHANGE:{
                    System.out.println("received role_change");
                    Controller.roleChange(gm, sender);
                    break;
                }
            }
        }
    }

}
