/**
 * Created by evgeniyh on 1/2/18.
 */
public class MessageData {
    private final long timestamp;
    private final int cid;
    private final String from;
    private final String to;
    private final String key;
    private final String data;

    public MessageData(long timestamp, int cid, String from, String to, String key, String data) {
        this.timestamp = timestamp;
        this.cid = cid;
        this.from = from;
        this.to = to;
        this.key = key;
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getKey() {
        return key;
    }

    public String getData() {
        return data;
    }

    public int getCid() {
        return cid;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
