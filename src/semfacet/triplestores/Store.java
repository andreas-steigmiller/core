package semfacet.triplestores;

import java.io.File;

public interface Store {
    void loadData(File file) throws StoreException;
    void loadOntology(String ontologyPath) throws StoreException;
    ResultSet executeQuery(String query, boolean computeUpperBound);
    long getItemsCount();
    void dispose();
}
