import me.ippolitov.fit.snakes.SnakesProto;

public class NetworkReader implements Runnable{
//listens on all messages except multicast





    public static void start(){
        Thread t = new Thread(new NetworkReader());
        t.start();
    }

    @Override
    public void run() {
        while (true) {
            SnakesProto.GameMessage gm = Network.receive();
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
