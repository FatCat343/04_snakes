import me.ippolitov.fit.snakes.SnakesProto;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Thread.sleep;
import static me.ippolitov.fit.snakes.SnakesProto.Direction.*;

public class GameProcess implements Runnable {
    public static ConcurrentHashMap<Integer, SnakesProto.Direction> steers;
    public static SnakesProto.GameState.Builder gameState;
    public static int aliveSnakes;
    public static Thread t;
    public static boolean running;
    public static boolean finished;
    private static int stateOrder = 0;
    GameProcess(){
        running = true;
        finished = false;
        gameState = SnakesProto.GameState.newBuilder();
        steers = new ConcurrentHashMap<>();
        gameState.setConfig(Model.config);
        gameState.setStateOrder(getStateOrder());
        Controller.playerId = 0;
        Controller.masterId = 0;
        Controller.role = SnakesProto.NodeRole.MASTER;
        aliveSnakes = 0;
        SnakesProto.GameState.Snake res = findPlace(0);
        if (res != null) {
            //can be added, snake already placed + configured (or not)
            SnakesProto.GamePlayer.Builder p = SnakesProto.GamePlayer.newBuilder();
            aliveSnakes++;
            p.setId(res.getPlayerId());
            p.setName(Controller.name);
            p.setIpAddress("1.1.1.1");
            p.setPort(Controller.port);
            p.setRole(SnakesProto.NodeRole.MASTER);
            p.setScore(0);

            gameState.addSnakes(res);
            SnakesProto.GamePlayers.Builder pls = SnakesProto.GamePlayers.newBuilder();
            pls.addPlayers(p);
            gameState.setPlayers(pls);
            Model.setState(gameState.build());
            GameListSender.start();
        }
        else {
            //cant
            GUI.error("Cant start a game");
            Model.exit();
        }
    }

