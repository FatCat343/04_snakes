import me.ippolitov.fit.snakes.SnakesProto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

class ConnectListener implements ActionListener{
    ConnectListener(){

    }
    @Override
    public void actionPerformed(ActionEvent e) {
        Controller.connect();
    }
}


public class GUI {
    public static JFrame window = new JFrame("snake");
    private static JPanel game;
    private static JPanel rightSide;
    private static JPanel tables;
    private static JTable scores;
    private static JTable gameSpecs;
    private static JPanel gameList;
    private static JPanel buttons;
    private static JPanel gameField;
    private static JButton newGame;
    private static JButton exit;
    public static int ox = 40;
    public static int oy = 30;
    public static int checksize = 20;
    private static HashMap<Integer, JButton> buttonList;

    private static void createUIComponents() {
        game = new JPanel();

        CreateField();
        CreateScores();
        CreateGameList();
        CreateSpecs(Model.config);
        CreateButtons();

    }
    public static void init() {
        //JFrame window = new JFrame("snake");
        rightSide = new JPanel();
        rightSide.setLayout(new BorderLayout());
        tables= new JPanel();
        createUIComponents();
        rightSide.add(tables, BorderLayout.NORTH);
        game.add(rightSide);
        //window.addKeyListener(new WASDKeyListener());
        window.setLayout(new BorderLayout());
        window.setContentPane(game);
        setKeyBindings();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //window.setSize(ox, oy);
        window.pack();
        window.setVisible(true);
    }

