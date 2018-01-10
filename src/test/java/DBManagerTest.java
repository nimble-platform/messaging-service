import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;

public class DBManagerTest {
    private static String messagingTableName = "test_messaging";
    private static String activeTableName = "test_active";

    private static DBManager dbManager = new DBManager(messagingTableName, activeTableName);

    @BeforeClass
    public static void createTables() throws SQLException {
        System.out.println("Creating tables");
        runUpdateStatement(dbManager, QueriesManager.getCreateMessagingTable("test_messaging"));
        runUpdateStatement(dbManager, QueriesManager.getCreateActiveTable("test_active"));
    }

    @Test
    public void addSingleRowTest() {
        try {
            String key = Common.createCollaborationKey("moshe", "david");
            dbManager.addNewMessage(new MessageData(123123123, 1, "moshe", "david", key, "hello"));
            dbManager.addNewCollaboration(key, 1);


            System.out.println(dbManager.executeQuery("SELECT * FROM " + messagingTableName));
            System.out.println(dbManager.executeQuery("SELECT * FROM " + activeTableName));

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