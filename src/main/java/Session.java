import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by evgeniyh on 1/13/18.
 */

public class Session {
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
            return messages.stream().filter((m) -> m.getSource().equals(user)).collect(Collectors.toList());
        }
    }

    MessageData getLastMessageFrom(String user) {
        synchronized (syncObject) {
            return messages.stream().filter((m) -> m.getSource().equals(user)).findFirst().orElse(null);
        }
    }

    void archive() {
        active = false;
    }

    boolean isActive() {
        return active;
    }
}
