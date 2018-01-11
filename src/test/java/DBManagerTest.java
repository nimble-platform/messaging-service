import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Map;

public class DBManagerTest {
    private static final String messagingTableName = "test_messaging";
    private static final String activeTableName = "test_active";

    private static final String user1 = "moshe";
    private static final String user2 = "david";
    private static final int sid = 1;
    private static final long timestamp = 232131212;
    private static final String simpleData= "this is simple data test";


    private static final String cKey = Common.createCollaborationKey(user1, user2);

    private static DBManager dbManager = new DBManager(messagingTableName, activeTableName);

    @BeforeClass
    public static void createTables() throws SQLException {
        System.out.println("Creating tables");
        runUpdateStatement(dbManager, QueriesManager.getCreateMessagingTable(messagingTableName));
        runUpdateStatement(dbManager, QueriesManager.getCreateActiveTable(activeTableName));
    }

    @Test
    public void addSingleRowTest() {
        try {
            dbManager.addNewMessage(new MessageData(timestamp, sid, user1, user2, cKey, simpleData));
            dbManager.addNewCollaboration(cKey, sid);

//            System.out.println(dbManager.executeQuery("SELECT * FROM " + messagingTableName));
//            System.out.println(dbManager.executeQuery("SELECT * FROM " + activeTableName));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testLoadingSimpleExistingCollaborations (){
        try {
            deleteTables();
            createTables();

            dbManager.addNewMessage(new MessageData(timestamp, sid, user1, user2, cKey, simpleData));
            dbManager.addNewCollaboration(cKey, sid);

            Map<String, Collaborations> keysToCollaborations = dbManager.loadCollaborations();
            Collaborations c = keysToCollaborations.get(cKey);
            Assert.assertNotNull(c);
            Assert.assertTrue("Collaboration should be active", c.isActive(sid));
            MessageData m = c.getLastMessageFrom(sid, user1);
            Assert.assertNotNull(m);
            Assert.assertEquals(m.getData(), simpleData);
            Assert.assertEquals(1, c.getAllMessagesFrom(sid, user1).size());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }


    @Test
    public void archiveCollaboration() {
        try {
            int sid = (int) (Math.random() * 1000 + 5);

            dbManager.addNewCollaboration(cKey, sid);
//            System.out.println(dbManager.executeQuery("SELECT * FROM " + activeTableName));

            Assert.assertTrue(dbManager.isCollaborationActive(cKey, sid));
            dbManager.archiveCollaboration(cKey, sid);
            Assert.assertFalse(dbManager.isCollaborationActive(cKey, sid));

//            System.out.println(dbManager.executeQuery("SELECT * FROM " + activeTableName));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @AfterClass
    public static void deleteTables() throws SQLException {
        System.out.println("Deleting tables");
        runUpdateStatement(dbManager, "DROP TABLE " + messagingTableName);
        runUpdateStatement(dbManager, "DROP TABLE " + activeTableName);
    }

    private static void runUpdateStatement(DBManager dbManager, String sql) throws SQLException {
        dbManager.executeUpdateStatement(sql);
        System.out.println(String.format("The query '%s' was completed successfully", sql));
    }
}