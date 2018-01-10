import org.junit.Test;

import java.sql.SQLException;

public class DBManagerTest {
    @Test
    public void connectionTest() {
        String messagingTableName = "test_messaging";
        String activeTableName = "test_active";
        try {
            DBManager dbManager = new DBManager(messagingTableName, activeTableName);
//            createTable(dbManager, QueriesManager.getCreateMessagingTable("test_messaging"));
//            createTable(dbManager, QueriesManager.getCreateActiveTable("test_active"));
//
            String key = Common.createCollaborationKey("moshe", "david");
            dbManager.addNewMessage(new MessageData(123123123, 1, "moshe", "david", key, "hello"));
            dbManager.addNewCollaboration(key, 1);


            System.out.println(dbManager.executeQuery("SELECT * FROM " + messagingTableName));
            System.out.println(dbManager.executeQuery("SELECT * FROM " + activeTableName));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createTable(DBManager dbManager, String sql) throws SQLException {
        if(dbManager.executeUpdateStatement(sql)) {
            System.out.println("Created the table");
        } else {
            System.out.println("Failed to create the table");
        }
    }

}