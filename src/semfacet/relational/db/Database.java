package semfacet.relational.db;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface Database {
    ResultSet executeQuery(String query) throws SQLException;
    void update(String query) throws SQLException;
    void shutdown();
}
