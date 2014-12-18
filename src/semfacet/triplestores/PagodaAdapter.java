package semfacet.triplestores;

import java.io.File;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.ox.cs.pagoda.owl.OWLHelper;
import uk.ac.ox.cs.pagoda.reasoner.QueryReasoner;

public class PagodaAdapter implements Store {
    static Logger LOG = Logger.getLogger(PagodaAdapter.class.getName());

    private QueryReasoner store;

    public PagodaAdapter(String ontologyPath) {
        try {
            OWLOntology ontology = OWLHelper.loadOntology(ontologyPath);
            this.store = QueryReasoner.getInstanceForSemFacet(ontology);
        } catch (Exception e) {
            LOG.error("Ontology was not found.");
        }

    }

    @Override
    public void loadData(File file) throws StoreException {
        if (file!= null && file.exists())
            store.importData(file.getAbsolutePath());
        if (!store.preprocess())
            LOG.error("The ontology and dataset is inconsistent!");
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
    public ResultSet executeQuery(String query, boolean computeUpperBound) {
        ResultSet result = new PagodaTupleIteratorAdapter(store.evaluate(query, computeUpperBound));
        return result;
    }

    @Override
    public long getItemsCount() {
        // This method is not supported by Pagoda
        return 0;
    }

    @Override
    public void dispose() {
        store.dispose();
    }

}
