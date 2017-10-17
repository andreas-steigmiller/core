/**
 * This class is the main class, which contains various methods to execute queries on a triple store. In MVC this would be call a Model class. Maybe this class already grew too much and maybe it makes sense to separate it into smaller classes.
 */

package semfacet.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


import org.apache.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import semfacet.controler.QueryManager;
import semfacet.data.enums.FacetValueEnum;
import semfacet.data.enums.PredicateTypeEnum;
import semfacet.data.structures.Configurations;
import semfacet.data.structures.FacetName;
import semfacet.data.structures.FacetValue;
import semfacet.data.structures.Triple;
import semfacet.search.SearchIndex;
import semfacet.triplestores.ResultSet;
import semfacet.triplestores.Store;

public class QueryExecutor {
    static Logger LOG = Logger.getLogger(QueryExecutor.class.getName());

    public static String getAttributeForId(Store store, String id, String attributeName) {
        String result = "";
        String query = String.format("SELECT ?z WHERE { <%s> <%s> ?z }", id, attributeName);
        ResultSet tupleIterator = store.executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            if (tupleIterator.hasNext()) {
                result = tupleIterator.getItem(0);
            }

            tupleIterator.dispose();
        }
        return result;
    }

    public static List<String> executeFocusOnClass(String query, Set<String> luceneAnswers, Configurations config) {
        List<String> subjects = new ArrayList<String>();
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, false);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                if (luceneAnswers.contains(tupleIterator.getItem(1))) {
                    subjects.add(tupleIterator.getItem(0));
                }
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
        return subjects;
    }

    public static List<String> executeSelectedFacetQuery(Store store, String facetQueryString) {

        List<String> subjects = new ArrayList<String>();
        ResultSet tupleIterator = store.executeQuery(facetQueryString, false);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                subjects.add(tupleIterator.getItem(0));
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
        return subjects;
    }

    public static Multimap<String, String> mapCategoriesToIds(Configurations config) {
        Multimap<String, String> multimap = HashMultimap.create();
        String query = String.format("select ?x ?z where { ?x <%s> ?z } ", config.getCategoryPredicate());
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                multimap.put(tupleIterator.getItem(0), tupleIterator.getItem(1));
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
        return multimap;
    }

    public static Map<String, String> mapLabelsToIds(Configurations config) {
        Map<String, String> multimap = new HashMap<String, String>();
        ResultSet tupleIterator = config.getTripleStore().executeQuery(
                String.format("select ?x ?z where { ?x <%s> ?z } ", config.getLabelPredicate()), true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                multimap.put(tupleIterator.getItem(0), tupleIterator.getItem(1));
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
        return multimap;
    }

    public static Multimap<String, String> getHierarchyMap(Configurations config) {
        Set<String> roots = new HashSet<String>();
        Set<String> allElements = new HashSet<String>();
        Multimap<String, String> multimap = HashMultimap.create();

        ResultSet tupleIterator = config.getTripleStore().executeQuery(
                String.format("SELECT ?x ?z WHERE { ?x <%s> ?z } ", config.getHierarchyPredicate()), true);

        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                String subject = tupleIterator.getItem(0);
                String object = tupleIterator.getItem(1);
                multimap.put(object, subject);
                roots.add(object);
                if (roots.contains(subject)) {
                    roots.remove(subject);
                }
                allElements.add(subject);
                allElements.add(object);
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }

        tupleIterator = config.getTripleStore()
                .executeQuery(String.format("SELECT ?x ?z WHERE { ?x <%s> ?z } ", config.getCategoryPredicate()), true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                String subject = tupleIterator.getItem(0);
                String object = tupleIterator.getItem(1);
                if (!allElements.contains(object))
                    roots.add(object);
                multimap.put(object, subject);
                allElements.add(subject);
                allElements.add(object);
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }

        for (String root : roots) {
            if (!root.contains("owl:Thing") && !root.contains("owl#Thing"))
                multimap.put("owl:Thing", root);
        }

        // System.out.println(multimap.get("owl:Thing").toString());
        LOG.info("Number of objects in hierarchy: " + multimap.size());
        return multimap;
    }

    public static List<String> getSnippetIdsForCategories(List<String> categories, Configurations config) {
        ResultSet tupleIterator = null;
        List<String> snippetIds = new ArrayList<String>();

        for (String cat : categories) {
            tupleIterator = config.getTripleStore().executeQuery(
                    String.format("select distinct ?x where { ?x <%s> <%s> } ", config.getCategoryPredicate(), cat), true);
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                    snippetIds.add(tupleIterator.getItem(0));
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
        }

        return snippetIds;

    }

    public static void addDataToSearchIndex(SearchIndex searchIndex, String predicate, Store store) {
        Set<String> uniqueIds = new HashSet<String>();
        String query = String.format("select ?x ?y where { ?x <%s> ?y . } ", predicate);
        ResultSet tupleIterator = store.executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                try {
                    if (!uniqueIds.contains(tupleIterator.getItem(0)))
                        searchIndex.addTextToSearchIndex(tupleIterator.getItem(0), tupleIterator.getItem(1), " ");
                    uniqueIds.add(tupleIterator.getItem(0));
                } catch (Exception e) {
                    LOG.error("Corrupt data: " + e.getMessage());
                }
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
    }

    public static boolean hideValue(String query, Set<String> retrievedIds, Configurations config) {
        boolean hide = true;
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                if (retrievedIds.contains(tupleIterator.getItem(0))) {
                    hide = false;
                    break;
                }
                tupleIterator.next();
            }

            tupleIterator.dispose();
        }
        return hide;
    }
    
    
    
    
    static Map<String, String> facet_query = new HashMap<String, String>();
    static Map<String, Set<String>> facet_results = new HashMap<String, Set<String>>();
    static String prefix_ontology = "";

    /**
     * New method added to find all the possible facet values of a reachable facet name.
     * We have to check recursively if 'facetName' is reachable from the current 'queryList'
     * and in the meanwhile we store the result of the paths for reaching the correspondent facet values
     * @param queryList: current queryList 
     * @param retrievedIds: current result set
     * @param limit: threshold for the nesting
     * @param count: counter for the recursion
     * @param config: configuration of the system
     * @return a boolean that indicates whether the facet name is reachable or not
     * 
     * */
    public static List<FacetValue> getReachableFacetValues(String facetName, String query,
    		Set<String> retrievedIds, int limit, int count, List<FacetValue> facet_values,Configurations config) {
    	if(count == 0){
    		return facet_values;
    	}
    	
    	if(count == limit){
    		facet_query.clear();
    		facet_results.clear();
    	}
    	
    	int iteration = limit-count;    	
			query = query.trim();			
			String triple = query.split(" . ")[0];			
			String object = triple.split(" ")[2].trim();
			
			String newSubject = "";
			if(iteration == 0) newSubject = object;
			else newSubject = "?z";
			
			String newPredicate = "?y";
			String newObject = "?zz";
			String filters = "";
			
			for(int i=0; i<iteration; i++){
				newSubject += "z";
				newPredicate += "y";
				newObject += "z";
				if(filters.equals(""))
					filters += newPredicate.substring(0, newPredicate.length()-1) +" !=  <" + config.getCategoryPredicate() + ">";
				else
					filters += " && " + newPredicate.substring(0, newPredicate.length()-1) +" !=  <" + config.getCategoryPredicate() + ">";
			}
			
			String newQueryWithVariable = query + " . "+ newSubject + " "+ newPredicate + " " + newObject;
			String newQuery = query + " . "+ newSubject + " <"+ prefix_ontology + facetName + "> " + newObject;
			String newQueryClass = String.format("%s . " +newObject+" <%s> ?t . ", newQuery, config.getCategoryPredicate());
			String completeObjectQuery = "";
			String completeClassQuery = "";
			
			if(!filters.equals("")){
				String path = "";
				for(int j=2; j<newPredicate.length(); j++)
					path += newPredicate.substring(0, j) + " ";
				
				completeObjectQuery = String.format("Select distinct ?x "+newObject+" "+path+" where {%s . FILTER (%s) }", newQuery, filters);
				completeClassQuery = String.format("Select distinct ?x ?t "+path+" where {%s .  "+newObject+" <%s> ?t . FILTER (%s) }",
						newQuery,config.getCategoryPredicate(), filters);
			}

			else{
				completeObjectQuery = String.format("Select distinct ?x " + newObject+" where {%s }", newQuery);
				completeClassQuery = String.format("Select distinct ?x ?t where {%s .  "+newObject+" <%s> ?t .}",
					newQuery,config.getCategoryPredicate(), filters);
			}				
				
			Set<String> facetObjectValues = addReachableFacetValues(newObject, newQuery, completeObjectQuery, FacetValueEnum.OBJECT.toString(), iteration, retrievedIds, config);
			Set<String> facetClassValues = addReachableFacetValues(newObject, newQueryClass, completeClassQuery, FacetValueEnum.CLASS.toString(), iteration, retrievedIds, config);
            
			//LOG.info("facet_results: " + facet_results);
			LOG.info("facet_query: " + facet_query);

			//Here we construct the map for the ranking for both individual and classes
			Map<String, Integer> map_facetClassValues = new HashMap<String, Integer>();
	        Map<String, Integer> map_facetObjectValues = new HashMap<String, Integer>();
			for(String obj : facetObjectValues){
				map_facetObjectValues.put(obj, facet_results.get(obj).size());
			}
			
			for(String cl : facetClassValues){
				map_facetClassValues.put(cl, 1);
			}
			
	    	List<FacetValue> facetValues = QueryManager.getFacetValueHierarchy(config, map_facetClassValues, map_facetObjectValues, false);
	    	for(FacetValue fv : facetValues){
	    		String parent = fv.getParent();
	    		if(parent != null){
	    			if(!facet_results.containsKey(parent)){
	    				Set<String> results = new HashSet<String>();
            			results.addAll(facet_results.get(fv.getObject()));
            			facet_results.put(parent, results);
	    			}
	    			else{
	    				facet_results.get(parent).addAll(facet_results.get(fv.getObject()));
	    			}
	    		}
	    	}
	    	
	    	//update of the answer sets for the ranking and queryList for each facet value
	    	List<FacetValue> nonPresent = new ArrayList<FacetValue>();
	       	for(FacetValue fv : facetValues){
	       		boolean already_present = false;
	       		for(FacetValue facet_value : facet_values){
	       			
	       			if(facet_value.getObject().equals(fv.getObject())){
	       				Set<String> answerSet = facet_value.getAnswer_set_on_selection();
	       				/*
	       				LOG.info("update of the answer set. Current answer set for the facet: "
	       				+ fv.getObject() + " --> " + answerSet);
	       				
	       				LOG.info("update of the query list. Current queryList for the facet: "
	    	       				+ fv.getObject() + " --> " + facet_value.getQuery_reachability());
	    	       		*/
	       				
	       				facet_value.getQuery_reachability().add(facet_query.get(fv.getObject()));
	       				answerSet.addAll(facet_results.get(fv.getObject()));
	       				facet_value.answerSetOnSelection(answerSet);
	       				facet_value.setRanking(answerSet.size());
		    			already_present = true;
		    			
		    			/*LOG.info("new answer set for the facet: "
			       				+ fv.getObject() + " --> " + facet_value.getAnswer_set_on_selection());
		    			
		    			LOG.info("new query list for the facet: "
			       				+ fv.getObject() + " --> " + facet_value.getQuery_reachability());
			       		*/
	       			}
	       		}
	       		
	       		if(!already_present){
	       			List<String> qList = new ArrayList<String>();
	    			qList.add(facet_query.get(fv.getObject()));
	    			fv.setQuery_reachability(qList);
	       			fv.answerSetOnSelection(facet_results.get(fv.getObject()));
	    			fv.setRanking(facet_results.get(fv.getObject()).size());
	    			nonPresent.add(fv);
	       		}
	    	}
	    	
	    	facet_values.addAll(nonPresent);
		
        return getReachableFacetValues(facetName,newQueryWithVariable,retrievedIds,limit, --count, facet_values, config);
    }
    
    
    private static Set<String> addReachableFacetValues(String newObject, String queryToReplace, String query, String typeFacetValue, 
    		int iteration, Set<String> retrievedIds, Configurations config){
    	Set<String> values = new HashSet<String>();
    	
    	LOG.info("query performed for the reachability: " + query);
    	ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            
            while (tupleIterator.hasNext()) {
            	String qToReplace = queryToReplace;
            	if (retrievedIds.contains(tupleIterator.getItem(0))) {
            		String facet = tupleIterator.getItem(1);
            		
            		//LOG.info("FACET: " + facet);
            		//Here we construct the query
            		String q = "";
            		String predicateToReplace = "?y";
            		for(int i=2; i<=iteration+1; i++){
            			String predicate = tupleIterator.getItem(i);
            			//LOG.info("predicate " + predicate);
            			qToReplace = qToReplace.replace(" "+predicateToReplace+" ", " <"+predicate+"> ");
            			predicateToReplace += "y";
            		}
            		if(typeFacetValue.equals(FacetValueEnum.OBJECT.toString()))
            			q = qToReplace.replace(newObject, "<"+facet+">");
            		else
            			q = qToReplace.replace("?t", "<"+facet+">");
            		
            		
            		//We add in the list if and only if it is new or it has the same query in the map
            		if(!facet_query.containsKey(facet)){
            			values.add(facet);
            			facet_query.put(facet, q);
            			Set<String> results = new HashSet<String>();
            			results.add(tupleIterator.getItem(0));
            			facet_results.put(facet, results);
            		}
            		else{
            			if((typeFacetValue.equals(FacetValueEnum.OBJECT.toString()) && facet_query.get(facet).equals(q))){
                			facet_results.get(facet).add(tupleIterator.getItem(0));
            			}
            		}
            	}
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
        return values;
    }
    
    
    
    
    
    
    
    
    
    
    /**
     * New method added to count the elements in the answer set of the query.
     * @param query: query to be performed
     * @param retrievedIds: current result set
     * @param config: configuration of the system
     * @return the cardinality of the answer set
     * 
     * */
    public static int getSizeResultSet(String query, Set<String> retrievedIds, Configurations config) {
    	int result = 0;
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                if (retrievedIds.contains(tupleIterator.getItem(0))) {
                    result++;
                }
                tupleIterator.next();
            }

            tupleIterator.dispose();
        }
        return result;
    }
    
    
    public static Set<String> getPredicatesFromStore(List<String> queryList, Configurations config) {
        Set<String> facetNames = new HashSet<String>();
        ResultSet tupleIterator = null;
        for (String q : queryList) {
            String query = String.format("select DISTINCT ?y where { ?x ?y ?z . %s } ", q);
            tupleIterator = config.getTripleStore().executeQuery(query, true);
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                    String predicate = tupleIterator.getItem(0);
                    if (!config.getExcludedPredicates().contains(predicate) && !config.getExcludedAggregatePredicates().contains(predicate)) {
                        facetNames.add(predicate);
                    }
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
        }
        return facetNames;
    }

    public static Set<String> getNestedPredicatesFromStore(List<String> queryList, FacetValue toggledFacetValue, Configurations config) {
        Set<String> facetNames = new HashSet<String>();
        ResultSet tupleIterator = null;
        
        for (String q : queryList) {
            if (q.contains(toggledFacetValue.getId())) {
                String query = String.format("select DISTINCT ?y where { ?%s ?y ?z . %s } ", toggledFacetValue.getId(), q);
                tupleIterator = config.getTripleStore().executeQuery(query, true);
                if (tupleIterator != null) {
                    tupleIterator.open();
                    while (tupleIterator.hasNext()) {
                        String predicate = tupleIterator.getItem(0);
                        if (!config.getExcludedPredicates().contains(predicate) && !config.getExcludedAggregatePredicates().contains(predicate)) {
                            facetNames.add(predicate);
                        }
                        tupleIterator.next();
                    }
                    tupleIterator.dispose();
                }
            }
        }

        return facetNames;
    }

    public static Set<String> getObjectsFromStore(List<String> queryList, String toggledFacetName, Configurations config) {
        Set<String> facetValues = new HashSet<String>();
        ResultSet tupleIterator = null;
        for (String q : queryList) {
            String query = String.format("select DISTINCT ?z where { ?x <%s> ?z . %s } ", toggledFacetName, q);
            tupleIterator = config.getTripleStore().executeQuery(query, true);
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                    if (tupleIterator.isIndividual(0)) {
                        facetValues.add(tupleIterator.getItem(0));
                    }
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
        }
        return facetValues;
    }

    public static Set<String> getClassesFromStore(List<String> queryList, String toggledFacetName, Configurations config) {
        Set<String> facetValues = new HashSet<String>();
        ResultSet tupleIterator = null;
        for (String q : queryList) {
            String query = String.format("select DISTINCT ?z where { ?x <%s> ?any . ?any <%s> ?z . %s . } ", toggledFacetName,
                    config.getCategoryPredicate(), q);
            tupleIterator = config.getTripleStore().executeQuery(query, true);
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                    if (tupleIterator.isIndividual(0)) {
                        facetValues.add(tupleIterator.getItem(0));
                    }
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
        }
        return facetValues;
    }

    public static Set<String> getFacetObjectValues(List<String> queryList, String subject, String toggledFacetName, String parentFacetValueId,
            Configurations config) {
        Set<String> facetValues = new HashSet<String>();
        ResultSet tupleIterator = null;
        String objectQuery;
        for (String q : queryList) {
            if (parentFacetValueId == null){
                objectQuery = String.format("select DISTINCT ?z where { <%s> <%s> ?z . " + q.replace("?x", "<" + subject + ">") + "} ", subject,
                        toggledFacetName);
            }
            else {
                objectQuery = String.format("select DISTINCT ?z where { ?%s <%s> ?z . " + q.replace("?x", "<" + subject + ">") + "} ",
                        parentFacetValueId, toggledFacetName);
            }

            if (parentFacetValueId == null || q.contains(parentFacetValueId)) {
                tupleIterator = config.getTripleStore().executeQuery(objectQuery, true);
                if (tupleIterator != null) {
                    tupleIterator.open();
                    while (tupleIterator.hasNext()) {
                        facetValues.add(tupleIterator.getItem(0));
                        tupleIterator.next();
                    }
                    tupleIterator.dispose();
                }
            }

        }
        return facetValues;
    }

    public static Set<String> getFacetClassValues(List<String> queryList, String subject, String toggledFacetName, String parentFacetValueId,
            Configurations config) {
        Set<String> facetValues = new HashSet<String>();
        ResultSet tupleIterator = null;
        String classQuery;
        for (String q : queryList) {
            if (parentFacetValueId == null)
                classQuery = String.format("select DISTINCT ?z where { <%s> <%s> ?x . ?x <%s> ?z . " + q.replace("?x", "<" + subject + ">") + "}",
                        subject, toggledFacetName, config.getCategoryPredicate());
            else
                classQuery = String.format("select DISTINCT ?z where { ?%s <%s> ?x . ?x <%s> ?z . " + q.replace("?x", "<" + subject + ">") + "}",
                        parentFacetValueId, toggledFacetName, config.getCategoryPredicate());
            if (parentFacetValueId == null || q.contains(parentFacetValueId)) {
                tupleIterator = config.getTripleStore().executeQuery(classQuery, true);
                if (tupleIterator != null) {
                    tupleIterator.open();
                    while (tupleIterator.hasNext()) {
                        facetValues.add(tupleIterator.getItem(0));
                        tupleIterator.next();
                    }
                    tupleIterator.dispose();
                }
            }
        }

        return facetValues;
    }

    public static boolean isQueryEmpty(String query, Configurations config) {
        boolean empty = true;
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            if (tupleIterator.hasNext()) {
                empty = false;
            }
            tupleIterator.dispose();
        }
        return empty;
    }

    public static Set<String> getAllPredicates(Configurations config) {
        Set<String> allPredicates = new HashSet<String>();
        String query = "SELECT DISTINCT ?y WHERE {?x ?y ?z}";
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                allPredicates.add(tupleIterator.getItem(0));
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
        return allPredicates;
    }

    public static void getDistinctSubjects(Configurations config) {
        String query = "SELECT DISTINCT ?x WHERE {?x ?y ?z}";
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            int count = 0;
            while (tupleIterator.hasNext()) {
                count++;
                // tupleIterator.getItem(0);
                tupleIterator.next();
            }
            LOG.info("Number of distinct subjects: " + count);
            tupleIterator.dispose();
        }
    }

    public static void setTripleInformation(Triple triple, Configurations config) {
        String query = String.format("SELECT ?x ?z WHERE {?x <%s> ?z}", triple.getPredicate());
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            if (tupleIterator.hasNext()) {
                triple.setSubject(tupleIterator.getItem(0));
                triple.setObject(tupleIterator.getItem(1));
            }
            tupleIterator.dispose();
        }

    }

    public static void addComments(Configurations config, String path) {
        String query = "SELECT DISTINCT ?x WHERE {?x ?y ?any . ?any <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?z}";
        //
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        File file = new File(path + "fly-extra-data.nt");
        BufferedWriter output;
        try {
            output = new BufferedWriter(new FileWriter(file));
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                    String subject = tupleIterator.getItem(0);
                    if (subject.startsWith("http://www.virtualflybrain.org/ontologies/individuals/")) {
                        output.write("<" + subject + ">  <http://www.w3.org/2000/01/rdf-schema#comment> \"random comment\"@en .\n");
                    }
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
            query = "SELECT ?x ?z WHERE {?x <http://xmlns.com/foaf/0.1/depicts> ?z}";
            tupleIterator = config.getTripleStore().executeQuery(query, true);
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                    String subject = tupleIterator.getItem(0);
                    String object = tupleIterator.getItem(1);
                    output.write("<" + object + ">  <http://dbpedia.org/ontology/thumbnail> <" + subject + "> .\n");
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
            output.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static List<String> getAllSubjects(Configurations config) {
        List<String> allSubjects = new ArrayList<String>();
        String query = String.format("SELECT DISTINCT ?x WHERE {?x <%s> ?z}", config.getSnippetDescriptionPredicate());
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            int count = 0;
            while (tupleIterator.hasNext() && count < config.getMaxSearchResuls()) {
                allSubjects.add(tupleIterator.getItem(0));
                tupleIterator.next();
                count++;
            }
            tupleIterator.dispose();
        }
        return allSubjects;
    }

    public static void iterateResults(Configurations config, int iterations) {
        String query = String.format("SELECT ?x ?y ?z WHERE {?x ?y ?z}", config.getSnippetDescriptionPredicate());
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            int count = 0;
            while (tupleIterator.hasNext() && count < iterations) {
                tupleIterator.getItem(0);
                count++;
                tupleIterator.next();
            }
            System.out.println(count);
            tupleIterator.dispose();
        }
    }
    /*
    public static FacetName createPredicateWithDataType(String predicate, Configurations config) {
        FacetName facetName = new FacetName();
        facetName.setName(predicate);
        PredicateTypeEnum type = PredicateTypeEnum.INTEGER;
        String query = String.format("SELECT DISTINCT ?z WHERE {?x <%s> ?z}", predicate);
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        Float min = Float.POSITIVE_INFINITY;
        Float max = Float.NEGATIVE_INFINITY;
        Float number;
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext() && !type.equals(PredicateTypeEnum.UNKNOWN)) {
                String object = tupleIterator.getItem(0);
                if (type.equals(PredicateTypeEnum.INTEGER)) {
                    try {
                        number = (float) Integer.parseInt(object);
                        if (number > max)
                            max = number;
                        if (number < min)
                            min = number;
                    } catch (NumberFormatException e) {
                        type = PredicateTypeEnum.FLOAT;
                    }
                } else if (type.equals(PredicateTypeEnum.FLOAT)) {
                    try {
                        number = Float.parseFloat(object);
                        if (number > max)
                            max = number;
                        if (number < min)
                            min = number;
                    } catch (NumberFormatException e) {
                        type = PredicateTypeEnum.UNKNOWN;
                    }
                } 
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
        if (!type.equals(PredicateTypeEnum.UNKNOWN) && !min.equals(Float.POSITIVE_INFINITY) && !max.equals(Float.NEGATIVE_INFINITY)) {
            facetName.setMax(max);
            facetName.setMin(min);
        }
        facetName.setType(type.toString());
        return facetName;
    }
    */
    
    public static FacetName createPredicateWithDataTypeA(String predicate, Configurations config) {
    	FacetName facetName = new FacetName();
    	facetName.setName(predicate);
    	PredicateTypeEnum type = PredicateTypeEnum.UNKNOWN;
    	String query  = String.format("SELECT DISTINCT ?z WHERE {?x <%s> ?z}", predicate);
    	ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
    	Float min = Float.POSITIVE_INFINITY;
    	Float max = Float.NEGATIVE_INFINITY;
    	Float number;
        LocalDateTime datetime_min = LocalDateTime.MAX;
        LocalDateTime datetime_max = LocalDateTime.MIN;
    	String postfix;
    	Integer numberOfDateTime = 0;
    	Integer numberOfNumerics = 0;
    	if (tupleIterator != null) {
    		tupleIterator.open();
	    	String object = tupleIterator.getNativeItem(0);
			if (object.startsWith("<") && object.endsWith(">")) {
				type = PredicateTypeEnum.UNKNOWN;
				facetName.setType(type.toString());
				return facetName;
			} else {
				int first = object.indexOf('"');
				int last = object.lastIndexOf('"');
				if (first != last && first > -1 && (last+1 < object.length())) {
					postfix = object.substring(last + 1, object.length());
					if (postfix.equals("^^<http://www.w3.org/2001/XMLSchema#dateTime>")) 
						type = PredicateTypeEnum.XSDDATETIME;
					else if (postfix.equals("^^<http://www.w3.org/2001/XMLSchema#float>")) {
						type = PredicateTypeEnum.FLOAT;
					}
					else if (postfix.equals("^^<http://www.w3.org/2001/XMLSchema#integer>")) {
						type = PredicateTypeEnum.INTEGER;
					}
					else if (postfix.equals("^^<http://www.w3.org/2001/XMLSchema#double>")){
						type = PredicateTypeEnum.DOUBLE;
					}		
				}
			}
			while (tupleIterator.hasNext()) {
				object = tupleIterator.getItem(0);
				if (type.equals(PredicateTypeEnum.XSDDATETIME)) {
		        	try {
		        		LocalDateTime datetime = LocalDateTime.parse(object, DateTimeFormatter.ISO_DATE_TIME);
		        		if (datetime.isAfter(datetime_max))
		        			datetime_max = datetime;
		        		if (datetime.isBefore(datetime_min))
		        			datetime_min = datetime;
		        		numberOfDateTime++;
		        	} catch (DateTimeParseException e) {
		        		//continue;
		        		type = PredicateTypeEnum.UNKNOWN;
						facetName.setType(type.toString());
						return facetName;
		        	}
				} else if (type.equals(PredicateTypeEnum.FLOAT) || type.equals(PredicateTypeEnum.DOUBLE) || type.equals(PredicateTypeEnum.INTEGER)) {
                    try {
                        number = (float) Float.parseFloat(object);
                        if (number > max)
                            max = number;
                        if (number < min)
                            min = number;
                        numberOfNumerics++;
                    } catch (NumberFormatException e) {
                    	//continue;
                    	type = PredicateTypeEnum.UNKNOWN;
						facetName.setType(type.toString());
						return facetName;
                    }
				} else {
					break;
				}
				tupleIterator.next();
			}
			tupleIterator.dispose();
    	}
        if ( (type.equals(PredicateTypeEnum.FLOAT) || type.equals(PredicateTypeEnum.DOUBLE) || type.equals(PredicateTypeEnum.INTEGER)) && !min.equals(Float.POSITIVE_INFINITY) && !max.equals(Float.NEGATIVE_INFINITY)) {
            facetName.setMax(max);
            facetName.setMin(min);
            facetName.setNumberOfNumerics(numberOfNumerics);
        } else if (type.equals(PredicateTypeEnum.XSDDATETIME) && !datetime_max.equals(LocalDateTime.MIN) && !datetime_min.equals(LocalDateTime.MAX)) {
        	facetName.setSliderDateTimeMaxValue(datetime_max);
        	facetName.setSliderDateTimeMinValue(datetime_min);
        	facetName.setNumberOfDateTime(numberOfDateTime);
        }
		facetName.setType(type.toString());
		return facetName;
    }
    
    /*
    public static FacetName createPredicateWithDataType(String predicate, Configurations config) {
        FacetName facetName = new FacetName();
        facetName.setName(predicate);
        PredicateTypeEnum type = PredicateTypeEnum.XSDDATETIME;
        String query = String.format("SELECT DISTINCT ?z WHERE {?x <%s> ?z}", predicate);
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        Float min = Float.POSITIVE_INFINITY;
        Float max = Float.NEGATIVE_INFINITY;
        Float number;
        LocalDateTime datetime_min = LocalDateTime.MAX;
        LocalDateTime datetime_max = LocalDateTime.MIN;
        Integer numberOfDateTime = 0;
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext() && !type.equals(PredicateTypeEnum.UNKNOWN)) {
                String object = tupleIterator.getItem(0);
                if (type.equals(PredicateTypeEnum.XSDDATETIME)) {
                	try {
                		LocalDateTime datetime = LocalDateTime.parse(object, DateTimeFormatter.ISO_DATE_TIME);
                		if (datetime.isAfter(datetime_max))
                			datetime_max = datetime;
                		if (datetime.isBefore(datetime_min))
                			datetime_min = datetime;
                		numberOfDateTime++;
                	} catch (DateTimeParseException e) {
                		type = PredicateTypeEnum.INTEGER;
                	}
                }
                if (type.equals(PredicateTypeEnum.INTEGER)) {
                    try {
                        number = (float) Integer.parseInt(object);
                        if (number > max)
                            max = number;
                        if (number < min)
                            min = number;
                    } catch (NumberFormatException e) {
                        type = PredicateTypeEnum.FLOAT;
                    }
                } else if (type.equals(PredicateTypeEnum.FLOAT)) {
                    try {
                        number = Float.parseFloat(object);
                        if (number > max)
                            max = number;
                        if (number < min)
                            min = number;
                    } catch (NumberFormatException e) {
                        type = PredicateTypeEnum.UNKNOWN;
                    }
                } 
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
        if ((type.equals(PredicateTypeEnum.INTEGER) || type.equals(PredicateTypeEnum.FLOAT)) && !min.equals(Float.POSITIVE_INFINITY) && !max.equals(Float.NEGATIVE_INFINITY)) {
            facetName.setMax(max);
            facetName.setMin(min);
        } else if (type.equals(PredicateTypeEnum.XSDDATETIME) && !datetime_max.equals(LocalDateTime.MIN) && !datetime_min.equals(LocalDateTime.MAX)) {
        	facetName.setMaxDateTime(datetime_max);
        	facetName.setMinDateTime(datetime_min);
        	facetName.setNumberOfDateTime(numberOfDateTime);
        }
        facetName.setType(type.toString());
        return facetName;
    }
   */

    public static void testQuery(Configurations config) {
        String query = "SELECT ?x ?z WHERE {?x <long> ?z . FILTER (?z >= \"0\")}";
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                System.out.println(tupleIterator.getItem(0));
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
    }

}
