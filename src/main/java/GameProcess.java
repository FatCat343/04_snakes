import me.ippolitov.fit.snakes.SnakesProto;

import static java.lang.Thread.sleep;

public class GameProcess implements Runnable {
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
            NetworkWriter.sendAll(gameState);
            gameState = tmp;
        }
    }
    public static void start() {
        Thread t = new Thread(new GameProcess());
        t.start();
    }

}
