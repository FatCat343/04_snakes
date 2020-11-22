import me.ippolitov.fit.snakes.SnakesProto;

import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;

public class GameProcess implements Runnable {
    public static ConcurrentHashMap<Integer, SnakesProto.Direction> steers = new ConcurrentHashMap<>();
    public void run() {
        SnakesProto.GameState.Builder gameState = SnakesProto.GameState.newBuilder();
        while(true) {
            try {
                sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            gameState.build();
            SnakesProto.GameState.Builder tmp = gameState.clone();
            NetworkWriter.queue.add(gameState);
            gameState = tmp;
        }
    }
    public static void start() {
        Thread t = new Thread(new GameProcess());
        t.start();
    }
    public static void setSteer(SnakesProto.Direction dir, int id){
        steers.put(id, dir);
    }

}
