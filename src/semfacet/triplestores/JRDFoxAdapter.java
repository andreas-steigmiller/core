package semfacet.triplestores;

import java.io.File;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.ox.cs.JRDFox.JRDFoxException;
import uk.ac.ox.cs.JRDFox.Prefixes;
import uk.ac.ox.cs.JRDFox.store.DataStore;
//import uk.ac.ox.cs.JRDFox.store.Parameters;

public class JRDFoxAdapter implements Store {
    static Logger LOG = Logger.getLogger(JRDFoxAdapter.class.getName());

    private DataStore store;
    private static final Prefixes prefixes = new Prefixes();
    //private static final Parameters parameters = new Parameters();

    public JRDFoxAdapter() throws StoreException {
        try {
            this.store = new DataStore(DataStore.StoreType.ParallelSimpleNN);
        } catch (JRDFoxException e) {
            throw new StoreException(e.getMessage());
        }
    }

    @Override
    public ResultSet executeQuery(String query, boolean computeUpperBound) {
    	if (!computeUpperBound)	queryLog.add(query);
        ResultSet result = null;
        try {
            result = new JRDFoxTupleIteratorAdapter(store.compileQuery(query, prefixes));
        } catch (Exception e) {
            LOG.error("QUERY: " + query + " EXCEPTION: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void loadData(File file) throws StoreException {
        try {
            if (file != null && file.exists()) {
            	File[] files = new File[1];
            	files[0] = file;
                //store.importFiles(files, prefixes);
            	store.importFiles(files);
            }
            store.applyReasoning();
        } catch (JRDFoxException e) {
            throw new StoreException(e.getMessage());
        }
    }

    @Override
    public long getItemsCount() {
        long count = 0;
        try {
            count = store.getTriplesCount();
        } catch (JRDFoxException e) {
            LOG.error(e.getMessage());
        }
        return count;
    }

    @Override
    public void dispose() {
    	queryLog.dispose();
        store.dispose();
    }

    @Override
    public void loadOntology(String ontologyPath) throws StoreException {
        File file = new File(ontologyPath);
        if (file != null && file.exists()) {
            try {
                OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
                OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
                store.importOntology(ontology);
            } catch (JRDFoxException | OWLOntologyCreationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    QueryLog queryLog; 
    
	@Override
	public void setQueryLog(QueryLog queryLog) {
		this.queryLog = queryLog; 		
	}
}
