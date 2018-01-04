import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by evgeniyh on 12/28/17.
 */

public class Collaborations {
    private int count = -1;
    private final String key;
    private final ArrayList<Session> sessions = new ArrayList<>();

    public Collaborations(String user1, String user2) {
        this.key = Messenger.createCollaborationKey(user1, user2);
    }

    public void addNewMessage(int cid, MessageData messageData) throws IllegalAccessException {
        Session s = sessions.get(cid);
        if (s == null) {
            throw new IllegalArgumentException(String.format("Collaboration with id %d doesn't exists", cid));
        }
        if (!s.isActive()) {
            throw new IllegalAccessException(String.format("Can't add collaboration with id %d has already been archived", cid));
        }
        s.addNewMessage(messageData);
    }

    public List<MessageData> getAllMessagesFrom(int cid, String user) {
        Session s = sessions.get(cid);
        if (s == null) {
            throw new IllegalArgumentException(String.format("Collaboration with id %d doesn't exists", cid));
        }
        return s.getAllMessagesFrom(user);
    }

    public MessageData getLastMessageFrom(int cid, String user) {
        Session s = sessions.get(cid);
        if (s == null) {
            throw new IllegalArgumentException(String.format("Collaboration with id %d doesn't exists", cid));
        }
        return s.getLastMessageFrom(user);
    }


    public int getCount() {
        return count;
    }

    public void archive(int cid) {
        Session s = sessions.get(cid);
        if (!s.isActive()) {
            throw new IllegalStateException(String.format("Can't archive - %d already been archived", cid));
        }
        s.archive();
    }

    public void startNew() {
        count++;
        sessions.add(count, new Session());
    }

    public boolean isActive(int cid) {
        Session s = sessions.get(cid);
        return s.isActive();
    }

    private class Session {
        private final LinkedList<MessageData> messages = new LinkedList<>();
        private final Object syncObject = new Object();
        private boolean active = true;

        void addNewMessage(MessageData messageData) {
            synchronized (syncObject) {
                messages.addFirst(messageData);
            }
        }

        List<MessageData> getAllMessagesFrom(String user) {
            synchronized (syncObject) {
                return messages.stream().filter((m) -> m.getFrom().equals(user)).collect(Collectors.toList());
            }
        }

        MessageData getLastMessageFrom(String user) {
            synchronized (syncObject) {
                return messages.stream().filter((m) -> m.getFrom().equals(user)).findFirst().orElse(null);
            }
        }

        void archive() {
            active = false;
        }

        boolean isActive() {
            return active;
        }
    }
}
