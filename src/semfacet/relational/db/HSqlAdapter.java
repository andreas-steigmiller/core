package semfacet.relational.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class HSqlAdapter implements Database {
    static Logger LOG = Logger.getLogger(HSqlAdapter.class.getName());
    private Connection conn;

    public HSqlAdapter(String dbFileNamePrefix, String username, String password) throws Exception {
        Class.forName("org.hsqldb.jdbcDriver");
        conn = DriverManager.getConnection("jdbc:hsqldb:" + dbFileNamePrefix, username, password);
    }

    public synchronized ResultSet executeQuery(String expression) throws SQLException {
        Statement st = null;
        ResultSet rs = null;
        st = conn.createStatement();
        rs = st.executeQuery(expression);
        st.close();
        return rs;
    }

    public synchronized void update(String expression) throws SQLException {
        Statement st = null;
        st = conn.createStatement();
        int i = st.executeUpdate(expression);
        if (i == -1) {
            LOG.error("db error : " + expression);
        }
        st.close();
    }

    public void shutdown() {
        try {
            Statement st = conn.createStatement();
            st.execute("SHUTDOWN");
            st.close();
            conn.close();
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
    }
}
