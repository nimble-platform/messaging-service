/**
 * Created by evgeniyh on 1/10/18.
 */

public class Common {
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static String createCollaborationKey(String userId1, String userId2) {
        if (isNullOrEmpty(userId1) || isNullOrEmpty(userId2)) {
            throw new NullPointerException("User ids can't be null or empty");
        }
        if (userId1.equals(userId2)) {
            throw new IllegalArgumentException("Ids can't be the same");
        }

        return (userId1.compareTo(userId2) > 0) ?
                String.format("%s%d%s", userId1, userId1.hashCode(), userId2) :
                String.format("%s%d%s", userId2, userId2.hashCode(), userId1);
    }
}
