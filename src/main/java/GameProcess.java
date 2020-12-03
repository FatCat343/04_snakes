import me.ippolitov.fit.snakes.SnakesProto;

import java.time.LocalTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;
import static me.ippolitov.fit.snakes.SnakesProto.Direction.*;

public class GameProcess implements Runnable {
    public static ConcurrentHashMap<Integer, SnakesProto.Direction> steers = new ConcurrentHashMap<>();
    public static SnakesProto.GameState.Builder gameState = SnakesProto.GameState.newBuilder();
    public void run() {

        while(true) {
            try {
                sleep(2000);
                doTurn(gameState);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //gameState.build();
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
        //TODO: sync iteration
        List<SnakesProto.GameState.Snake> snakesList = gameState.getSnakesList();
        Iterator<SnakesProto.GameState.Snake> iter = snakesList.iterator();
        //SnakesProto.GameState.Snake s = null;
        while (iter.hasNext()) {
            if (iter.next().getPlayerId() == id) {
                switch (iter.next().getHeadDirection()) {
                    case UP: if (!dir.equals(DOWN)) steers.put(id, dir);
                    case DOWN: if (!dir.equals(UP)) steers.put(id, dir);
                    case LEFT: if (!dir.equals(RIGHT)) steers.put(id, dir);
                    case RIGHT: if (!dir.equals(LEFT)) steers.put(id, dir);
                }
                break;
            }
        }
    }
    public static void newPlayer(SnakesProto.GameMessage gm, Sender sender){
        SnakesProto.GameState.Snake res = findPlace(); //finds + places if possible

        if (res != null) {
            //can be added, snake already placed + configured
            SnakesProto.GamePlayer.Builder p = SnakesProto.GamePlayer.newBuilder();

            p.setId(res.getPlayerId());
            p.setName(gm.getJoin().getName());
            p.setIpAddress(sender.ip);
            p.setPort(sender.port);
            p.setRole(SnakesProto.NodeRole.NORMAL);
            p.setScore(0);

            Controller.players.add(p.build());
            Model.sendAck(gm, Controller.getId(sender));
        }
        else {
            //cant
            Model.sendError(gm, sender, "Cant join");
        }
    }
    public static SnakesProto.GameState.Snake findPlace(){

        //finds empty square 5x5
        return null;
    }
    public static void placeFood(){

    }
    public static void confirmMoves(){

    }
    public static void doTurn(SnakesProto.GameState.Builder gameState){
        ConcurrentHashMap<Integer, SnakesProto.GameState.Coord> new_checks = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, SnakesProto.GameState.Coord> used_checks = new ConcurrentHashMap<>();
        //TODO: sync iteration
        List<SnakesProto.GameState.Snake> snakesList = gameState.getSnakesList();
        Iterator<SnakesProto.GameState.Snake> iter = snakesList.iterator();
        //SnakesProto.GameState.Snake s = null;
        while (iter.hasNext()) {
            SnakesProto.Direction dir = null;
            if (steers.containsKey(iter.next().getPlayerId())){
                dir = steers.get(iter.next().getPlayerId());
            }
            else dir = iter.next().getHeadDirection();
            SnakesProto.GameState.Coord.Builder newHead = SnakesProto.GameState.Coord.newBuilder();
            SnakesProto.GameState.Coord head = iter.next().getPoints(0);
            switch (dir) {
                case UP: {
                    newHead.setX(head.getX());
                    newHead.setY((head.getY() + 1) % Model.config.getHeight());
                }
                case DOWN: {
                    newHead.setX(head.getX());
                    newHead.setY((head.getY() - 1) % Model.config.getHeight());
                }
                case LEFT: {
                    newHead.setX((head.getY() - 1) % Model.config.getWidth());
                    newHead.setY(head.getY());
                }
                case RIGHT: {
                    newHead.setX((head.getY() + 1) % Model.config.getWidth());
                    newHead.setY(head.getY());
                }
            }
            new_checks.put(iter.next().getPlayerId(), newHead.build());
            List<SnakesProto.GameState.Coord> snakeBody = iter.next().getPointsList();
            int i = 0;
            Iterator<SnakesProto.GameState.Coord> it = snakeBody.iterator();
            SnakesProto.GameState.Coord prev = head;
            while (it.hasNext()){
                if (i != 0) {
                    if (it.next().getX() != 0){
                        while(){

                        }
                    }
                    else { //y!=0
                        while(){
                            used_checks.put(iter.next().getPlayerId(), );
                        }
                    }
                    prev = it.next();
                }
                i++;
            }
            if ()
//            if (dir.equals(iter.next().getHeadDirection())) {
//                iter.next().getPointsList().set(0, )
//            }

            //List<SnakesProto.GameState.Coord> coords = iter.next().getPointsList();
            iter.next().getPointsList().add(0, )
        }
    }
    public static int newPlayerId(){
        return 0;
    }

}
