package semfacet.triplestores;

import java.io.File;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.ox.cs.JRDFox.JRDFStoreException;
import uk.ac.ox.cs.JRDFox.Prefixes;
import uk.ac.ox.cs.JRDFox.store.DataStore;
import uk.ac.ox.cs.JRDFox.store.Parameters;

public class JRDFoxAdapter implements Store {
    static Logger LOG = Logger.getLogger(JRDFoxAdapter.class.getName());

    private DataStore store;
    private static final Prefixes prefixes = new Prefixes();
    private static final Parameters parameters = new Parameters();

    public JRDFoxAdapter() throws StoreException {
        try {
            this.store = new DataStore(DataStore.StoreType.NarrowParallelHead, false);
        } catch (JRDFStoreException e) {
            throw new StoreException(e.getMessage());
        }
    }

    @Override
    public ResultSet executeQuery(String query, boolean computeUpperBound) {
        ResultSet result = null;
        try {
            result = new JRDFoxTupleIteratorAdapter(store.compileQuery(query, prefixes, parameters));
        } catch (Exception e) {
            LOG.error("QUERY: " + query + " EXCEPTION: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void loadData(File file) throws StoreException {
        try {
            if (file != null && file.exists())
                store.importTurtleFile(file, prefixes);
            store.applyReasoning();
        } catch (JRDFStoreException e) {
            throw new StoreException(e.getMessage());
        }
    }

    @Override
    public long getItemsCount() {
        long count = 0;
        try {
            count = store.getTriplesCount();
        } catch (JRDFStoreException e) {
            LOG.error(e.getMessage());
        }
        return count;
    }

    @Override
    public void dispose() {
        store.dispose();
    }

    @Override
    public void loadOntology(String ontologyPath) throws StoreException {
        File file = new File(ontologyPath);
        if (file != null && file.exists()) {
            try {
                OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
                OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
                store.addOntology(ontology);
            } catch (JRDFStoreException | OWLOntologyCreationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
}