    public static void main(String[] args) {
        init();
    }


//    public static void Init() { //creates buttons, etc
//        CreateField();
//        CreateGameList();
//        CreateScores();
//        CreateSpecs(Model.config);
//        CreateButtons();
//    }
    public static void repaint(SnakesProto.GameState state){
        RepaintField(state.getSnakesList());
        //RepaintGameList(); //another method
        RepaintScores(state.getPlayers().getPlayersList());
        RepaintSpecs(state.getConfig());
        RepaintButtons();
    }
    public static void error(String message){
        JFrame f = new JFrame("Error!");
        JPanel panel = new JPanel();
        JTextField textField = new JTextField();
        textField.setBackground(Color.WHITE);
        textField.setColumns(14); //ширина поля
        textField.setText(message);
        panel.add(textField);
        f.getContentPane().add(panel);
        f.setSize(400,400);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
    private static void CreateField() { //adds field to window
        gameField = new JPanel();
        //TODO: change 5 to ox, oy
        gameField.setLayout(new GridLayout(5, 5));
        buttonList = new HashMap<>();
        for (int i = 0; i < 5*5; i++){
            buttonList.put(i, new JButton());
            JButton b = buttonList.get(i);
            //System.out.println(b.hashCode());
            b.setBackground(Color.BLACK);
            b.setPreferredSize(new Dimension(checksize,checksize));
            gameField.add(b);
        }
        gameField.setSize(5, 5);
        game.add(gameField);
    }
    private static void CreateGameList() { //jtable with buttons
        gameList = new JPanel();

        String[] columnNames = {"Name", "Players", "Size", "Food"};
        String[][] data = {};
        int size = 14;
        JToolBar toolbar = new JToolBar(SwingConstants.VERTICAL);
        for (int i = 0; i < size; i++){
            JButton b = new JButton("name " + "connect");
            b.addActionListener(new ConnectListener());
            b.setPreferredSize(new Dimension(checksize, 10));
            toolbar.add(b);
        }
        JTable gamelistinfo = new JTable(data, columnNames);
        gamelistinfo.setRowHeight(20);
        JScrollPane scrollPane = new JScrollPane(gamelistinfo);
        scrollPane.setPreferredSize(new Dimension((int)(ox*checksize*0.4), (int)(oy*checksize*0.5)));
        toolbar.setPreferredSize(new Dimension((int)(ox*checksize*0.4), (int)(oy*checksize*0.5)));
        gameList.add(scrollPane, BorderLayout.CENTER);
        gameList.add(toolbar);
        rightSide.add(gameList, BorderLayout.SOUTH);
    }
    private static void CreateSpecs(SnakesProto.GameConfig config) {
        String[] columnNames = {"Type", "Value"};
        String[][] data = {
                {"Width", "", ""},
                {"Height", "", ""},
                {"food_static", "", ""},
                {"food_per_player", "", ""},
                {"state_delay_ms", "", ""},
                {"dead_food_prob", "", ""},
                {"ping_delay_ms", "", ""},
                {"node_timeout_ms", "", ""},
        };
        String s = "";
        if (config.hasWidth()) data[0][2] = Integer.toString(config.getWidth());
        if (config.hasHeight()) data[0][2] = Integer.toString(config.getHeight());
        if (config.hasFoodStatic()) data[0][2] = Integer.toString(config.getFoodStatic());
        if (config.hasFoodPerPlayer()) data[0][2] = Float.toString(config.getFoodPerPlayer());
        if (config.hasStateDelayMs()) data[0][2] = Integer.toString(config.getStateDelayMs());
        if (config.hasDeadFoodProb()) data[0][2] = Float.toString(config.getDeadFoodProb());
        if (config.hasPingDelayMs()) data[0][2] = Integer.toString(config.getPingDelayMs());
        if (config.hasNodeTimeoutMs()) data[0][2] = Integer.toString(config.getNodeTimeoutMs());
        gameSpecs = new JTable(data, columnNames);
        JScrollPane scrollPane = new JScrollPane(gameSpecs);
        scrollPane.setPreferredSize(new Dimension((int)(ox*checksize*0.4), (int)(oy*checksize*0.4)));
        tables.add(scrollPane, BorderLayout.LINE_END);
    }
    private static void CreateScores() {
        String[] columnNames = {"Position", "Name", "Score", "IsMe"};
        String[][] data = {};
        scores = new JTable(data, columnNames);
        JScrollPane scrollPane = new JScrollPane(scores);
        scrollPane.setPreferredSize(new Dimension((int)(ox*checksize*0.5), (int)(oy*checksize*0.4)));
        tables.add(scrollPane, BorderLayout.BEFORE_LINE_BEGINS);
    }
    private static void CreateButtons() {
        buttons = new JPanel();
        exit = new JButton("exit");
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Controller.exit();
            }
        });
        newGame = new JButton("new game");
        newGame.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Controller.newgame();
                System.out.println("new");
            }
        });
        buttons.add(exit, BorderLayout.AFTER_LINE_ENDS);
        buttons.add(newGame, BorderLayout.BEFORE_LINE_BEGINS);
        buttons.setPreferredSize(new Dimension((int)(ox*checksize), (int)(oy*checksize*0.1)));
        rightSide.add(buttons, BorderLayout.CENTER);
    }

    private static void RepaintField(List<SnakesProto.GameState.Snake> snakesList) { //repaints field to window
        Iterator<SnakesProto.GameState.Snake> iter = snakesList.iterator();
        while (iter.hasNext()) {
            List<SnakesProto.GameState.Coord> snakeBody = iter.next().getPointsList();
            int i = 0;
            Iterator<SnakesProto.GameState.Coord> it = snakeBody.iterator();
            SnakesProto.GameState.Coord prev = snakeBody.get(0);
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
                            //used_checks.put(tmp.build(), iter.next().getPlayerId());
                            repaintCheck(tmp.build(), iter.next().getPlayerId());
                            if (it.next().getY() == (j + step) % Model.config.getWidth()){
                                tmp.setX(it.next().getX());
                                tmp.setY(it.next().getY());
                                tmp.build();
                                //tail = tmp.build();
                                //used_checks.put(tmp.build(), iter.next().getPlayerId());
                                repaintCheck(tmp.build(), iter.next().getPlayerId());
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
                            //used_checks.put(tmp.build(), iter.next().getPlayerId());
                            repaintCheck(tmp.build(), iter.next().getPlayerId());
                            if (it.next().getY() == (j + step) % Model.config.getHeight()){
                                tmp.setX(it.next().getX());
                                tmp.setY(it.next().getY());
                                tmp.build();
                                //tail = tmp.build();
                                //used_checks.put(tmp.build(), iter.next().getPlayerId());
                                repaintCheck(tmp.build(), iter.next().getPlayerId());
                            }
                        }
                    }
                    prev = it.next();
                }
                i++;
            }
        }
    }
    private static void repaintCheck(SnakesProto.GameState.Coord coord, int playerId){
        int butNum = coord.getY() * Model.config.getWidth() + coord.getX();
        if (playerId == Controller.playerId) buttonList.get(butNum).setBackground(Color.YELLOW);
        else buttonList.get(butNum).setBackground(Color.RED);
    }
    public static void repaintGameList(ConcurrentHashMap<String, LocalTime> table) {

    }
    private static void RepaintSpecs(SnakesProto.GameConfig config) {
        String[] columnNames = {"Type", "Value"};
        String[][] data = {
                {"Width", "", ""},
                {"Height", "", ""},
                {"food_static", "", ""},
                {"food_per_player", "", ""},
                {"state_delay_ms", "", ""},
                {"dead_food_prob", "", ""},
                {"ping_delay_ms", "", ""},
                {"node_timeout_ms", "", ""},
        };
        String s = "";
        if (config.hasWidth()) data[0][2] = Integer.toString(config.getWidth());
        if (config.hasHeight()) data[0][2] = Integer.toString(config.getHeight());
        if (config.hasFoodStatic()) data[0][2] = Integer.toString(config.getFoodStatic());
        if (config.hasFoodPerPlayer()) data[0][2] = Float.toString(config.getFoodPerPlayer());
        if (config.hasStateDelayMs()) data[0][2] = Integer.toString(config.getStateDelayMs());
        if (config.hasDeadFoodProb()) data[0][2] = Float.toString(config.getDeadFoodProb());
        if (config.hasPingDelayMs()) data[0][2] = Integer.toString(config.getPingDelayMs());
        if (config.hasNodeTimeoutMs()) data[0][2] = Integer.toString(config.getNodeTimeoutMs());
        DefaultTableModel model = new DefaultTableModel(data,columnNames);
        scores.setModel(model);
        model.fireTableDataChanged();
    }
    private static void RepaintScores(java.util.List<SnakesProto.GamePlayer> players) {
        String[] columnNames = {"Position", "Name", "Score", "IsMe"};
        String[][] data = new String[players.size() + 1][5];
        Iterator<SnakesProto.GamePlayer> playerIterator = players.iterator();
        int i = 0;
        while (playerIterator.hasNext()){
            data[i][0] = Integer.toString(i+1);
            data[i][1] = playerIterator.next().getName();
            data[i][2] = Integer.toString(playerIterator.next().getScore());
            if (playerIterator.next().getId() == Controller.playerId) data[i][3] = "yes";
            else data[i][3] = "no";
        }
        DefaultTableModel model = new DefaultTableModel(data,columnNames); // for example
        scores.setModel(model);
        model.fireTableDataChanged();
    }
    private static void RepaintButtons() {

    }
    private static void setKeyBindings() {
        InputMap inputMap =
                game.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke("W"), "up arrow");
        inputMap.put(KeyStroke.getKeyStroke("S"), "down arrow");
        inputMap.put(KeyStroke.getKeyStroke("A"), "left arrow");
        inputMap.put(KeyStroke.getKeyStroke("D"), "right arrow");

        inputMap.put(KeyStroke.getKeyStroke("UP"), "up arrow");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "down arrow");
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "left arrow");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "right arrow");

        inputMap = game.getInputMap(JPanel.WHEN_FOCUSED);
        inputMap.put(KeyStroke.getKeyStroke("UP"), "up arrow");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "down arrow");
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "left arrow");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "right arrow");


        game.getActionMap().put("up arrow",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        //Controller.steer(SnakesProto.Direction.UP);
                        System.out.println("UP");
                        buttonList.get(1).setBackground(Color.RED);

                    }
                });
        game.getActionMap().put("down arrow",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        //Controller.steer(SnakesProto.Direction.DOWN);
                    }
                });
        game.getActionMap().put("left arrow",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        //Controller.steer(SnakesProto.Direction.LEFT);
                    }
                });
        game.getActionMap().put("right arrow",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        //Controller.steer(SnakesProto.Direction.RIGHT);
                    }
                });
    }
}
