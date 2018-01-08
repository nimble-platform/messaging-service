import org.junit.Test;

import static org.junit.Assert.*;

public class DBManagerTest {
    @Test
    public void connectionTest() {
        try {
            DBManager dbManager = new DBManager();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}