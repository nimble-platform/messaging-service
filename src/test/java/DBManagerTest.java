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
    private static final String simpleData = "this is simple data test";


    private static final String cKey = Common.createCollaborationKey(user1, user2);

    private static DBManager dbManager = new DBManager(messagingTableName, activeTableName);

    @BeforeClass
    public static void createTables() throws SQLException {
        System.out.println("Creating tables");
        runUpdateStatement(dbManager, QueriesManager.getCreateMessagingTable(messagingTableName));
        runUpdateStatement(dbManager, QueriesManager.getCreateActiveTable(activeTableName));
    }

    @Test
    public void addSingleRowNoExceptionsTest() {
        try {
            int randSid = getRansomNumber();

            dbManager.addNewMessage(new MessageData((int) (timestamp * Math.random()), randSid, user1, user2, cKey, simpleData));
            dbManager.addNewCollaboration(cKey, randSid);

//            System.out.println(dbManager.executeQuery("SELECT * FROM " + messagingTableName));
//            System.out.println(dbManager.executeQuery("SELECT * FROM " + activeTableName));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testLoadingSimpleExistingCollaborations() {
        try {
            int randSid = getRansomNumber();
//            startFresshMessagingTable();

            dbManager.addNewMessage(new MessageData((int) (timestamp * Math.random()), randSid, user1, user2, cKey, simpleData));
            dbManager.addNewCollaboration(cKey, randSid);

            Map<String, Collaborations> keysToCollaborations = dbManager.loadCollaborations();
            Collaborations c = keysToCollaborations.get(cKey);
            Assert.assertNotNull(c);
            Assert.assertTrue("Collaboration should be active", c.isActive(randSid));
            MessageData m = c.getLastMessageFrom(randSid, user1);
            Assert.assertNotNull(m);
            Assert.assertEquals(m.getData(), simpleData);
            Assert.assertEquals(1, c.getAllMessagesFrom(randSid, user1).size());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    private int getRansomNumber() {
        return (int) (100000 * Math.random());
    }

    private static void startFresshMessagingTable() throws SQLException {
        runUpdateStatement(dbManager, "DROP TABLE " + messagingTableName);
        runUpdateStatement(dbManager, QueriesManager.getCreateMessagingTable(messagingTableName));
    }

    @Test
    public void testLoadingComplexCollaborations() {
        try {
            int randomStart = getRansomNumber();
//            startFresshMessagingTable();

            dbManager.addNewMessage(new MessageData(timestamp + 1, randomStart, user1, user2, cKey, simpleData + String.valueOf(0)));
            dbManager.addNewMessage(new MessageData(timestamp + 2, randomStart, user1, user2, cKey, simpleData + String.valueOf(1)));
            dbManager.addNewMessage(new MessageData(timestamp + 3, randomStart, user1, user2, cKey, simpleData + String.valueOf(2)));
            dbManager.addNewMessage(new MessageData(timestamp + 4, randomStart, user2, user1, cKey, simpleData + String.valueOf(3)));
            dbManager.addNewCollaboration(cKey, randomStart);
//            Map<String, Collaborations> keysToCollaborations1 = dbManager.loadCollaborations();

            for (int i = 1; i < 10; i++) {
                dbManager.addNewMessage(new MessageData(timestamp, randomStart+i, user1, user2, cKey, simpleData + String.valueOf(i)));
                dbManager.addNewCollaboration(cKey, randomStart+i);
                if (i % 2 == 0) {
                    dbManager.archiveCollaboration(cKey, randomStart+i);
                }
            }

            Map<String, Collaborations> keysToCollaborations = dbManager.loadCollaborations();

            Collaborations c = keysToCollaborations.get(cKey);
            Assert.assertNotNull(c);

            for (int i = 1; i < 10; i++) {
                if (i % 2 == 0) {
                    Assert.assertFalse("Collaboration should be archived", c.isActive(randomStart+i));
                } else {
                    Assert.assertTrue("Collaboration should be active", c.isActive(randomStart+i));
                }
                MessageData m = c.getLastMessageFrom(randomStart+i, user1);
                Assert.assertNotNull(m);
                Assert.assertEquals(m.getData(), simpleData + String.valueOf(i));
                Assert.assertEquals(1, c.getAllMessagesFrom(randomStart+i, user1).size());
            }

            Assert.assertTrue("Collaboration should be active", c.isActive(randomStart));

            MessageData m = c.getLastMessageFrom(randomStart, user2);
            Assert.assertNotNull(m);
            Assert.assertEquals(m.getData(), simpleData + String.valueOf(3));
            Assert.assertEquals(1, c.getAllMessagesFrom(randomStart, user2).size());

            m = c.getLastMessageFrom(randomStart, user1);
            Assert.assertNotNull(m);
            Assert.assertEquals(m.getData(), simpleData + String.valueOf(2));
            Assert.assertEquals(3, c.getAllMessagesFrom(randomStart, user1).size());
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