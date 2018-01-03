import com.google.gson.JsonObject;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by evgeniyh on 12/28/17.
 */

public class Collaboration {
    private int count = 0;
    private final String key;
    private final Object syncObject = new Object();
    private final LinkedList<MessageData> messages = new LinkedList<>();

    public Collaboration(String user1, String user2) {
        this.key = Messenger.createCollaborationKey(user1, user2);
    }

    public void addNewMessage(MessageData messageData) {
        synchronized (syncObject) {
            messages.addFirst(messageData);
        }
    }

    public List<MessageData> getAllMessagesFrom(String user) {
        synchronized (syncObject) {
            return messages.stream().filter((m) -> m.getFrom().equals(user)).collect(Collectors.toList());
        }
    }

    public MessageData getLastMessageFrom(String user) {
        synchronized (syncObject) {
            return messages.stream().filter((m) -> m.getFrom().equals(user)).findFirst().orElse(null);
        }
    }


    public int getCount() {
        return count;
    }

    public void startNew() {
        messages.clear();
        count++;
    }
}
