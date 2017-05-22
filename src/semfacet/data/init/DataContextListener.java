/**
 * This class is used to load various data before the system starts. Here all "OFFLINE" computations are done.
 */
package semfacet.data.init;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import semfacet.data.enums.StoresEnum;
import semfacet.data.structures.Configurations;
import semfacet.data.structures.FacetName;
import semfacet.model.QueryExecutor;
import semfacet.relational.db.Database;
import semfacet.relational.db.HSqlAdapter;
import semfacet.search.SearchIndex;
import semfacet.triplestores.*;

public class DataContextListener implements ServletContextListener {
    public final static String CONFIGURATIONS = "config";
    static Logger LOG = Logger.getLogger(DataContextListener.class.getName());

    private Store store;
    private Database db;

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        this.store.dispose();
        this.db.shutdown();
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();
        String path = sc.getRealPath("");

        LOG.info("Loading configuration file.");
        Configurations config = loadConfigFile(path);
        LOG.info("Configuration file was loaded.");

        LOG.info("Loading ontology and importing RDF data to triple store.");
        try {
            store = getStore(config);
            store.loadOntology(config.getOntologyPath());
            store.loadData(new File(config.getDataPath()));
            config.setTripleStore(store);
            LOG.info("Number of triples: " + store.getItemsCount());
            if (config.isAggregate() && StoresEnum.valueOf(config.getStoreType()) == StoresEnum.JRDFOX) {
            	Set<String> predicates = QueryExecutor.getAllPredicates(config);
            	store.loadAggregateFacts(predicates);
                Set<String> allpredicates = QueryExecutor.getAllPredicates(config);
                Map<String, FacetName> facetTypeMap = new HashMap<String, FacetName>();
                for (String predicate : allpredicates)
                    //facetTypeMap.put(predicate, QueryExecutor.createPredicateWithDataType(predicate, config));
                	facetTypeMap.put(predicate, QueryExecutor.createPredicateWithDataTypeA(predicate, config));
                config.setFacetTypeMap(facetTypeMap);
                //LOG.info("Predicate types were set.");
                LOG.info("Number of triples after materializing aggregate information: " + store.getItemsCount());
            } else if (config.isAggregate() && StoresEnum.valueOf(config.getStoreType())!= StoresEnum.JRDFOX){
            	LOG.info("Aggregates for this type of store is not supported yet");
            }
        } catch (StoreException e) {
            e.printStackTrace();
        }
        config.setTripleStore(store);
        LOG.info("Ontology was loaded and data was imported to triple store.");

        loadInMemoryIndexes(config);

