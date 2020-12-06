import me.ippolitov.fit.snakes.SnakesProto;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Thread.sleep;
import static me.ippolitov.fit.snakes.SnakesProto.Direction.*;

public class GameProcess implements Runnable {
    public static ConcurrentHashMap<Integer, SnakesProto.Direction> steers = new ConcurrentHashMap<>();
    public static SnakesProto.GameState.Builder gameState = SnakesProto.GameState.newBuilder();
    public static int aliveSnakes;
    public void run() {
        while(true) {
            try {
                sleep(2000);
                doTurn();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            gameState.setStateOrder(gameState.getStateOrder() + 1);
            Model.setState(gameState.build());
            Model.sendState(gameState.build());
            Model.showState(gameState.build());
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
        SnakesProto.GameState.Snake res = findPlace(newPlayerId()); //finds + places if possible
        if (res != null) {
            //can be added, snake already placed + configured
            SnakesProto.GamePlayer.Builder p = SnakesProto.GamePlayer.newBuilder();

            p.setId(res.getPlayerId());
            p.setName(gm.getJoin().getName());
            p.setIpAddress(sender.ip);
            p.setPort(sender.port);
            p.setRole(SnakesProto.NodeRole.NORMAL);
            p.setScore(0);

            gameState.getPlayers().getPlayersList().add(p.build());
            Model.setState(gameState.build());
            Model.sendAck(gm, Controller.getId(sender));
        }
        else {
            //cant
            Model.sendError(gm, sender, "Cant join");
        }
    }
    //finds snake + places it
    public static SnakesProto.GameState.Snake findPlace(int id){
        aliveSnakes++;
        ConcurrentHashMap<SnakesProto.GameState.Coord, Integer> used_checks = new ConcurrentHashMap<>();
        //TODO: sync iteration
        List<SnakesProto.GameState.Snake> snakesList = gameState.getSnakesList();
        Iterator<SnakesProto.GameState.Snake> iter = snakesList.iterator();
        while (iter.hasNext()) {
            List<SnakesProto.GameState.Coord> snakeBody = iter.next().getPointsList();
            int i = 0;
            Iterator<SnakesProto.GameState.Coord> it = snakeBody.iterator();
            SnakesProto.GameState.Coord prev = snakeBody.get(0);
            //SnakesProto.GameState.Coord tail = snakeBody.get(0);
            while (it.hasNext()){
                if (i != 0) {
                    if (it.next().getX() != 0){
                        int step;
                        if (it.next().getX() < 0) {
                            step = -1;
                        }
                        else step = 1;
                        for (int j = prev.getX(); j != it.next().getX(); j = (j + step) % Model.config.getWidth()){
                            SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
                            tmp.setY(it.next().getY());
                            tmp.setX(j);
                            //tail = tmp.build();
                            used_checks.put(tmp.build(), iter.next().getPlayerId());
                            if (it.next().getY() == (j + step) % Model.config.getWidth()){
                                tmp.setX(it.next().getX());
                                tmp.setY(it.next().getY());
                                tmp.build();
                                //tail = tmp.build();
                                used_checks.put(tmp.build(), iter.next().getPlayerId());
                            }
                        }
                    }
                    else { //y!=0
                        int step;
                        if (it.next().getY() < 0) {
                            step = -1;
                        }
                        else step = 1;
                        for (int j = prev.getY(); j != it.next().getY(); j = (j + step) % Model.config.getHeight()){
                            SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
                            tmp.setX(it.next().getX());
                            tmp.setY(j);
                            tmp.build();
                            //tail = tmp.build();
                            used_checks.put(tmp.build(), iter.next().getPlayerId());
                            if (it.next().getY() == (j + step) % Model.config.getHeight()){
                                tmp.setX(it.next().getX());
                                tmp.setY(it.next().getY());
                                tmp.build();
                                //tail = tmp.build();
                                used_checks.put(tmp.build(), iter.next().getPlayerId());
                            }
                        }
                    }
                    prev = it.next();
                }
                i++;
            }
        }
        int foundPlace = 0;
        SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
        for (int i = 0; i < Model.config.getWidth(); i++){
            for (int j = 0; j < Model.config.getHeight(); j++){
                tmp.setX(i).setY(j);
                int k;
                for (k = 0; k < 25; k++){
                    SnakesProto.GameState.Coord.Builder tmp1 = SnakesProto.GameState.Coord.newBuilder();
                    tmp1.setX((i + k % 5) % Model.config.getWidth()).setY((j + k / 5) % Model.config.getHeight());
                    if (used_checks.containsKey(tmp1.build())) break;
                }
                if (k == 25) {
                    foundPlace = 1;
                    break;
                }
            }
            if (foundPlace == 1) break;
        }
        SnakesProto.GameState.Snake.Builder snake = SnakesProto.GameState.Snake.newBuilder();
        if (foundPlace == 0) return null;
        else {
            snake.setPoints(0, tmp.setX((tmp.getX() + 2) % Model.config.getWidth()).setY((tmp.getY() + 2) % Model.config.getHeight()).build());
            snake.setPoints(1, tmp.setX((tmp.getX() + 1) % Model.config.getWidth()).setY((tmp.getY() + 1) % Model.config.getHeight()).build());
            snake.setHeadDirection(LEFT);
            snake.setPlayerId(id);
            snake.setState(SnakesProto.GameState.Snake.SnakeState.ALIVE);
            return snake.build();
        }
    }
//    public static void placeFood(){
//        //find empty (not food or snake)
//        //add to food
//    }
//    public static void confirmMoves(){
//
//    }
    public static void doTurn(){
        ConcurrentHashMap<Integer, SnakesProto.GameState.Coord> new_checks = new ConcurrentHashMap<>();
        ConcurrentHashMap<SnakesProto.GameState.Coord, Integer> used_checks = new ConcurrentHashMap<>();
        List<Integer> dead_players = new ArrayList<>();
        //TODO: sync iteration
        List<SnakesProto.GameState.Snake> snakesList = gameState.getSnakesList();
        Iterator<SnakesProto.GameState.Snake> iter = snakesList.iterator();
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
                    newHead.setY((head.getY() - 1) % Model.config.getHeight());
                }
                case DOWN: {
                    newHead.setX(head.getX());
                    newHead.setY((head.getY() + 1) % Model.config.getHeight());
                }
                case LEFT: {
                    newHead.setX((head.getX() - 1) % Model.config.getWidth());
                    newHead.setY(head.getY());
                }
                case RIGHT: {
                    newHead.setX((head.getX() + 1) % Model.config.getWidth());
                    newHead.setY(head.getY());
                }
            }
            new_checks.put(iter.next().getPlayerId(), newHead.build());
            List<SnakesProto.GameState.Coord> snakeBody = iter.next().getPointsList();
            int i = 0;
            Iterator<SnakesProto.GameState.Coord> it = snakeBody.iterator();
            SnakesProto.GameState.Coord prev = head;
            SnakesProto.GameState.Coord tail = head;
            while (it.hasNext()){
                if (i != 0) {
                    if (it.next().getX() != 0){
                        int step;
                        if (it.next().getX() < 0) {
                            step = -1;
                        }
                        else step = 1;
                        for (int j = prev.getX(); j != it.next().getX(); j = (j + step) % Model.config.getWidth()){
                            SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
                            tmp.setY(it.next().getY());
                            tmp.setX(j);
                            tail = tmp.build();
                            used_checks.put(tmp.build(), iter.next().getPlayerId());
                            if (it.next().getY() == (j + step) % Model.config.getWidth()){
                                tmp.setX(it.next().getX());
                                tmp.setY(it.next().getY());
                                tmp.build();
                                tail = tmp.build();
                                used_checks.put(tmp.build(), iter.next().getPlayerId());
                            }
                        }
                    }
                    else { //y!=0
                        int step;
                        if (it.next().getY() < 0) {
                            step = -1;
                        }
                        else step = 1;
                        for (int j = prev.getY(); j != it.next().getY(); j = (j + step) % Model.config.getHeight()){
                            SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
                            tmp.setX(it.next().getX());
                            tmp.setY(j);
                            tmp.build();
                            tail = tmp.build();
                            used_checks.put(tmp.build(), iter.next().getPlayerId());
                            if (it.next().getY() == (j + step) % Model.config.getHeight()){
                                tmp.setX(it.next().getX());
                                tmp.setY(it.next().getY());
                                tmp.build();
                                tail = tmp.build();
                                used_checks.put(tmp.build(), iter.next().getPlayerId());
                            }
                        }
                    }
                    prev = it.next();
                }
                i++;
            }
            if (!gameState.getFoodsList().contains(newHead.build())) {
                used_checks.remove(tail, iter.next().getPlayerId());
            }
            else {
                gameState.getFoodsList().remove(newHead.build());
                addPoint(iter.next().getPlayerId());
            }
        }
        Iterator<SnakesProto.GameState.Snake> it = snakesList.iterator();
        while (it.hasNext()) {
            SnakesProto.GameState.Snake snake = it.next();
            SnakesProto.GameState.Coord p_check = new_checks.get(snake.getPlayerId());
            new_checks.remove(snake.getPlayerId());
            if ((new_checks.containsValue(p_check)) || (used_checks.containsKey(p_check))) {
                dead_players.add(snake.getPlayerId());
                if (used_checks.containsKey(p_check)) {
                    addPoint(used_checks.get(p_check));
                }
            }
            new_checks.put(snake.getPlayerId(), p_check);
        }
        Iterator<Integer> dead = dead_players.iterator();
        while(dead.hasNext()){
            //gameState.getSnakesList().remove(gameState.getSnakes(dead.next()));
            Model.becomeViewer(dead.next());
        }
        snakesList = gameState.getSnakesList();
        Iterator<SnakesProto.GameState.Snake> snakeIterator= snakesList.iterator();
        //confirm moves of active snakes
        while(snakeIterator.hasNext()){
            List<SnakesProto.GameState.Coord>coordList = snakeIterator.next().getPointsList();
            SnakesProto.GameState.Coord tail = coordList.get(coordList.size() - 1);
            SnakesProto.GameState.Coord head = coordList.get(0);
            SnakesProto.GameState.Coord newHead = new_checks.get(snakeIterator.next().getPlayerId());
            SnakesProto.GameState.Coord head_next = coordList.get(1);
            int step = 1;
            if (tail.getX() != 0) {
                if (tail.getX() < 0) step = -1;
                SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
                tmp.setY(tail.getY());
                tmp.setX(step * (abs(tail.getX()) - 1));
                if (tmp.getX() == 0) {
                    snakeIterator.next().getPointsList().remove(coordList.size() - 1);
                }
                else {
                    snakeIterator.next().getPointsList().add(coordList.size() - 1, tmp.build());
                }
            }
            else { //getY != 0
                if (tail.getY() < 0) step = -1;
                SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
                tmp.setX(tail.getX());
                tmp.setY(step * (abs(tail.getY()) - 1));
                if (tmp.getY() == 0) {
                    snakeIterator.next().getPointsList().remove(coordList.size() - 1);
                }
                else {
                    snakeIterator.next().getPointsList().add(coordList.size() - 1, tmp.build());
                }
            }
            SnakesProto.GameState.Coord.Builder tmp1 = SnakesProto.GameState.Coord.newBuilder();
            if (newHead.getX() == head.getX() && head_next.getX() == 0){
                //in 1 vert line
                tmp1.setY((head_next.getY() / abs(head_next.getY())) * (abs(head_next.getY() + 1)));
                tmp1.setX(newHead.getX());
                snakeIterator.next().getPointsList().set(0, newHead);
                snakeIterator.next().getPointsList().set(1, tmp1.build());
            }
            else {
                if (newHead.getY() == head.getY() && head_next.getY() == 0){
                    //in 1 hor line
                    tmp1.setY((head_next.getX() / abs(head_next.getX())) * (abs(head_next.getX() + 1)));
                    tmp1.setX(newHead.getY());
                    snakeIterator.next().getPointsList().set(0, newHead);
                    snakeIterator.next().getPointsList().set(1, tmp1.build());
                }
                else {
                    //head is corner of snake
                    tmp1.setY(head.getY() - newHead.getY());
                    tmp1.setX(head.getX() - newHead.getX());
                    snakeIterator.next().getPointsList().set(0, tmp1.build());
                    snakeIterator.next().getPointsList().add(0, newHead);
                }
            }
        }
        //add food
        //aliveSnakes = snake.size - dead.size
        float foodMax = Model.config.getFoodStatic() + aliveSnakes * Model.config.getFoodPerPlayer();
        Random rnd = new Random();
        Iterator<Map.Entry<SnakesProto.GameState.Coord, Integer>> it1 = used_checks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<SnakesProto.GameState.Coord, Integer> pair = it1.next();
            if (gameState.getFoodsList().size() >= foodMax) break;
            if (dead_players.contains(pair.getValue())) {
                int spawn_chance = rnd.nextInt(100);
                if (Model.config.getDeadFoodProb() < spawn_chance) {
                    gameState.getFoodsList().add(it1.next().getKey());
                }
            }
        }
        //add food if needed
        while (gameState.getFoodsList().size() < foodMax){
            //place food: find non food or alive_snake + add to food
            int x = rnd.nextInt(Model.config.getWidth());
            int y = rnd.nextInt(Model.config.getHeight());
            SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
            tmp.setX(x).setY(y);
            if (gameState.getFoodsList().contains(tmp.build())) continue; //already food there
            if (used_checks.containsKey(tmp.build())) {
                if (!dead_players.contains(used_checks.get(tmp.build()))) { //point where is a snake
                    continue;
                }
            }
            if (new_checks.contains(tmp.build())) continue; //point where is (or not) a snake
            //solution: add all relevant to used_check and check only used_check
            gameState.getFoodsList().add(tmp.build());
        }

        //mb update stats
    }
    public static void addPoint(Integer playerId) {
        //TODO: sync iteration
        List<SnakesProto.GamePlayer> playerList = gameState.getPlayers().getPlayersList();
        for (int i = 0; i < playerList.size(); i++) {
            if (playerList.get(i).getId() == playerId) {
                SnakesProto.GamePlayer.Builder player = SnakesProto.GamePlayer.newBuilder(playerList.get(i));
                player.setScore(player.getScore() + 1);
                playerList.set(i, player.build());
                break;
            }
        }
    }
    public static int newPlayerId(){
        List<SnakesProto.GamePlayer> playerList = gameState.getPlayers().getPlayersList();
        int id = playerList.size() + 1;
        for (int i = 0; i < playerList.size(); i++) {
            int j;
            for (j = 0; j < playerList.size(); j++) {
                if (playerList.get(j).getId() == i) {
                    break;
                }
            }
            if (j == playerList.size()) {
                id = i;
                break;
            }
        }
        return id;
    }

}
