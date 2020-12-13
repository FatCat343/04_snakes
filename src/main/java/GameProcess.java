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
    public static SnakesProto.GameState.Builder gameState;
    public static int aliveSnakes;
    public static boolean running;
    private static int stateOrder = 0;
    GameProcess(){
        gameState = SnakesProto.GameState.newBuilder();
        gameState.setConfig(Model.config);
        gameState.setStateOrder(getStateOrder());
        Controller.playerId = 0;
        SnakesProto.GameState.Snake res = findPlace(0);
        if (res != null) {
            //can be added, snake already placed + configured (or not)
            SnakesProto.GamePlayer.Builder p = SnakesProto.GamePlayer.newBuilder();

            p.setId(res.getPlayerId());
            p.setName(Controller.name);
            p.setIpAddress("1.1.1.1");
            p.setPort(Controller.port);
            p.setRole(SnakesProto.NodeRole.NORMAL);
            p.setScore(0);

            //gameState.getPlayers().getPlayersList().add(p.build());
            //List<SnakesProto.GamePlayer> pl = gameState.getPlayers().getPlayersList();
            //pl.add(p.build());
            gameState.addSnakes(res);
            SnakesProto.GamePlayers.Builder pls = SnakesProto.GamePlayers.newBuilder();
            pls.addPlayers(p);
            gameState.setPlayers(pls);
            //
            Model.setState(gameState.build());
            GameListSender.start();
            //Model.sendAck(gm, Controller.getId(sender));
        }
        else {
            //cant
            GUI.error("Cant start a game");
            Model.exit();
        }
    }
    GameProcess(SnakesProto.GameState state){
        gameState = SnakesProto.GameState.newBuilder(state);
        Controller.findDeputy();
    }

    public void run() {
        while(running) {
            try {
                sleep(2000);
                System.out.println("do turn");
                doTurn();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            gameState.setStateOrder(getStateOrder());
            Model.setState(gameState.build());
            Model.sendState(gameState.build());
            //Model.showState(gameState.build());
        }
    }
    public static void start() {
        Thread t = new Thread(new GameProcess());
        running = true;
        t.start();
    }
    public static void continueGame(SnakesProto.GameState state){
        Thread t = new Thread(new GameProcess(state));
        running = true;
        t.start();
    }
    public static void changeState(int id, SnakesProto.NodeRole role){
        //TODO: sync
        List<SnakesProto.GamePlayer> playerList = gameState.getPlayers().getPlayersList();
        for (int i = 0; i < playerList.size(); i++) {
            if (playerList.get(i).getId() == id) {
                SnakesProto.GamePlayer.Builder player = SnakesProto.GamePlayer.newBuilder(playerList.get(i));
                player.setRole(role);
                playerList.set(i, player.build());
                break;
            }
        }
        Model.setState(gameState.build());
    }
    public static void setSteer(SnakesProto.Direction dir, int id){
        //TODO: sync iteration
        List<SnakesProto.GameState.Snake> snakesList = gameState.getSnakesList();
        Iterator<SnakesProto.GameState.Snake> iter = snakesList.iterator();
        while (iter.hasNext()) {
            SnakesProto.GameState.Snake snake = iter.next();
            if (snake.getPlayerId() == id) {
                switch (snake.getHeadDirection()) {
                    case UP: if (!dir.equals(DOWN)) steers.put(id, dir);
                    case DOWN: if (!dir.equals(UP)) steers.put(id, dir);
                    case LEFT: if (!dir.equals(RIGHT)) steers.put(id, dir);
                    case RIGHT: if (!dir.equals(LEFT)) steers.put(id, dir);
                }
                break;
            }
        }
    }
    public static void makeZombie(int playerId){
        //TODO: sync
        List<SnakesProto.GameState.Snake> snakesList = gameState.getSnakesList();
        for (int i = 0; i < snakesList.size(); i++) {
            if (snakesList.get(i).getPlayerId() == playerId) {
                SnakesProto.GameState.Snake.Builder snake = SnakesProto.GameState.Snake.newBuilder(snakesList.get(i));
                snake.setState(SnakesProto.GameState.Snake.SnakeState.ZOMBIE);
                snake.setPlayerId(newZombieSnakeid());
                break;
            }
        }
        aliveSnakes--;
        Model.setState(gameState.build());
    }
    public static void deletePlayer(int playerId){
        //TODO: sync
        gameState.getPlayers().getPlayersList().remove(playerId);
        Model.setState(gameState.build());
    }
    private static int newZombieSnakeid(){
        //TODO: sync
        List<SnakesProto.GameState.Snake> snakesList = gameState.getSnakesList();
        int id = -1;
        while (true) {
            int i;
            for (i = 0; i < snakesList.size(); i++) {
                if (snakesList.get(i).getPlayerId() == id) {
                    break;
                }
            }
            if (i < snakesList.size()){  //found id somewhere
                id--;
            }
            else { //didnt found
                break;
            }
        }
        return id;
    }
    private static int getStateOrder(){
        stateOrder++;
        return stateOrder;
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
            SnakesProto.GameState.Snake snake = iter.next();
            List<SnakesProto.GameState.Coord> snakeBody = snake.getPointsList();
            int i = 0;
            Iterator<SnakesProto.GameState.Coord> it = snakeBody.iterator();
            SnakesProto.GameState.Coord prev = snakeBody.get(0);
            //SnakesProto.GameState.Coord tail = snakeBody.get(0);
            while (it.hasNext()){
                SnakesProto.GameState.Coord coord = it.next();
                if (i != 0) {
                    if (coord.getX() != 0){
                        int step;
                        if (coord.getX() < 0) {
                            step = -1;
                        }
                        else step = 1;
                        for (int j = prev.getX(); j != coord.getX(); j = (j + step) % Model.config.getWidth()){
                            SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
                            tmp.setY(coord.getY());
                            tmp.setX(j);
                            //tail = tmp.build();
                            used_checks.put(tmp.build(), snake.getPlayerId());
                            if (coord.getY() == (j + step) % Model.config.getWidth()){
                                tmp.setX(coord.getX());
                                tmp.setY(coord.getY());
                                tmp.build();
                                //tail = tmp.build();
                                used_checks.put(tmp.build(), snake.getPlayerId());
                            }
                        }
                    }
                    else { //y!=0
                        int step;
                        if (coord.getY() < 0) {
                            step = -1;
                        }
                        else step = 1;
                        for (int j = prev.getY(); j != coord.getY(); j = (j + step) % Model.config.getHeight()){
                            SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
                            tmp.setX(coord.getX());
                            tmp.setY(j);
                            tmp.build();
                            //tail = tmp.build();
                            used_checks.put(tmp.build(), snake.getPlayerId());
                            if (coord.getY() == (j + step) % Model.config.getHeight()){
                                tmp.setX(coord.getX());
                                tmp.setY(coord.getY());
                                tmp.build();
                                //tail = tmp.build();
                                used_checks.put(tmp.build(), snake.getPlayerId());
                            }
                        }
                    }
                    prev = coord;
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
            SnakesProto.GameState.Coord.Builder coord = SnakesProto.GameState.Coord.newBuilder
                    (tmp.setX((tmp.getX() + 2) % Model.config.getWidth()).setY((tmp.getY() + 2) % Model.config.getHeight()).build());
            snake.addPoints(0, coord.build());
            //snake.setPoints(0, tmp.setX((tmp.getX() + 2) % Model.config.getWidth()).setY((tmp.getY() + 2) % Model.config.getHeight()).build());
            //snake.setPoints(1, tmp.setX((tmp.getX() + 1) % Model.config.getWidth()).setY((tmp.getY() + 1) % Model.config.getHeight()).build());
            coord = SnakesProto.GameState.Coord.newBuilder(tmp.setX(1).setY(0).build());
            snake.addPoints(1, coord.build());
            snake.setHeadDirection(LEFT);
            snake.setPlayerId(id);
            snake.setState(SnakesProto.GameState.Snake.SnakeState.ALIVE);
            System.out.println(snake.build());
            return snake.build();
        }
    }

    public static void doTurn(){
        ConcurrentHashMap<Integer, SnakesProto.GameState.Coord> new_checks = new ConcurrentHashMap<>();
        ConcurrentHashMap<SnakesProto.GameState.Coord, Integer> used_checks = new ConcurrentHashMap<>();
        List<Integer> dead_players = new ArrayList<>();
        //TODO: sync iteration
        List<SnakesProto.GameState.Snake> snakesList = gameState.getSnakesList();
        Iterator<SnakesProto.GameState.Snake> iter = snakesList.iterator();
        while (iter.hasNext()) {
            SnakesProto.GameState.Snake snake = iter.next();
            SnakesProto.Direction dir = null;
            if (steers.containsKey(snake.getPlayerId())){
                dir = steers.get(snake.getPlayerId());
            }
            else dir = snake.getHeadDirection();
            SnakesProto.GameState.Coord.Builder newHead = SnakesProto.GameState.Coord.newBuilder();
            SnakesProto.GameState.Coord head = snake.getPoints(0);
            System.out.println("dir = " + dir);
            switch (dir) {
                case UP: {
                    newHead.setX(head.getX());
                    newHead.setY((Model.config.getHeight() + head.getY() - 1) % Model.config.getHeight());
                    break;
                }
                case DOWN: {
                    newHead.setX(head.getX());
                    newHead.setY((Model.config.getHeight() + head.getY() + 1) % Model.config.getHeight());
                    break;
                }
                case LEFT: {
                    //System.out.println((head.getX() - 1) % Model.config.getWidth());
                    newHead.setX((Model.config.getWidth() + head.getX() - 1) % Model.config.getWidth());
                    newHead.setY(head.getY());
                    break;
                }
                case RIGHT: {
                    newHead.setX((Model.config.getWidth() + head.getX() + 1) % Model.config.getWidth());
                    newHead.setY(head.getY());
                    break;
                }
            }
            //System.out.println(newHead);
            new_checks.put(snake.getPlayerId(), newHead.build());
            List<SnakesProto.GameState.Coord> snakeBody = snake.getPointsList();
            int i = 0;
            Iterator<SnakesProto.GameState.Coord> it = snakeBody.iterator();
            SnakesProto.GameState.Coord prev = head;
            SnakesProto.GameState.Coord tail = head;
            SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
            while (it.hasNext()){
                SnakesProto.GameState.Coord coord = it.next();
                if (i != 0) {
                    if (coord.getX() != 0){
                        int step;
                        if (coord.getX() < 0) {
                            step = -1;
                        }
                        else step = 1;
                        for (int j = prev.getX(); j != (Model.config.getWidth() + coord.getX() + prev.getX()) % Model.config.getWidth(); j = (Model.config.getWidth() + j + step) % Model.config.getWidth()){
                            tmp.setY(prev.getY());
                            tmp.setX(j);
                            tail = tmp.build();
                            used_checks.put(tmp.build(), snake.getPlayerId());
//                            if (coord.getY() == (j + step) % Model.config.getWidth()){
//                                tmp.setX(coord.getX());
//                                tmp.setY(coord.getY());
//                                tmp.build();
//                                tail = tmp.build();
//                                used_checks.put(tmp.build(), snake.getPlayerId());
//                            }
                        }
                        tmp.setX((Model.config.getWidth() + coord.getX() + prev.getX()) % Model.config.getWidth());
                        tail = tmp.build();
                        used_checks.put(tail, snake.getPlayerId());
                    }
                    else { //y!=0
                        int step;
                        if (coord.getY() < 0) {
                            step = -1;
                        }
                        else step = 1;
                        for (int j = prev.getY(); j != (Model.config.getHeight() + coord.getY() + prev.getY()) % Model.config.getHeight(); j = (Model.config.getHeight() + j + step) % Model.config.getHeight()){
                            tmp.setX(prev.getX());
                            tmp.setY(j);
                            //tmp.build();
                            tail = tmp.build();
                            used_checks.put(tmp.build(), snake.getPlayerId());
//                            if (coord.getY() == (j + step) % Model.config.getHeight()){
//                                tmp.setX(coord.getX());
//                                tmp.setY(coord.getY());
//                                tmp.build();
//                                tail = tmp.build();
//                                used_checks.put(tmp.build(), snake.getPlayerId());
//                            }
                        }
                        tmp.setY((Model.config.getHeight() + coord.getY() + prev.getY()) % Model.config.getHeight());
                        tail = tmp.build();
                        used_checks.put(tail, snake.getPlayerId());
                    }
                    prev = tmp.build();
                }
                i++;
            }
            if (!gameState.getFoodsList().contains(newHead.build())) {
                used_checks.remove(tail, snake.getPlayerId());
            }
            else {
                gameState.getFoodsList().remove(newHead.build());
                if (snake.getState() != SnakesProto.GameState.Snake.SnakeState.ZOMBIE) {
                    addPoint(snake.getPlayerId());
                }
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
            int deadid = dead.next();
            //gameState.getSnakesList().remove(gameState.getSnakes(dead.next()));
            if (deadid >= 0) Model.makeViewer(deadid);
        }
        snakesList = gameState.getSnakesList();
        //List<SnakesProto.GameState.Snake.Builder> snakesListnew = new ArrayList<>();
        Iterator<SnakesProto.GameState.Snake> snakeIterator= snakesList.iterator();
        int i = 0;
        //confirm moves of active snakes
        while(snakeIterator.hasNext()){
            SnakesProto.GameState.Snake snake = snakeIterator.next();

            SnakesProto.GameState.Snake.Builder newsnake = SnakesProto.GameState.Snake.newBuilder(snake);
           // snakesListnew.add(newsnake);
            System.out.println(newsnake);
            List<SnakesProto.GameState.Coord>coordList = newsnake.getPointsList();
            SnakesProto.GameState.Coord tail = coordList.get(coordList.size() - 1);
            SnakesProto.GameState.Coord head = coordList.get(0);
            SnakesProto.GameState.Coord newHead = new_checks.get(newsnake.getPlayerId());
            System.out.println(newHead);
            SnakesProto.GameState.Coord head_next = coordList.get(1);
            int step = 1;
            if (tail.getX() != 0) {
                if (tail.getX() < 0) step = -1;
                SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
                tmp.setY(tail.getY());
                tmp.setX(step * (abs(tail.getX()) - 1));
                if (tmp.getX() == 0) {
                    //snake.getPointsList().remove(coordList.size() - 1);
                    newsnake.removePoints(coordList.size() - 1);
                }
                else {
                    //snake.getPointsList().add(coordList.size() - 1, tmp.build());
                    newsnake.setPoints(coordList.size() - 1, tmp.build()); //was add
                }
            }
            else { //getY != 0
                if (tail.getY() < 0) step = -1;
                SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
                tmp.setX(tail.getX());
                tmp.setY(step * (abs(tail.getY()) - 1));
                if (tmp.getY() == 0) {
                    //snake.getPointsList().remove(coordList.size() - 1);
                    newsnake.removePoints(coordList.size() - 1);
                }
                else {
                    //snake.getPointsList().add(coordList.size() - 1, tmp.build());
                    newsnake.setPoints(coordList.size() - 1, tmp.build()); //was add
                }
            }
            SnakesProto.GameState.Coord.Builder tmp1 = SnakesProto.GameState.Coord.newBuilder();
            if (newHead.getX() == head.getX() && head_next.getX() == 0){
                //in 1 vert line
                tmp1.setX(0);
                //snake.getPointsList().set(0, newHead);
                newsnake.setPoints(0, newHead);
                //snake.getPointsList().set(1, tmp1.build());
                if (newsnake.getPointsList().size() > 2) {
                    tmp1.setY((head_next.getY() / abs(head_next.getY())) * (abs(head_next.getY()) + 1));
                    newsnake.setPoints(1, tmp1.build());
                }
                else {
                    tmp1.setY((head_next.getY() / abs(head_next.getY())) * (abs(head_next.getY())));
                    if (newsnake.getPointsList().size() > 1) newsnake.setPoints(1, tmp1.build());
                    else newsnake.addPoints(1, tmp1.build());
                }
            }
            else {
                if (newHead.getY() == head.getY() && head_next.getY() == 0){
                    //in 1 hor line
                    tmp1.setY(0);
                    //snake.getPointsList().set(0, newHead);
                    newsnake.setPoints(0, newHead);
                    //snake.getPointsList().set(1, tmp1.build());
                    //System.out.println(newsnake.build());
                    if (newsnake.getPointsList().size() > 2) {
                        tmp1.setX((head_next.getX() / abs(head_next.getX())) * (abs(head_next.getX()) + 1));
                        newsnake.setPoints(1, tmp1.build());
                    }
                    else {
                        tmp1.setX((head_next.getX() / abs(head_next.getX())) * (abs(head_next.getX())));
                        if (newsnake.getPointsList().size() > 1) newsnake.setPoints(1, tmp1.build());
                        else newsnake.addPoints(1, tmp1.build());
                    }
                    System.out.println(newsnake.build());
                }
                else {
                    //head is corner of snake
                    tmp1.setY(head.getY() - newHead.getY());
                    tmp1.setX(head.getX() - newHead.getX());
                    //snake.getPointsList().set(0, tmp1.build());
                    newsnake.setPoints(0, tmp1.build());
                    //snake.getPointsList().add(0, newHead);
                    newsnake.addPoints(0, newHead);
                }
            }

            gameState.setSnakes(i, newsnake.build());
            i++;
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
                    gameState.getFoodsList().add(pair.getKey());
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
            gameState.addFoods(tmp.build());
//            SnakesProto.GamePlayers.Builder pls = SnakesProto.GamePlayers.newBuilder();
//            pls.addPlayers(p);
//            gameState.setPlayers(pls);
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
