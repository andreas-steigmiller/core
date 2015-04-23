package semfacet.triplestores;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

public class SesameAdapter implements Store {
    static Logger LOG = Logger.getLogger(SesameAdapter.class.getName());

    private Repository repository;
    RepositoryConnection store;
    Resource[] resources = {};

    public SesameAdapter() throws StoreException {
        this.repository = new SailRepository(new MemoryStore());
        try {
            repository.initialize();
            store = repository.getConnection();
        } catch (RepositoryException e) {
            throw new StoreException(e.getMessage());
        }
    }

    @Override
    public void loadData(File file) throws StoreException {
        try {
            if (file != null && file.exists())
                store.add(file, null, RDFFormat.N3, resources);

        } catch (RepositoryException | RDFParseException | IOException e) {
            throw new StoreException(e.getMessage());
        }

    }

    @Override
    public void loadOntology(String ontologyPath) throws StoreException {
        try {
            File file = new File(ontologyPath);
            if (file != null && file.exists())
                store.add(file, null, RDFFormat.RDFXML, resources);
        } catch (RepositoryException | RDFParseException | IOException e) {
            throw new StoreException(e.getMessage());
        }
    }

    @Override
    public ResultSet executeQuery(String query, boolean computeUpperBound) {
    	if (!computeUpperBound) queryLog.add(query);    	
        ResultSet result = null;
        try {
            TupleQuery tupleQuery = store.prepareTupleQuery(QueryLanguage.SPARQL, query, null);
            result = new SesameTupleIteratorAdapter(tupleQuery.evaluate());
        } catch (RepositoryException | MalformedQueryException | QueryEvaluationException e) {
            LOG.error("QUERY: " + query + " EXCEPTION: " + e.getMessage());
        }
        return result;
    }

    @Override
    public long getItemsCount() {
        try {
            return store.size(resources);
        } catch (RepositoryException e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public void dispose() {
    	queryLog.dispose();
        try {
            repository.shutDown();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }

    }
    
    QueryLog queryLog; 

	@Override
	public void setQueryLog(QueryLog queryLog) {
		this.queryLog = queryLog;		
	}

}
