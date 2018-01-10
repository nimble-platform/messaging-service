import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;

public class DBManagerTest {
    private static final String messagingTableName = "test_messaging";
    private static final String activeTableName = "test_active";

    private static final String user1 = "moshe";
    private static final String user2 = "david";
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
            dbManager.addNewMessage(new MessageData(123123123, 1, user1, user2, cKey, "hello"));
            dbManager.addNewCollaboration(cKey, 1);

//            System.out.println(dbManager.executeQuery("SELECT * FROM " + messagingTableName));
//            System.out.println(dbManager.executeQuery("SELECT * FROM " + activeTableName));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void archiveCollaboration() {
        try {
            int cid = (int) (Math.random() * 1000 + 5);

            dbManager.addNewCollaboration(cKey, cid);
//            System.out.println(dbManager.executeQuery("SELECT * FROM " + activeTableName));

            Assert.assertTrue(dbManager.isCollaborationActive(cKey, cid));
            dbManager.archiveCollaboration(cKey, cid);
            Assert.assertFalse(dbManager.isCollaborationActive(cKey, cid));

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