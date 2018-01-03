import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class CollaborationsKeyTest {
    @Test
    public void testSameKey() {
        CollaborationKey c1 = new CollaborationKey("moshe", "david");
        CollaborationKey c2 = new CollaborationKey("david", "moshe");

        Assert.assertEquals(c1.getKey(), c2.getKey());
    }

    @Test
    public void testDifferentKey() {
        CollaborationKey c1 = new CollaborationKey("moshe", "david");
        CollaborationKey c2 = new CollaborationKey("rami", "moshe");

        Assert.assertNotEquals(c1.getKey(), c2.getKey());
    }

    @Test
    public void testSimilarIdsDifferentKey() {
        CollaborationKey c1 = new CollaborationKey("abc", "def");
        CollaborationKey c2 = new CollaborationKey("ab", "cdef");

        Assert.assertNotEquals(c1.getKey(), c2.getKey());

        c1 = new CollaborationKey("abcd", "ef");
        c2 = new CollaborationKey("cdef", "ab");

        Assert.assertNotEquals(c1.getKey(), c2.getKey());
    }
}