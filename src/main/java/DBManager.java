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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by evgeniyh on 1/4/18.
 */

public class DBManager {
    private final static Logger logger = Logger.getLogger(DBManager.class);
    private Connection connection;
    private final String urlTemplate = "jdbc:postgresql://";
    private final String messagingTableName;
    private final String activeTableName;

    private final int KEY_INDEX = 1;
    private final int SESSION_ID_INDEX = 2;
    private final int SOURCE_INDEX = 3;
    private final int TARGET_INDEX = 4;
    private final int TIME_INDEX = 5;
    private final int DATA_INDEX = 6;

    private final int ACTIVE_INDEX = 3;

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

    boolean isConnected() {
        try {
            return connection != null && connection.isValid(5);
        } catch (SQLException e) {
            logger.error("Error during validating the connections to the DB", e);
            e.printStackTrace();
            return false;
        }
    }

    void addNewMessage(MessageData m) throws SQLException {
        PreparedStatement statement = QueriesManager.getInsertIntoMessagingTable(
                connection, messagingTableName, m.getKey(), m.getSessionId(), m.getSource(), m.getTarget(), m.getTimestamp(), m.getData());
        executeUpdate(statement);
    }


    void addNewActiveSession(String cKey, int sid) throws SQLException {
        PreparedStatement statement = QueriesManager.getInsertNewActiveCollaborationStatment(connection, activeTableName, cKey, sid);
        executeUpdate(statement);
    }

    void archiveCollaboration(String cKey, int sid) throws SQLException {
        PreparedStatement statement = QueriesManager.getArchiveCollaborationStatement(connection, activeTableName, cKey, sid);
        executeUpdate(statement);
    }

    public void executeUpdateStatement(String statement) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(statement);
        executeUpdate(ps);
    }

    private void executeUpdate(PreparedStatement statement) throws SQLException {
        logger.info("Executing update statement - " + statement);
        if (statement == null) {
            throw new NullPointerException("Failed to create statement");
        }
        int affectedRows = statement.executeUpdate();
        logger.info(String.format("The update statement has affected %d lines", affectedRows));
    }

    List<MessageData> getAllMessages(String user1, String user2, int sid) {

        return null;
    }

    boolean isCollaborationActive(String cKey, int sid) throws SQLException {
        PreparedStatement statement = QueriesManager.getIsCollaborationActive(connection, activeTableName, cKey, sid);
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

    private Map<String, Collaborations> loadMessages() throws SQLException {
        Map<String, Collaborations> keyToCollaboration = new HashMap<>();

        String allRecordsQuery = "SELECT * FROM " + messagingTableName;

        try (Statement st = connection.createStatement();
             ResultSet messagesResults = executeQuery(st, allRecordsQuery)) {

            while (messagesResults.next()) {
                String ckey = messagesResults.getString(KEY_INDEX);
                int sessionId = messagesResults.getInt(SESSION_ID_INDEX);
                String source = messagesResults.getString(SOURCE_INDEX);
                String target = messagesResults.getString(TARGET_INDEX);
                long time = messagesResults.getLong(TIME_INDEX);
                String data = messagesResults.getString(DATA_INDEX);

                Collaborations c = keyToCollaboration.get(ckey);
                if (c == null) {
                    c = new Collaborations();
                    c.startNewSession(sessionId);
                    keyToCollaboration.put(ckey, c);
                }
                if (!c.isSessionExists(sessionId)) {
                    c.startNewSession(sessionId);
                }
                c.addNewMessage(sessionId, new MessageData(time, sessionId, source, target, ckey, data));
            }
        }
        return keyToCollaboration;
    }

    public Map<String, Collaborations> loadCollaborations() throws SQLException {
        Map<String, Collaborations> keyToCollaboration = loadMessages();

        String allActivesQuery = "SELECT * FROM " + activeTableName;

        try (Statement st = connection.createStatement();
             ResultSet activeResults = executeQuery(st, allActivesQuery)) {
//            System.out.println(resultSetToString(activeResults));
            while (activeResults.next()) {
                String ckey = activeResults.getString(KEY_INDEX);
                int sid = activeResults.getInt(SESSION_ID_INDEX);

                if (activeResults.getBoolean(ACTIVE_INDEX)) {
                    logger.info(String.format("Collaboration key %s and id %d is still active - continuing", ckey, sid));
                } else {
                    logger.info(String.format("Collaboration key %s and id %d is archived - Archiving", ckey, sid));
                    Collaborations c = keyToCollaboration.get(ckey);
                    if (c == null) {
                        throw new RuntimeException("Something really bad has happened - collaboration doesn't exists");
                    }
                    c.archive(sid);
                }
            }

            return keyToCollaboration;
        }
    }

    private ResultSet executeQuery(Statement st, String sqlQuery) throws SQLException {
        logger.info("Executing query - " + sqlQuery);
        ResultSet rs = st.executeQuery(sqlQuery);

        if (!rs.isBeforeFirst()) {
            logger.info("Query completed successfully - result was empty !!!");
        } else {

            logger.info("Query completed successfully - returning results ");
        }
        return rs;
    }

}
