package semfacet.triplestores;

import java.io.File;
import java.util.Set;

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
    public void loadData(String dataPath) throws StoreException {
        File file = new File(dataPath);
        loadData(file);
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
    public void loadAggregateFacts(Set<String> allpredicates) {
    	String rules = new String();
    	for (String predicate : allpredicates) {
    			// the following two are the rules for materializing count and sum aggregate facets
    			rules = rules + "<" + predicate +"_count>(?X, ?N) :- " + String.format("<%s>(?X, ?Z),  AGGREGATE(<%s>(?X, ?Y) ON ?X BIND COUNT(?Y) as ?N) . ", predicate, predicate);
    			rules = rules + "<" + predicate +"_min>(?X, ?N) :- " + String.format("<%s>(?X, ?Z),  AGGREGATE(<%s>(?X, ?Y) ON ?X BIND MIN(?Y) as ?N) . ", predicate, predicate);
    	}
    	//the following is ad-hoc implementation of sum and avg, but has to be replaced as above once RDFox supports sum. For now it is quite slow.
    	/*
    	String facts = new String();
    	for (String predicate : allpredicates) {
    			String query  = String.format("SELECT DISTINCT ?x WHERE {?x <%s> ?z}", predicate);
    			ResultSet tupleIterator = this.executeQuery(query, true);
    			if (tupleIterator != null) { 
    				tupleIterator.open();
    				Float number;
    				String postfix;
    				PredicateTypeEnum type = PredicateTypeEnum.UNKNOWN;
					while (tupleIterator.hasNext()) {
						String object = tupleIterator.getNativeItem(0);
						Float sum=0.0f, avg = 0.0f;
						Integer count = 0;
						String query1 = String.format("SELECT ?z WHERE {%s <%s> ?z}", object,predicate);
						ResultSet tupleIterator1 = this.executeQuery(query1, true);
						if (tupleIterator1 != null) {
							tupleIterator1.open();
							String subject = tupleIterator1.getNativeItem(0);
							int first = subject.indexOf('"');
							int last = subject.lastIndexOf('"');
							if (first != last && first > -1 && (last+1 < subject.length())) {
								postfix = subject.substring(last + 1, subject.length());
								if (postfix.equals("^^<http://www.w3.org/2001/XMLSchema#float>")) {
									type = PredicateTypeEnum.FLOAT;
								}
								else if (postfix.equals("^^<http://www.w3.org/2001/XMLSchema#integer>")) {
									type = PredicateTypeEnum.INTEGER;
									
								}
								else if (postfix.equals("^^<http://www.w3.org/2001/XMLSchema#double>")){
									type = PredicateTypeEnum.DOUBLE;
								}		
							}
							if (type.equals(PredicateTypeEnum.FLOAT) || type.equals(PredicateTypeEnum.INTEGER) || type.equals(PredicateTypeEnum.DOUBLE)) {
								while (tupleIterator1.hasNext()) {
									subject = tupleIterator1.getItem(0);
										try {
											number = (float) Float.parseFloat(subject);
											sum = sum + number;
											count++;
										} catch (NumberFormatException e) {
											LOG.error("Formatting error: " + e.getMessage());
										}		
									tupleIterator1.next();										
								}
							}
							tupleIterator1.dispose();
							if (type.equals(PredicateTypeEnum.FLOAT)){ 
								facts = facts + String.format("%s <%s_sum> \"%s\"^^<http://www.w3.org/2001/XMLSchema#float> . ", object, predicate,sum.toString());
								avg = (float) sum/count;
								facts = facts + String.format("%s <%s_avg> \"%s\"^^<http://www.w3.org/2001/XMLSchema#float> . ", object, predicate,avg.toString());
							}
							else if (type.equals(PredicateTypeEnum.INTEGER)) {
								facts = facts +  String.format("%s <%s_sum> \"%s\"^^<http://www.w3.org/2001/XMLSchema#integer> . ", object, predicate, String.valueOf(Math.round(sum)));
								avg = (float) sum/count;
								facts = facts + String.format("%s <%s_avg> \"%s\"^^<http://www.w3.org/2001/XMLSchema#float> . ", object, predicate,avg.toString());
							}
							else if (type.equals(PredicateTypeEnum.DOUBLE)) {
								facts = facts + String.format("%s <%s_sum> \"%s\"^^<http://www.w3.org/2001/XMLSchema#double> . ", object, predicate, String.valueOf((double) sum));
								avg = (float) sum/count;
								facts = facts + String.format("%s <%s_avg> \"%s\"^^<http://www.w3.org/2001/XMLSchema#float> . ", object, predicate,avg.toString());
							}
						}
						type = PredicateTypeEnum.UNKNOWN;
						tupleIterator.next();
					}
					tupleIterator.dispose();
    			}
    		}
    		*/	
    	try {
    		store.importText(rules);
    		store.applyReasoning();
    		//store.importText(facts);
    	} catch (JRDFoxException e) {
            LOG.error(e.getMessage());
    	}
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
