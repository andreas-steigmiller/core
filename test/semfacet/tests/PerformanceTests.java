package semfacet.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;

import semfacet.controler.QueryManager;
import semfacet.data.init.DataContextListener;
import semfacet.data.structures.Configurations;
import semfacet.data.structures.Triple;
import semfacet.model.QueryExecutor;
import semfacet.triplestores.*;

public class PerformanceTests {

    private static List<String> search_result = new ArrayList<String>();
    private static Configurations config;
    private static final String SEARCH_KEYWORDS = "";
    static Logger LOG = Logger.getLogger(PerformanceTests.class.getName());

    @BeforeClass
    public static void initObjectsLoadData() {
        String path = System.getProperty("user.dir") + "/WebContent";

        LOG.info("Loading configuration file.");
        config = DataContextListener.loadConfigFile(path);
        LOG.info("Configuration file was loaded.");

        LOG.info("Loading ontology and importing RDF data to triple store.");
        Store store = null;
        try {
            store = DataContextListener.getStore(config);
            store.loadOntology(config.getOntologyPath());
            store.loadData(new File(config.getDataPath()));
            LOG.info("Number of triples: " + store.getItemsCount());
        } catch (StoreException e) {
            e.printStackTrace();
        }
        config.setTripleStore(store);
        LOG.info("Ontology was loaded and data was imported to triple store.");

        DataContextListener.loadInMemoryIndexes(config);

        LOG.info("Getting ids from keywords.");
        search_result.addAll(QueryManager.getIdsFromKeywordSearch(SEARCH_KEYWORDS, config));
        LOG.info("Ids were obtained.");
    }

    @org.junit.Test
    public void getIdsFromKeywordSearchTest() {
        try {
            List<String> results = QueryManager.getIdsFromKeywordSearch(SEARCH_KEYWORDS, config);
            LOG.info("Keyword search found " + results.size() + " results.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @org.junit.Test
    public void getAllPredicates() {
        QueryExecutor.getAllPredicates(config);
    }

    @org.junit.Test
    public void getDistinctSubjects() {
        QueryExecutor.getDistinctSubjects(config);
    }

    @org.junit.Test
    public void getDataViewTest() {
        LOG.info("Start getDataView.");
        List<Triple> dataView = QueryManager.getDataView(config);
        LOG.info("There are " + dataView.size() + " distinct predicates.");
    }

    @org.junit.Test
    public void testResultIterationPerformance() {
        int iterations = 1000000;
        QueryExecutor.iterateResults(config, iterations);
    }
}
