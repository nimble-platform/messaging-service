/**
 * Created by evgeniyh on 1/2/18.
 */
public class MessageData {
    private final long timestamp;
    private final int cid;
    private final String source;
    private final String target;
    private final String key;
    private final String data;

    public MessageData(long timestamp, int cid, String source, String target, String key, String data) {
        this.timestamp = timestamp;
        this.cid = cid;
        this.source = source;
        this.target = target;
        this.key = key;
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
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
