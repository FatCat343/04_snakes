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
            //SnakesProto.GameMessage
        }
    }

}
