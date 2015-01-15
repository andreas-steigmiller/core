/**
 * This interface is used to add arbitrary relational databases. It contains the most important functionalities currently used by the SemFacet. If needed it can be easily extended.
 * It should be implemented as recommended in adapted design pattern. 
 */

package semfacet.relational.db;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface Database {
    ResultSet executeQuery(String query) throws SQLException;
    void update(String query) throws SQLException;
    void shutdown();
}
