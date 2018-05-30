import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by evgeniyh on 1/4/18.
 */

public class DBManager {
    private final static Logger logger = Logger.getLogger(DBManager.class);

    private String user;
    private String password;
    private String connectionUrl;

    private Connection connection;

    private final String messagingTableName;
    private final String sessionsTableName;

    private final int KEY_INDEX = 1;
    private final int SESSION_ID_INDEX = 2;
    private final int SOURCE_INDEX = 3;
    private final int TARGET_INDEX = 4;
    private final int TIME_INDEX = 5;
    private final int DATA_INDEX = 6;

    private final int ACTIVE_INDEX = 3;

    public DBManager(String messagingTableName, String sessionsTableName, boolean verifyTables) {
        if (Common.isNullOrEmpty(messagingTableName) || Common.isNullOrEmpty(sessionsTableName)) {
            throw new NullPointerException("Tables can't be null or empty");
        }
        this.messagingTableName = messagingTableName;
        this.sessionsTableName = sessionsTableName;

        try {
            Class.forName("org.postgresql.Driver"); // Check that the driver is ok

            user = System.getenv("POSTGRES_USERNAME");
            password = System.getenv("POSTGRES_PASSWORD");
            String url = System.getenv("POSTGRES_URL");

            connectionUrl = "jdbc:postgresql://" + url;
            if (Common.isNullOrEmpty(user) || Common.isNullOrEmpty(password) || Common.isNullOrEmpty(url)) {
                throw new Exception("Credential values can't be null or empty");
            }

            connection = DriverManager.getConnection(connectionUrl, user, password);

            if (!verifyTables) {
                return;
            }
            DatabaseMetaData dbm = connection.getMetaData();

            createTableIfMissing(dbm, messagingTableName, QueriesManager.getCreateMessagingTable(messagingTableName));
            createTableIfMissing(dbm, sessionsTableName, QueriesManager.getCreateSessionsTable(sessionsTableName));
        } catch (Exception e) {
            connection = null;
            logger.error("Failed to connect to the db", e);
        }
    }

    private void createTableIfMissing(DatabaseMetaData dbm, String tableName, String queryOnError) throws SQLException {
        logger.info("Verifying table with name - " + tableName + " exists");
        ResultSet tables = dbm.getTables(null, null, tableName, null);
        if (tables.next()) {
            logger.info("SUCCESS !!! The table - " + tableName + " exists");
        } else {
            logger.error("ERROR !!! The table - " + tableName + " doesn't exists - creating it now");
            executeUpdateStatement(queryOnError);
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

    boolean reconnect() {
        try {
            connection = DriverManager.getConnection(connectionUrl, user, password);
            return isConnected();
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
        PreparedStatement statement = QueriesManager.getInsertNewActiveCollaborationStatment(connection, sessionsTableName, cKey, sid);
        executeUpdate(statement);
    }

    void archiveCollaboration(String cKey, int sid) throws SQLException {
        PreparedStatement statement = QueriesManager.getArchiveCollaborationStatement(connection, sessionsTableName, cKey, sid);
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

    public List<SessionInfo> getAllSession(String user) throws SQLException {
        String allSessionsQuery = "SELECT * FROM " + sessionsTableName;
        List<SessionInfo> sessions = new LinkedList<>();


        try (Statement st = connection.createStatement();
             ResultSet rs = executeQuery(st, allSessionsQuery)) {

            while (rs.next()) {
                String ckey = rs.getString(KEY_INDEX);
                int sessionId = rs.getInt(SESSION_ID_INDEX);
                boolean isActive = rs.getBoolean(ACTIVE_INDEX);

                String startRegex = String.format("%s[0-9].*|.*[0-9]*%s", user, user);
                if (ckey.matches(startRegex)) {
//                    System.out.println("Matches start - " + startRegex);
                    sessions.add(new SessionInfo(ckey, sessionId, isActive));
                }
            }
        }
        return sessions;
    }

    public List<MessageData> getAllMessages(String user1, String user2, int sid) {

        return null;
    }

    boolean isCollaborationActive(String cKey, int sid) throws SQLException {
        PreparedStatement statement = QueriesManager.getIsCollaborationActive(connection, sessionsTableName, cKey, sid);
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

        String allSessionsQuery = "SELECT * FROM " + sessionsTableName;

        try (Statement st = connection.createStatement();
             ResultSet sessions = executeQuery(st, allSessionsQuery)) {
//            System.out.println(resultSetToString(activeResults));
            while (sessions.next()) {
                String ckey = sessions.getString(KEY_INDEX);
                int sid = sessions.getInt(SESSION_ID_INDEX);
                boolean isSessionActive = sessions.getBoolean(ACTIVE_INDEX);

                Collaborations c = keyToCollaboration.computeIfAbsent(ckey, k -> {
                    logger.error("Read a collaboration with no messages between users with key - " + ckey);
                    Collaborations tmp = new Collaborations();
                    tmp.startNewSession(sid);
                    return tmp;
                });

                if (isSessionActive) {
                    logger.info(String.format("Collaboration key %s and session id %d is still active - continuing", ckey, sid));
                } else {
                    logger.info(String.format("Collaboration key %s and session id %d was archived - Archiving", ckey, sid));
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
