package semfacet.triplestores;

import java.io.File;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.ox.cs.pagoda.owl.OWLHelper;
import uk.ac.ox.cs.pagoda.reasoner.QueryReasoner;

public class HermitAdapter implements Store {

    static Logger LOG = Logger.getLogger(PagodaAdapter.class.getName());
    private QueryReasoner store;

    public HermitAdapter() {
        this.store = QueryReasoner.getHermiTReasoner(false);
    }

    @Override
    public void loadData(File file) throws StoreException {
        if (file!= null && file.exists())
            store.importData(file.getAbsolutePath());
        if (!store.preprocess())
            LOG.error("The ontology and dataset are inconsistent!");
    }
    
    @Override
    public void loadData(String dataPath) throws StoreException {
    	File file = new File(dataPath);
    	loadData(file);
    }
    
    @Override
    public void loadOntology(String ontologyPath) throws StoreException {
        try {
            OWLOntology ontology = OWLHelper.loadOntology(ontologyPath);
            this.store.loadOntology(ontology);
        } catch (Exception e) {
            LOG.error("Ontology was not found.");
        }
    }
    
    @Override
    public void loadAggregateFacts(Set<String> allpredicates){
    	
    }
    
    @Override
    public ResultSet executeQuery(String query, boolean computeUpperBound) {
    	if (!computeUpperBound) queryLog.add(query);
        ResultSet result = new HermitTupleIteratorAdapter(store.evaluate(query, computeUpperBound));
        return result;
    }

    @Override
    public long getItemsCount() {
     // This method is not supported by Hermit
        return 0;
    }

    @Override
    public void dispose() {
    	queryLog.dispose();
        store.dispose();
    }

    QueryLog queryLog; 
    
	@Override
	public void setQueryLog(QueryLog queryLog) {
		this.queryLog = queryLog; 
	}

}
