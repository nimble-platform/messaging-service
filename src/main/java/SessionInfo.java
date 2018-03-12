/**
 * Created by evgeniyh on 3/11/18.
 */

public class SessionInfo {
    private final boolean active;
    private final int sid;
    private final String cid;

    public SessionInfo(String cid, int sid, boolean active) {
        this.active = active;
        this.sid = sid;
        this.cid = cid;
    }

    public boolean isActive() {
        return active;
    }

    public int getSid() {
        return sid;
    }

    public String getCid() {
        return cid;
    }

    @Override
    public String toString() {
        return "SessionInfo{" +
                "active=" + active +
                ", sid=" + sid +
                ", cid='" + cid + '\'' +
                '}';
    }
}
