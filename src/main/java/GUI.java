import me.ippolitov.fit.snakes.SnakesProto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
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
        CreateSpecs();
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
        window.setSize(ox, oy);
        window.pack();
        window.setVisible(true);
    }

    public static void main(String[] args) {
        init();
    }


    public static void Init() { //creates buttons, etc
        CreateField();
        CreateGameList();
        CreateScores();
        CreateSpecs();
        CreateButtons();
    }
    public static void repaint(SnakesProto.GameState state){
        RepaintField();
        RepaintGameList();
        RepaintScores();
        RepaintSpecs();
        RepaintButtons();
        //show result of addings
    }
    public static void error(String message){

    }
    private static void CreateField() { //adds field to window
        gameField = new JPanel();
        gameField.setLayout(new GridLayout(oy, ox));
        buttonList = new HashMap<>();
        for (int i = 0; i < ox*oy; i++){
            buttonList.put(i, new JButton());
            JButton b = buttonList.get(i);
            //System.out.println(b.hashCode());
            b.setBackground(Color.BLACK);
            b.setPreferredSize(new Dimension(checksize,checksize));
            gameField.add(b);
        }
        gameField.setSize(ox, oy);
        game.add(gameField);
    }
    private static void CreateGameList() { //jtable with buttons
        gameList = new JPanel();

        String[] columnNames = {"Name", "Players", "Size", "Food"};
        String[][] data = {
                {"addins", "02.11.2006 19:15", "Folder", ""},
                {"AppPatch", "03.10.2006 14:10", "Folder", ""},
                {"assembly", "02.11.2006 14:20", "Folder", ""},
                {"Boot", "13.10.2007 10:46", "Folder", ""},
                {"Branding", "13.10.2007 12:10", "Folder", ""},
                {"Cursors", "23.09.2006 16:34", "Folder", ""},
                {"Debug", "07.12.2006 17:45", "Folder", ""},
                {"Fonts", "03.10.2006 14:08", "Folder", ""},
                {"Help", "08.11.2006 18:23", "Folder", ""},
                {"explorer.exe", "18.10.2006 14:13", "File", "2,93MB"},
                {"helppane.exe", "22.08.2006 11:39", "File", "4,58MB"},
                {"twunk.exe", "19.08.2007 10:37", "File", "1,08MB"},
                {"nsreg.exe", "07.08.2007 11:14", "File", "2,10MB"},
                {"avisp.exe", "17.12.2007 16:58", "File", "12,67MB"},
        };
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
    private static void CreateSpecs() {
        String[] columnNames = {"Type", "Value"};
        String[][] data = {
                {"addins", "02.11.2006 19:15", "Folder", ""},
                {"AppPatch", "03.10.2006 14:10", "Folder", ""},
                {"assembly", "02.11.2006 14:20", "Folder", ""},
                {"Boot", "13.10.2007 10:46", "Folder", ""},
                {"Branding", "13.10.2007 12:10", "Folder", ""},
                {"Cursors", "23.09.2006 16:34", "Folder", ""},
                {"Debug", "07.12.2006 17:45", "Folder", ""},
                {"Fonts", "03.10.2006 14:08", "Folder", ""},
                {"Help", "08.11.2006 18:23", "Folder", ""},
                {"explorer.exe", "18.10.2006 14:13", "File", "2,93MB"},
                {"helppane.exe", "22.08.2006 11:39", "File", "4,58MB"},
                {"twunk.exe", "19.08.2007 10:37", "File", "1,08MB"},
                {"nsreg.exe", "07.08.2007 11:14", "File", "2,10MB"},
                {"avisp.exe", "17.12.2007 16:58", "File", "12,67MB"},
        };
        gameSpecs = new JTable(data, columnNames);
        JScrollPane scrollPane = new JScrollPane(gameSpecs);
        scrollPane.setPreferredSize(new Dimension((int)(ox*checksize*0.4), (int)(oy*checksize*0.4)));
        tables.add(scrollPane, BorderLayout.LINE_END);
    }
    private static void CreateScores() {
        String[] columnNames = {"Position", "Name", "Score", "IsMe"};
        String[][] data = {
                {"addins", "02.11.2006 19:15", "Folder", ""},
                {"AppPatch", "03.10.2006 14:10", "Folder", ""},
                {"assembly", "02.11.2006 14:20", "Folder", ""},
                {"Boot", "13.10.2007 10:46", "Folder", ""},
                {"Branding", "13.10.2007 12:10", "Folder", ""},
                {"Cursors", "23.09.2006 16:34", "Folder", ""},
                {"Debug", "07.12.2006 17:45", "Folder", ""},
                {"Fonts", "03.10.2006 14:08", "Folder", ""},
                {"Help", "08.11.2006 18:23", "Folder", ""},
                {"explorer.exe", "18.10.2006 14:13", "File", "2,93MB"},
                {"helppane.exe", "22.08.2006 11:39", "File", "4,58MB"},
                {"twunk.exe", "19.08.2007 10:37", "File", "1,08MB"},
                {"nsreg.exe", "07.08.2007 11:14", "File", "2,10MB"},
                {"avisp.exe", "17.12.2007 16:58", "File", "12,67MB"},
        };
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


    private static void RepaintField() { //repaints field to window

    }
    private static void RepaintGameList() {

    }
    private static void RepaintSpecs() {

    }
    private static void RepaintScores() {

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
                        Controller.steer(SnakesProto.Direction.UP);
                    }
                });
        game.getActionMap().put("down arrow",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Controller.steer(SnakesProto.Direction.DOWN);
                    }
                });
        game.getActionMap().put("left arrow",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Controller.steer(SnakesProto.Direction.LEFT);
                    }
                });
        game.getActionMap().put("right arrow",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Controller.steer(SnakesProto.Direction.RIGHT);
                    }
                });
    }
}
