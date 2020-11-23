import me.ippolitov.fit.snakes.SnakesProto;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MessageCustom {
    public SnakesProto.GameMessage gm;
    public LocalTime sendtime; //time of last sending
    public LocalTime origtime; //time of first sending
    public List<SnakesProto.GamePlayer> branches = new ArrayList<>(); //clients where to send
}
