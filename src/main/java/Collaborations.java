import javax.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Created by evgeniyh on 12/28/17.
 */

public class Collaborations {
//    private final String key;
    private final Map<Integer, Session> sessions = new HashMap<>();

    public Collaborations() {
//        this.key = Common.createCollaborationKey(user1, user2);
    }

    public boolean isSessionExists(int sessionId) {
        return sessions.containsKey(sessionId);
    }

    public void addNewMessage(int sid, MessageData messageData) {
        Session s = sessions.get(sid);
        if (s == null) {
            throw new IllegalArgumentException(String.format("Collaboration with id %d doesn't exists", sid));
        }
        if (!s.isActive()) {
            throw new NotAuthorizedException(String.format("Can't add collaboration with id %d has already been archived", sid));
        }
        s.addNewMessage(messageData);
    }

    public List<MessageData> getAllMessagesFrom(int sid, String user) {
        Session s = sessions.get(sid);
        if (s == null) {
            throw new IllegalArgumentException(String.format("Collaboration with id %d doesn't exists", sid));
        }
        return s.getAllMessagesFrom(user);
    }

    public MessageData getLastMessageFrom(int sid, String user) {
        Session s = sessions.get(sid);
        if (s == null) {
            throw new IllegalArgumentException(String.format("Collaboration with id %d doesn't exists", sid));
        }
        return s.getLastMessageFrom(user);
    }


    public int createNewSessionId() {
        int randomNum = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        while (sessions.containsKey(randomNum)) {
            randomNum = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        }
        return randomNum;
    }

    public void archive(int sid) {
        Session s = sessions.get(sid);
        if (s == null) { // Started session without messages and archived
            s = new Session();
            sessions.put(sid, s);
        }
        if (!s.isActive()) {
            throw new IllegalStateException(String.format("Can't archive - %d already been archived", sid));
        }
        s.archive();
    }

    public void startNewSession(int sid) {
        if (sessions.containsKey(sid)) {
            throw new RuntimeException(String.format("Session with id '%d' already exists", sid));
        }
        sessions.put(sid, new Session());
    }

    public boolean isActive(int sid) {
        Session s = sessions.get(sid);
        return s.isActive();
    }
}
