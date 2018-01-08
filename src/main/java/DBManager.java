import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Created by evgeniyh on 1/4/18.
 */

public class DBManager {
    private final static Logger logger = Logger.getLogger(DBManager.class);
    private Connection connection;
    private final String urlTemplate = "jdbc:postgresql://";

    public DBManager() {
        try {
            Class.forName("org.postgresql.Driver"); // Check that the driver is ok

            String user = System.getenv("POSTGRES_USERNAME");
            String password = System.getenv("POSTGRES_PASSWORD");
            String url = System.getenv("POSTGRES_URL");

            if (user == null || password == null || url == null || user.isEmpty() || password.isEmpty() || url.isEmpty()) {
                throw new Exception("Credential values can't be null or empty");
            }

            connection = DriverManager.getConnection(urlTemplate + url, user, password);
            String query = "SELECT table_name FROM information_schema.tables WHERE table_schema='public'";
//            String query = "SELECT table_name FROM information_schema.tables WHERE table_schema='public'";
//            String query = "CREATE TABLE TEST_TABLE(\n" +
//                    "   ID INT PRIMARY KEY     NOT NULL,\n" +
//                    "   MESSAGE        TEXT    NOT NULL);";
            logger.debug("Executing query " + query);
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(query)) {
                logger.debug("Query executed successfully ");
                if (!rs.isBeforeFirst()) {
                    logger.info("EMPTY !!!");
                } else {
                    ResultSetMetaData rsmd = rs.getMetaData();
                    int columnsNumber = rsmd.getColumnCount();
                    System.out.print(rsmd.getColumnName(1));
                    for (int i = 2; i < columnsNumber; i++) {
                        System.out.print(", " + rsmd.getColumnName(i));
                    }
                    System.out.println("");
                    while (rs.next()) {
                        System.out.print(rs.getString(1));

                        for (int i = 2; i <= columnsNumber; i++) {
                            System.out.print(", " + rs.getString(i));
                        }
                        System.out.println("");
                    }
//                JsonArray table = resultSetToJsonArray(rs);
//                logger.info(table.toString().replace("{", "\n{"));
                }
            }
        } catch (Exception e) {
            connection = null;
            logger.error("Failed to connect to the db", e);
        }
    }

    boolean addNewMessage(MessageData m) throws SQLException {

        return false;


    }

    List<MessageData> getAllMessages(String user1, String user2, int cid) {

        return null;
    }

    boolean isCollaborationActive(String user1, String user2, int cid) {

        return false;
    }

    private JsonArray resultSetToJsonArray(ResultSet resultSet) throws Exception {
        JsonArray jsonArray = new JsonArray();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnsCount = metaData.getColumnCount();

        String[] columns = new String[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            columns[i] = metaData.getColumnLabel(i + 1);
        }
        JsonObject obj;
        while (resultSet.next()) {
            obj = new JsonObject();
            for (int i = 0; i < columnsCount; i++) {
                obj.addProperty(columns[i], resultSet.getObject(i + 1).toString());
            }
            jsonArray.add(obj);
        }
        return jsonArray;
    }
}