        LOG.info("Opening user activity table connection and creating activity table.");
        try {
            db = new HSqlAdapter(path + "/WEB-INF/db/" + "db_file", "username", "password");
            config.setActivityDatabase(db);
            String newTableQuery = "CREATE TABLE user_activity (user_id INTEGER DEFAULT 0, ip_address VARCHAR(256), keywords VARCHAR(256), facet_name VARCHAR(256), facet_value VARCHAR(256), insertion_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL ) ";
            db.update(newTableQuery);
        } catch (Exception e) {
            LOG.info("user activity table already exists.");
        }
        LOG.info("Connection was opened.");
        sc.setAttribute(CONFIGURATIONS, config);
    }

    
    /** CategoryMap connects individual to their class through predicate type
     * 
     *  **/
    public static void loadInMemoryIndexes(Configurations config) {
        LOG.info("Loading data to search index.");
        config.setSearchIndex(loadDataToSearchIndex(config));
        LOG.info("Data was loaded to search index.");

        LOG.info("Map categories to ids.");
        config.setIdCategoryMap(QueryExecutor.mapCategoriesToIds(config));        
        LOG.info("Number of ids that have categories : " + config.getIdCategoryMap().size());
        LOG.info("Categories were mapped.");

        LOG.info("Map labels to ids.");
        config.setIdLabelMap(QueryExecutor.mapLabelsToIds(config));
        LOG.info("Number of ids that have labels : " + config.getIdLabelMap().size());
        LOG.info("Labels were mapped.");

        LOG.info("Create hierarchy map.");
        config.setHierarchyMap(QueryExecutor.getHierarchyMap(config));
        LOG.info("Hierarchy map was created.");

        LOG.info("Analyze and set predicate types.");
        Set<String> predicates = QueryExecutor.getAllPredicates(config);
                
        Map<String, FacetName> facetTypeMap = new HashMap<String, FacetName>();
        for (String predicate : predicates)
            //facetTypeMap.put(predicate, QueryExecutor.createPredicateWithDataType(predicate, config));
        	facetTypeMap.put(predicate, QueryExecutor.createPredicateWithDataTypeA(predicate, config));
        config.setFacetTypeMap(facetTypeMap);
        
        LOG.info("###### facet Type Map #####  \n" + facetTypeMap);
        LOG.info("Predicate types were set.");
    }

    public static Store getStore(Configurations config) throws StoreException {
        Store store;
        switch (StoresEnum.valueOf(config.getStoreType())) {
        case JRDFOX:
            store = new JRDFoxAdapter();
            break;
        case PAGODA:
            store = new PagodaAdapter(config.getOntologyPath(), config.getPAGOdAToClassify(), config.getPAGOdAToCallHermiT());
            break;
        case SESAME:
            store = new SesameAdapter();
            break;
        case HERMIT:
            store = new HermitAdapter();
            break;
        default:
            store = new JRDFoxAdapter();
            break;
        }
        store.setQueryLog(new QueryLog(config.getQueryLogPath())); 
        return store;
    }

    public static Configurations loadConfigFile(String path) {
        Properties properties = new Properties();
        Configurations config = new Configurations();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(path + "/CONFIG/sys.conf");
            properties.load(inputStream);
            config.setDataPath(path + properties.getProperty("DATA"));
            config.setOntologyPath(path + properties.getProperty("ONTOLOGY"));
            String value = properties.getProperty("MAX_RESULTS_FROM_KEYWORDS"); 
            if (value != null) config.setMaxSearchResuls(Integer.parseInt(value));
            config.setSnippetImagePredicate(properties.getProperty("SNIPPET_IMAGE_PEDICATE"));
            config.setSnippetURLPredicate(properties.getProperty("SNIPPET_URL_PREDICATE"));
            config.setSnippetDescriptionPredicate(properties.getProperty("SNIPPET_DESCRIPTION_PREDICATE"));
            config.setSnippetTitlePredicate(properties.getProperty("SNIPPET_TITLE_PREDICATE"));
            config.setSnippetExtra1Predicate(properties.getProperty("SNIPPET_EXTRA_PREDICATE1"));
            config.setSnippetExtra2Predicate(properties.getProperty("SNIPPET_EXTRA_PREDICATE2"));
            config.setSnippetExtra3Predicate(properties.getProperty("SNIPPET_EXTRA_PREDICATE3"));
            config.setCategoryPredicate(properties.getProperty("CATEGORY_PREDICATE"));
            config.setHierarchyPredicate(properties.getProperty("HIERARCHY_PREDICATE"));
            config.setLabelPredicate(properties.getProperty("LABEL_PREDICATE"));
            config.setLongitudePredicate(properties.getProperty("LONGITUDE_PREDICATE"));
            config.setLatitudePredicate(properties.getProperty("LATITUDE_PREDICATE"));
            config.setStoreType(properties.getProperty("STORE_TYPE"));
            if (StoresEnum.valueOf(config.getStoreType()) == StoresEnum.PAGODA) {
            	if ((value = properties.getProperty("PAGODA_TO_CLASSIFY")) != null)
            		config.setPAGOdAToClassify(Boolean.parseBoolean(value));
            	if ((value = properties.getProperty("PAGODA_CALL_HERMIT")) != null)
            		config.setPAGOdAToCallHermiT(Boolean.parseBoolean(value));
            }
            if ((value = properties.getProperty("QUERY_LOG")) != null)  
            	config.setQueryLogPath(value);
            String nesting = properties.getProperty("NESTING");
            if ("disabled".equals(nesting))
                config.setNesting(false);
            else
                config.setNesting(true);
            String aggregates = properties.getProperty("AGGREGATES");
            if ("disabled".equals(aggregates))
            	config.setAggregates(false);
            else
            	config.setAggregates(true);
            Set<String> conjunctivePredicates = getPredicateSet(properties.getProperty("CONJUNCTIVE_PREDICATES"));
            config.setConjunctivePredicates(conjunctivePredicates);
            Set<String> excludedPredicates = getPredicateSet(properties.getProperty("EXCLUDED_PREDICATES"));
            setDefaultExcludedPredicates(excludedPredicates, config);
            config.setExcludedPredicates(excludedPredicates);
            Set<String> excludedPredicatesAgg = new HashSet<String>();
            for (String predicate : excludedPredicates) {
                excludedPredicatesAgg.add(predicate + "_count");
            	excludedPredicatesAgg.add(predicate + "_min");
            	excludedPredicatesAgg.add(predicate + "_sum");
            	excludedPredicatesAgg.add(predicate + "_max");
            	excludedPredicatesAgg.add(predicate + "_avg");
            }
            config.setExcludedAggregatePredicates(excludedPredicatesAgg);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }
        return config;
    }

    private static Set<String> getPredicateSet(String predicates) {
        Set<String> predicateSet = new HashSet<String>();
        String[] cleanPredicates = predicates.split(",");
        for (String predicate : cleanPredicates)
            predicateSet.add(predicate);
        return predicateSet;
    }

    public static void setDefaultExcludedPredicates(Set<String> excludedPredicates, Configurations config) {
        excludedPredicates.add(config.getSnippetImagePredicate());
        excludedPredicates.add(config.getSnippetURLPredicate());
        excludedPredicates.add(config.getSnippetTitlePredicate());
        excludedPredicates.add(config.getSnippetExtra1Predicate());
        excludedPredicates.add(config.getSnippetExtra2Predicate());
        excludedPredicates.add(config.getSnippetExtra3Predicate());
        excludedPredicates.add(config.getSnippetDescriptionPredicate());
        excludedPredicates.add(config.getCategoryPredicate());
        excludedPredicates.add(config.getLabelPredicate());
        excludedPredicates.add(config.getLongitudePredicate());
        excludedPredicates.add(config.getLatitudePredicate());
        excludedPredicates.add(config.getHierarchyPredicate());
    }

    public static SearchIndex loadDataToSearchIndex(Configurations config) {
        SearchIndex searchIndex = new SearchIndex();
        QueryExecutor.addDataToSearchIndex(searchIndex, config.getSnippetTitlePredicate(), config.getTripleStore());
        QueryExecutor.addDataToSearchIndex(searchIndex, config.getSnippetDescriptionPredicate(), config.getTripleStore());
        QueryExecutor.addDataToSearchIndex(searchIndex, config.getSnippetExtra1Predicate(), config.getTripleStore());
        QueryExecutor.addDataToSearchIndex(searchIndex, config.getSnippetExtra2Predicate(), config.getTripleStore());
        QueryExecutor.addDataToSearchIndex(searchIndex, config.getSnippetExtra3Predicate(), config.getTripleStore());
        searchIndex.getSearchIndexStatistics();
        return searchIndex;
    }

}
