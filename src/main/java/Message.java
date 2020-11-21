import me.ippolitov.fit.snakes.SnakesProto;

import java.time.LocalTime;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Message {
    public SnakesProto.GameMessage gm;
    public LocalTime sendtime; //time of last sending
    public LocalTime origtime; //time of first sending
    public ConcurrentHashMap<Integer, SnakesProto.GamePlayer> branches = new ConcurrentHashMap<>(); //clients where to send
}
