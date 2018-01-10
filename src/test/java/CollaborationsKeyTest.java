import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class CollaborationsKeyTest {
    @Test
    public void testSameKey() {
        String key1 = Common.createCollaborationKey("moshe", "david");
        String key2 = Common.createCollaborationKey("david", "moshe");

        Assert.assertEquals(key1, key2);
    }

    @Test
    public void testDifferentKey() {
        String key1 =  Common.createCollaborationKey("moshe", "david");
        String key2 =  Common.createCollaborationKey("rami", "moshe");

        Assert.assertNotEquals(key1, key2);
    }

    @Test
    public void testSimilarIdsDifferentKey() {
        String key1 = Common.createCollaborationKey("abc", "def");
        String key2 = Common.createCollaborationKey("ab", "cdef");

        Assert.assertNotEquals(key1, key2);

        key1 = Common.createCollaborationKey("abcd", "ef");
        key2 = Common.createCollaborationKey("cdef", "ab");

        Assert.assertNotEquals(key1, key2);
    }
}