/**
 * This is a triple store interface, which have some most common methods among different triple stores. If one would like to add new triple store, then this interface should be implemented.
 * To add a new triple store we use adapter design pattern. 
 */

package semfacet.triplestores;

import java.io.File;
import java.util.Set;


public interface Store {
    void loadData(File file) throws StoreException;
    
    void loadData(String dataPath) throws StoreException;

    void loadOntology(String ontologyPath) throws StoreException;
    
    public void loadAggregateFacts(Set<String> allpredicates);

    ResultSet executeQuery(String query, boolean computeUpperBound);

    // Maybe this method should be removed, because it is not used for any
    // important tasks but item count, also not all the triple stores support
    // it. On the other hand if it is not supported by a triple store it can
    // just remove 0.
    long getItemsCount();

    void dispose();

	void setQueryLog(QueryLog queryLog);
	
}
