import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by evgeniyh on 1/8/18.
 */

public class QueriesManager {
    public static String GET_ALL_TABLE_NAMES = "SELECT table_name FROM information_schema.tables WHERE table_schema='public'";

    private static String UPDATE_ACTIVE_TEMPLATE = "UPDATE %s SET active=FALSE WHERE c_key=? AND s_id=?;";
    private static String QUERY_IF_ACTIVE_TEMPLATE = "SELECT active From %s WHERE c_key=? AND s_id=?;";

    private static String INSERT_INTO_ACTIVE_TEMPLATE = "INSERT INTO %s VALUES (?,?,TRUE);";
    private static String INSERT_INTO_MESSAGING = "INSERT INTO %s VALUES (?,?,?,?,?,?);";

    public static String getCreateSessionsTable(String tableName) {
        return "CREATE TABLE " + tableName + "(" +
                "c_key  TEXT     NOT NULL ," +
                "s_id   INTEGER  NOT NULL ," +
                "active BOOLEAN  NOT NULL ," +
                "PRIMARY KEY(c_key, s_id) " +
                ");";
    }

    public static String getCreateMessagingTable(String tableName) {
        return "CREATE TABLE " + tableName + "(" +
                "c_key      TEXT     NOT NULL ," +
                "s_id       INTEGER  NOT NULL ," +
                "source     TEXT     NOT NULL ," +
                "target     TEXT     NOT NULL ," +
                "time       BIGINT   NOT NULL ," +
                "data       TEXT     NOT NULL ," +
                "PRIMARY    KEY(c_key, s_id, time) " +
                ");";
    }

    public static PreparedStatement getInsertIntoMessagingTable(Connection connection, String tableName, String cKey, int sid, String from, String to, long time, String data) {
        try {
            String statement = String.format(INSERT_INTO_MESSAGING, tableName);
            PreparedStatement p = connection.prepareStatement(statement);

            p.setString(1, cKey);
            p.setInt(2, sid);
            p.setString(3, from);
            p.setString(4, to);
            p.setLong(5, time);
            p.setString(6, data);

            return p;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PreparedStatement getInsertNewActiveCollaborationStatment(Connection connection, String tableName, String cKey, int sid) {
        try {
            String statement = String.format(INSERT_INTO_ACTIVE_TEMPLATE, tableName);
            PreparedStatement p = connection.prepareStatement(statement);

            p.setString(1, cKey);
            p.setInt(2, sid);

            return p;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PreparedStatement getIsCollaborationActive(Connection connection, String tableName, String cKey, int sid) {
        try {
            String statement = String.format(QUERY_IF_ACTIVE_TEMPLATE, tableName);
            PreparedStatement p = connection.prepareStatement(statement);

            p.setString(1, cKey);
            p.setInt(2, sid);

            return p;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PreparedStatement getArchiveCollaborationStatement(Connection connection, String tableName, String cKey, int sid) {
        try {
            String statement = String.format(UPDATE_ACTIVE_TEMPLATE, tableName);
            PreparedStatement p = connection.prepareStatement(statement);

            p.setString(1, cKey);
            p.setInt(2, sid);

            return p;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
