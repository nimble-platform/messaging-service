import java.util.Arrays;
import java.util.Objects;

/**
 * Created by evgeniyh on 12/28/17.
 */

public class CollaborationKey {
    private final String key;

    public CollaborationKey(String userId1, String userId2) {
        if (userId1 == null || userId2 == null || userId1.isEmpty() || userId2.isEmpty()) {
            throw new NullPointerException("User ids can't be null or empty");
        }
        if (userId1.equals(userId2)) {
            throw new IllegalArgumentException("Ids can't be the same");
        }

        this.key = (userId1.compareTo(userId2) > 0) ? String.format("%s%d%s", userId1, userId1.hashCode(), userId2) : String.format("%s%d%s", userId2, userId2.hashCode(), userId1);
        System.out.print("The created key is : " + key);
    }

    public String getKey() {
        return key;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CollaborationKey that = (CollaborationKey) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
