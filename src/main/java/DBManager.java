import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
    private final String messagingTableName;
    private final String activeTableName;

    public DBManager(String messagingTableName, String activeTableName) {
        if (Common.isNullOrEmpty(messagingTableName) || Common.isNullOrEmpty(activeTableName)) {
            throw new NullPointerException("Tables can't be null or empty");
        }
        this.messagingTableName = messagingTableName;
        this.activeTableName = activeTableName;

        try {
            Class.forName("org.postgresql.Driver"); // Check that the driver is ok

            String user = System.getenv("POSTGRES_USERNAME");
            String password = System.getenv("POSTGRES_PASSWORD");
            String url = System.getenv("POSTGRES_URL");

            if (user == null || password == null || url == null || user.isEmpty() || password.isEmpty() || url.isEmpty()) {
                throw new Exception("Credential values can't be null or empty");
            }

            connection = DriverManager.getConnection(urlTemplate + url, user, password);
        } catch (Exception e) {
            connection = null;
            logger.error("Failed to connect to the db", e);
        }
    }


    String executeQuery(String query) throws SQLException {
        logger.debug("Executing query " + query);

        String result = null;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            logger.debug("Query executed successfully ");
            if (!rs.isBeforeFirst()) {
                logger.info("Query result was empty !!!");
            } else {
                result = resultSetToString(rs);
                logger.debug("The result of the query is :\n" + result);
            }
        }
        return result;
    }

    private String resultSetToString(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();

        ResultSetMetaData rsmd = rs.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        sb.append(rsmd.getColumnName(1));
        for (int i = 2; i <= columnsNumber; i++) {
            sb.append(", ").append(rsmd.getColumnName(i));
        }
        sb.append("\n");
        while (rs.next()) {
            sb.append(rs.getString(1));

            for (int i = 2; i <= columnsNumber; i++) {
                sb.append(", ").append(rs.getString(i));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

//    TODO: maybe add check for connection is valid

    void addNewMessage(MessageData m) throws SQLException {
        PreparedStatement statement = QueriesManager.getInsertIntoMessagingTable(
                connection, messagingTableName, m.getKey(), m.getCid(), m.getSource(), m.getTarget(), m.getTimestamp(), m.getData());
        executeUpdate(statement);
    }


    void addNewCollaboration(String cKey, int cid) throws SQLException {
        PreparedStatement statement = QueriesManager.getInsertNewActiveCollaborationStatment(connection, activeTableName, cKey, cid);
        executeUpdate(statement);
    }

    void archiveCollaboration(String cKey, int cid) throws SQLException {
        PreparedStatement statement = QueriesManager.getArchiveCollaborationStatement(connection, activeTableName, cKey, cid);
        executeUpdate(statement);
    }

    public void executeUpdateStatement(String statement) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(statement);
        executeUpdate(ps);
    }

    private void executeUpdate(PreparedStatement statement) throws SQLException {
        if (statement == null) {
            throw new NullPointerException("Failed to create statement");
        }
        int affectedRows = statement.executeUpdate();
        logger.info(String.format("The update statement has affected %d lines", affectedRows));
    }

    List<MessageData> getAllMessages(String user1, String user2, int cid) {

        return null;
    }

    boolean isCollaborationActive(String cKey, int cid) throws SQLException {
        PreparedStatement statement = QueriesManager.getIsCollaborationActive(connection, activeTableName, cKey, cid);
        if (statement == null) {
            throw new NullPointerException("Failed to create statement");
        }
        try (ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getBoolean(1);
        }
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