    public void run() {
        System.out.println("starts run");
        while(true) {
            try {
                sleep(2000);
                System.out.println("do turn");
                doTurn();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            gameState.setStateOrder(getStateOrder());
            if (running) {
                System.out.println("update model");
                Model.setState(gameState.build());
                Model.sendState(gameState.build());
            }
            else System.out.println("stops working");
        }
    }
    public static void start() {
        t = new Thread(new GameProcess());
        running = true;
        t.start();
    }
    public static void restart() {
        running = true;
    }
    public static void stop(){
        running = false;
    }
    public static void continueGame(SnakesProto.GameState state){
        gameState = SnakesProto.GameState.newBuilder(state);
        int count = 0;
        List<SnakesProto.GamePlayer> playerList = state.getPlayers().getPlayersList();
        for (int i = 0; i < playerList.size(); i++) {
            if (!playerList.get(i).getRole().equals(SnakesProto.NodeRole.VIEWER)) {
                count++;
            }
        }

        aliveSnakes = count;
        running = true;
    }
    public static void changeState(int id, SnakesProto.NodeRole role){
        synchronized (gameState) {
            List<SnakesProto.GamePlayer> playerList = gameState.getPlayers().getPlayersList();
            for (int i = 0; i < playerList.size(); i++) {
                if (playerList.get(i).getId() == id) {
                    SnakesProto.GamePlayer.Builder player = SnakesProto.GamePlayer.newBuilder(playerList.get(i));
                    player.setRole(role);
                    SnakesProto.GamePlayers.Builder players = SnakesProto.GamePlayers.newBuilder(gameState.getPlayers());
                    players.setPlayers(i, player.build());
                    //playerList.set(i, player.build());
                    gameState.setPlayers(players);
                    break;
                }
            }
            Model.setState(gameState.build());
        }
    }
    public static void setSteer(SnakesProto.Direction dir, int id){
        System.out.println("set steer "+ dir + " to player " + id);
        synchronized (gameState) {
            List<SnakesProto.GameState.Snake> snakesList = gameState.getSnakesList();
            Iterator<SnakesProto.GameState.Snake> iter = snakesList.iterator();
            while (iter.hasNext()) {
                SnakesProto.GameState.Snake snake = iter.next();
                if (snake.getPlayerId() == id) {
                    switch (snake.getHeadDirection()) {
                        case UP: {
                            if (!dir.equals(DOWN)) steers.put(id, dir);
                            break;
                        }
                        case DOWN: {
                            if (!dir.equals(UP)) steers.put(id, dir);
                            break;
                        }
                        case LEFT: {
                            if (!dir.equals(RIGHT)) steers.put(id, dir);
                            break;
                        }
                        case RIGHT: {
                            if (!dir.equals(LEFT)) steers.put(id, dir);
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }
    public static void makeZombie(int playerId){
        synchronized (gameState) {
            List<SnakesProto.GameState.Snake> snakesList = gameState.getSnakesList();
            for (int i = 0; i < snakesList.size(); i++) {
                if (snakesList.get(i).getPlayerId() == playerId) {
                    SnakesProto.GameState.Snake.Builder snake = SnakesProto.GameState.Snake.newBuilder(snakesList.get(i));
                    snake.setState(SnakesProto.GameState.Snake.SnakeState.ZOMBIE);
                    snake.setPlayerId(newZombieSnakeid());
                    gameState.setSnakes(i, snake);
                    aliveSnakes--;
                    break;
                }
            }
        }
        Model.setState(gameState.build());
    }
    public static void deletePlayer(int playerId){
        synchronized (gameState) {
            SnakesProto.GamePlayers.Builder players = SnakesProto.GamePlayers.newBuilder(gameState.getPlayers());
            int playerInd = Controller.findIdIndex(playerId);
            players.removePlayers(playerInd);
            gameState.setPlayers(players);
        }
        Model.setState(gameState.build());
    }
    private static int newZombieSnakeid(){
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
            System.out.println("can join player");
            aliveSnakes++;
            SnakesProto.GamePlayer.Builder p = SnakesProto.GamePlayer.newBuilder();

            p.setId(res.getPlayerId());
            p.setName(gm.getJoin().getName());
            p.setIpAddress(sender.ip);
            p.setPort(sender.port);
            if (Controller.findRole(SnakesProto.NodeRole.DEPUTY) >= 0) p.setRole(SnakesProto.NodeRole.NORMAL);
            else p.setRole(SnakesProto.NodeRole.DEPUTY);
            p.setScore(0);
            gameState.addSnakes(res);
            SnakesProto.GamePlayers.Builder pls = SnakesProto.GamePlayers.newBuilder(gameState.getPlayers());
            pls.addPlayers(p);
            gameState.setPlayers(pls);
            Model.state = gameState.build();
            Model.sendAck(gm, res.getPlayerId());
            Model.sendState(Model.state);
        }
        else {
            //cant
            System.out.println("cant join player");
            Model.sendError(gm, sender, "Cant join");
        }
    }
    //finds snake + places it
    public static SnakesProto.GameState.Snake findPlace(int id){
        ConcurrentHashMap<SnakesProto.GameState.Coord, Integer> used_checks = new ConcurrentHashMap<>();
        synchronized (gameState) {
            List<SnakesProto.GameState.Snake> snakesList = gameState.getSnakesList();
            Iterator<SnakesProto.GameState.Snake> iter = snakesList.iterator();
            while (iter.hasNext()) {
                SnakesProto.GameState.Snake snake = iter.next();
                List<SnakesProto.GameState.Coord> snakeBody = snake.getPointsList();
                int i = 0;
                Iterator<SnakesProto.GameState.Coord> it = snakeBody.iterator();
                SnakesProto.GameState.Coord prev = snakeBody.get(0);
                SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
                while (it.hasNext()) {
                    SnakesProto.GameState.Coord coord = it.next();
                    if (i != 0) {
                        if (coord.getX() != 0) {
                            int step;
                            if (coord.getX() < 0) {
                                step = -1;
                            } else step = 1;
                            for (int j = prev.getX();
                                 j != (Model.config.getWidth() + coord.getX() + prev.getX()) % Model.config.getWidth();
                                 j = (Model.config.getWidth() + j + step) % Model.config.getWidth()) {

                                tmp.setY(coord.getY());
                                tmp.setX(j);
                                used_checks.put(tmp.build(), snake.getPlayerId());
                            }
                            tmp.setX((Model.config.getWidth() + coord.getX() + prev.getX()) % Model.config.getWidth());
                            used_checks.put(tmp.build(), snake.getPlayerId());
                        } else { //y!=0
                            int step;
                            if (coord.getY() < 0) {
                                step = -1;
                            } else step = 1;
                            for (int j = prev.getY();
                                 j != (Model.config.getHeight() + coord.getY() + prev.getY()) % Model.config.getHeight();
                                 j = (Model.config.getHeight() + j + step) % Model.config.getHeight()) {
                                tmp.setX(coord.getX());
                                tmp.setY(j);
                                used_checks.put(tmp.build(), snake.getPlayerId());
                            }
                            tmp.setY((Model.config.getHeight() + coord.getY() + prev.getY()) % Model.config.getHeight());
                            used_checks.put(tmp.build(), snake.getPlayerId());
                        }
                        prev = coord;
                    }
                    i++;
                }
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
        List<Integer> food_eaters = new ArrayList<>();
        List<Integer> dead_players = new ArrayList<>();
        synchronized (gameState) {
            List<SnakesProto.GameState.Snake> snakesList = gameState.getSnakesList();
            Iterator<SnakesProto.GameState.Snake> iter = snakesList.iterator();
            SnakesProto.Direction dir = null;
            while (iter.hasNext()) {
                dir = null;
                SnakesProto.GameState.Snake snake = iter.next();
                if (steers.containsKey(snake.getPlayerId())) {
                    dir = steers.get(snake.getPlayerId());
                } else dir = snake.getHeadDirection();
                SnakesProto.GameState.Coord.Builder newHead = SnakesProto.GameState.Coord.newBuilder();
                SnakesProto.GameState.Coord head = snake.getPoints(0);
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
                System.out.println("dir = " + dir + " for player = " + snake.getPlayerId());
                new_checks.put(snake.getPlayerId(), newHead.build());
                List<SnakesProto.GameState.Coord> snakeBody = snake.getPointsList();
                int i = 0;
                Iterator<SnakesProto.GameState.Coord> it = snakeBody.iterator();
                SnakesProto.GameState.Coord prev = head;
                SnakesProto.GameState.Coord tail = head;
                SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
                while (it.hasNext()) {
                    SnakesProto.GameState.Coord coord = it.next();
                    if (i != 0) {
                        if (coord.getX() != 0) {
                            int step;
                            if (coord.getX() < 0) {
                                step = -1;
                            } else step = 1;
                            for (int j = prev.getX(); j != (Model.config.getWidth() + coord.getX() + prev.getX()) % Model.config.getWidth(); j = (Model.config.getWidth() + j + step) % Model.config.getWidth()) {
                                tmp.setY(prev.getY());
                                tmp.setX(j);
                                tail = tmp.build();
                                used_checks.put(tmp.build(), snake.getPlayerId());
                            }
                            tmp.setX((Model.config.getWidth() + coord.getX() + prev.getX()) % Model.config.getWidth());
                            tail = tmp.build();
                            used_checks.put(tail, snake.getPlayerId());
                        } else { //y!=0
                            int step;
                            if (coord.getY() < 0) {
                                step = -1;
                            } else step = 1;
                            for (int j = prev.getY(); j != (Model.config.getHeight() + coord.getY() + prev.getY()) % Model.config.getHeight(); j = (Model.config.getHeight() + j + step) % Model.config.getHeight()) {
                                tmp.setX(prev.getX());
                                tmp.setY(j);
                                tail = tmp.build();
                                used_checks.put(tmp.build(), snake.getPlayerId());
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
                } else {
                    int t = 0;
                    for (SnakesProto.GameState.Coord coord : gameState.getFoodsList()) {
                        if (coord.equals(newHead.build())) break;
                        else t++;
                    }
                    gameState.removeFoods(t);
                    if (snake.getState() != SnakesProto.GameState.Snake.SnakeState.ZOMBIE) {
                        addPoint(snake.getPlayerId());
                    }
                    food_eaters.add(snake.getPlayerId());
                }
            }
            Iterator<SnakesProto.GameState.Snake> it = snakesList.iterator();
            while (it.hasNext()) {
                SnakesProto.GameState.Snake snake = it.next();
                SnakesProto.GameState.Coord p_check = new_checks.get(snake.getPlayerId());
                new_checks.remove(snake.getPlayerId());
                if ((new_checks.containsValue(p_check)) || (used_checks.containsKey(p_check))) {
                    dead_players.add(snake.getPlayerId());
                    System.out.println("snake dead");
                    if (used_checks.containsKey(p_check)) {
                        addPoint(used_checks.get(p_check));
                    }
                }
                new_checks.put(snake.getPlayerId(), p_check);
            }
            Iterator<Integer> dead = dead_players.iterator();
            while (dead.hasNext()) {
                int deadid = dead.next();
                deleteSnake(deadid);
                if (deadid >= 0) Model.makeViewer(deadid);
            }
            snakesList = gameState.getSnakesList();
            Iterator<SnakesProto.GameState.Snake> snakeIterator = snakesList.iterator();
            int i = 0;
            //confirm moves of active snakes
            while (snakeIterator.hasNext()) {
                SnakesProto.GameState.Snake snake = snakeIterator.next();

                SnakesProto.GameState.Snake.Builder newsnake = SnakesProto.GameState.Snake.newBuilder(snake);
                List<SnakesProto.GameState.Coord> coordList = newsnake.getPointsList();
                SnakesProto.GameState.Coord tail = coordList.get(coordList.size() - 1);
                SnakesProto.GameState.Coord head = coordList.get(0);
                SnakesProto.GameState.Coord newHead = new_checks.get(newsnake.getPlayerId());
                SnakesProto.GameState.Coord head_next = coordList.get(1);
                int step = 1;
                if (!food_eaters.contains(snake.getPlayerId())) {
                    if (tail.getX() != 0) {
                        if (tail.getX() < 0) step = -1;
                        SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
                        tmp.setY(tail.getY());
                        tmp.setX(step * (abs(tail.getX()) - 1));
                        if (tmp.getX() == 0) {
                            newsnake.removePoints(coordList.size() - 1);
                        } else {
                            newsnake.setPoints(coordList.size() - 1, tmp.build()); //was add
                        }
                    } else { //getY != 0
                        if (tail.getY() < 0) step = -1;
                        SnakesProto.GameState.Coord.Builder tmp = SnakesProto.GameState.Coord.newBuilder();
                        tmp.setX(tail.getX());
                        tmp.setY(step * (abs(tail.getY()) - 1));
                        if (tmp.getY() == 0) {
                            newsnake.removePoints(coordList.size() - 1);
                        } else {
                            newsnake.setPoints(coordList.size() - 1, tmp.build()); //was add
                        }
                    }
                }
                SnakesProto.GameState.Coord.Builder tmp1 = SnakesProto.GameState.Coord.newBuilder();
                if (newHead.getX() == head.getX() && head_next.getX() == 0) {
                    //in 1 vert line
                    tmp1.setX(0);
                    newsnake.setPoints(0, newHead);
                    if (newsnake.getPointsList().size() > 1) {
                        head_next = newsnake.getPoints(1);
                        tmp1.setY((head_next.getY() / abs(head_next.getY())) * (abs(head_next.getY()) + 1));
                        newsnake.setPoints(1, tmp1.build());
                    } else {
                        tmp1.setY(head_next.getY());
                        newsnake.addPoints(1, tmp1.build());
                    }
                } else {
                    if (newHead.getY() == head.getY() && head_next.getY() == 0) {
                        //in 1 hor line
                        tmp1.setY(0);
                        newsnake.setPoints(0, newHead);
                        if (newsnake.getPointsList().size() > 1) {
                            head_next = newsnake.getPoints(1);
                            tmp1.setX((head_next.getX() / abs(head_next.getX())) * (abs(head_next.getX()) + 1));
                            newsnake.setPoints(1, tmp1.build());
                        } else {
                            tmp1.setX(head_next.getX());
                            newsnake.addPoints(1, tmp1.build());
                        }
                    } else {
                        //head is corner of snake
                        tmp1.setY(head.getY() - newHead.getY());
                        tmp1.setX(head.getX() - newHead.getX());
                        newsnake.setPoints(0, tmp1.build());
                        newsnake.addPoints(0, newHead);
                    }
                }
                if (steers.containsKey(snake.getPlayerId())) {
                    dir = steers.get(snake.getPlayerId());
                } else dir = snake.getHeadDirection();
                newsnake.setHeadDirection(dir);
                gameState.setSnakes(i, newsnake.build());
                i++;
            }

            //add food
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
            while (gameState.getFoodsList().size() < foodMax) {
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
                if (new_checks.contains(tmp.build())) continue;
                gameState.addFoods(tmp.build());
            }
        }
    }
    public static void addPoint(Integer playerId) {
        synchronized (gameState) {
            List<SnakesProto.GamePlayer> playerList = gameState.getPlayers().getPlayersList();
            for (int i = 0; i < playerList.size(); i++) {
                if (playerList.get(i).getId() == playerId) {
                    SnakesProto.GamePlayer.Builder player = SnakesProto.GamePlayer.newBuilder(playerList.get(i));
                    player.setScore(player.getScore() + 1);
                    SnakesProto.GamePlayers.Builder players = SnakesProto.GamePlayers.newBuilder(gameState.getPlayers());
                    players.setPlayers(i, player.build());
                    gameState.setPlayers(players.build());
                    break;
                }
            }
        }
    }
    public static int newPlayerId(){
        List<SnakesProto.GamePlayer> playerList = gameState.getPlayers().getPlayersList();
        int id = playerList.size();
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
    private static void deleteSnake(int id){
        List<SnakesProto.GameState.Snake> snakeList = gameState.getSnakesList();
        for (int i = 0; i < snakeList.size(); i++) {
            if (snakeList.get(i).getPlayerId() == id) {
                gameState.removeSnakes(i);
                break;
            }
        }
    }
}
