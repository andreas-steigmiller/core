package semfacet.cuncurrency;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import semfacet.controler.QueryManager;
import semfacet.data.structures.Configurations;
import semfacet.data.structures.FacetName;
import semfacet.data.structures.FacetValue;
import semfacet.triplestores.ResultSet;


/**
 * Class to retrieve the relevant Facet names from a set of retrievedIds
 * The class implements the Runnable interface -> multithreading
 * 
 * */
public class RelevantFacetName implements Runnable{
	static Logger LOG = Logger.getLogger(RelevantFacetName.class.getName());
	
	private FacetValue toggledFacetValue;
	private List<String> namesToCheck;
	private List<FacetName> relevantFacetNames;
	private Set<String> retrievedIds;
	private Configurations config;
	private List<String> queryList;
	public static int MAX_DEPTH = 2;
	public static int RESULT_THRESHOLD = 1;
	public static int NESTED_THRESHOLD = 5;

	
	public RelevantFacetName(FacetValue toggledFacetValue, List<String> namesToCheck,List<FacetName> relevantFacetNames,
			Set<String> retrievedIds, List<String> queryList, Configurations config){
		this.toggledFacetValue = toggledFacetValue;
		this.namesToCheck = namesToCheck;
		this.relevantFacetNames = relevantFacetNames;
		this.retrievedIds = retrievedIds;
		this.queryList = queryList;
		this.config = config;
	}
	
	
	public void run(){
		if(queryList != null)
			checkRelevantFacetNames(queryList, null);
	}

	private void checkRelevantFacetNames(List<String> queryList, FacetName predecessor) {
		for (String name : namesToCheck) {
            boolean hide = true;
            Set<String> answerSet = new HashSet<String>();
             
            for (String query : queryList) {
            	String completeQuery = "";
            	if (toggledFacetValue != null) {
                    if (query.contains(toggledFacetValue.getId())){
                    	completeQuery = String.format("Select ?x where {%s. ?%s <%s> ?z . }", query, toggledFacetValue.getId(), name);
                      	
                    	ResultSet tupleIterator = config.getTripleStore().executeQuery(completeQuery, true);
                        if (tupleIterator != null) {
                            tupleIterator.open();
                            while (tupleIterator.hasNext()) {
                                if (retrievedIds.contains(tupleIterator.getItem(0))) {
                                    hide = false;
                                    answerSet.add(tupleIterator.getItem(0));
                                }
                                tupleIterator.next();
                            }

                            tupleIterator.dispose();
                        }                    	
                    }
                }
            	else{
                   	completeQuery = String.format("Select ?x where {%s . ?x <%s> ?z }", query, name);
                  	ResultSet tupleIterator = config.getTripleStore().executeQuery(completeQuery, true);
                    if (tupleIterator != null) {
                        tupleIterator.open();
                        while (tupleIterator.hasNext()) {
                            if (retrievedIds.contains(tupleIterator.getItem(0))) {
                                hide = false;
                                answerSet.add(tupleIterator.getItem(0));
                            }
                            tupleIterator.next();
                        }

                        tupleIterator.dispose();
                    }
            	}
            }
            if (!hide) {
            	FacetName fn = new FacetName(name);
            	fn.setAnswerSet(answerSet);
            	
            	if(config.isBrowsingOrder()){
                	fn.setRanking(computeH(fn, fn.getAnswerSet().hashCode(), fn.getAnswerSet().size(), queryList, MAX_DEPTH,0));
            	}
            	
                relevantFacetNames.add(fn);
            }
        }
		
	}
	
	
	/**
	 * Recursive method to compute the Nesting of a Facet Name
	 * @param: name: FacetName taken into account
	 * @param: hashAnswerSet: hashcode of the answer set of the query (name, {any})
	 * @param: sizeAnswerSet: size of the answer set of the query (name, {any})
	 * @param: queryList: current query List
	 * @param: d: threshold on the depth/recursion
	 * @param: score: depth of the facet name <= d
	 * 
	 * */
	public int computeH(FacetName name, int hashAnswerSet, int sizeAnswerSet, List<String> queryList, int d, int score) {
		
		if(d==0 || sizeAnswerSet <= RESULT_THRESHOLD){
			return score;
		}
				
		Set<String> nestedresults = new HashSet<String>();
		
		List<String> newQueryList = new ArrayList<String>();
		for(String query : queryList){
			String newSubject = "?z";
			String newPredicate = "?y";
			String newObject = "?zz";
			String filters = newPredicate +" !=  <" + config.getCategoryPredicate() + ">";
			// We consider 'z' because is the variable used in checkRelevantFacetNames method
			for(int i=0; i<MAX_DEPTH-d; i++){
				newSubject += "z";
				newPredicate += "y";
				newObject += "z";
				filters += " && " + newPredicate +" !=  <" + config.getCategoryPredicate() + ">";
			}
			String newQuery = query + ". "+ newSubject + " "+ newPredicate + " " + newObject;
			
			String completeQuery = String.format("Select distinct ?x where {%s. ?x <%s> ?z. FILTER (%s) }", newQuery, name.getName(), filters);
			newQueryList.add(newQuery);
			
			
			ResultSet tupleIterator = config.getTripleStore().executeQuery(completeQuery, true);
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                	
                    if (retrievedIds.contains(tupleIterator.getItem(0))) {                    
                    	nestedresults.add(tupleIterator.getItem(0));
                    }
                    
                    //threshold for the nested results
                    if(nestedresults.size() == RESULT_THRESHOLD+NESTED_THRESHOLD){
                    	break;
                    }
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
		}		
		if(nestedresults.hashCode() == hashAnswerSet){
			return score+1;
		}
		return computeH(name,nestedresults.hashCode(), nestedresults.size(), newQueryList, d-1, score+1);		
	}
	
	
	
	
	
	
	/**
	 * This static method is useful for the evaluation.
	 * 
	 * */
	public static int computeH(FacetName name, int hashAnswerSet, int sizeAnswerSet, List<String> queryList, int d, int score, Configurations config,
			Set<String> retrievedIds) {
		
		if(d==0 || sizeAnswerSet <= RESULT_THRESHOLD){
			return score;
		}
				
		Set<String> nestedresults = new HashSet<String>();
		
		List<String> newQueryList = new ArrayList<String>();
		for(String query : queryList){
			String newSubject = "?z";
			String newPredicate = "?y";
			String newObject = "?zz";
			String filters = newPredicate +" !=  <" + config.getCategoryPredicate() + ">";
			// We consider 'z' because is the variable used in checkRelevantFacetNames method
			for(int i=0; i<MAX_DEPTH-d; i++){
				newSubject += "z";
				newPredicate += "y";
				newObject += "z";
				filters += " && " + newPredicate +" !=  <" + config.getCategoryPredicate() + ">";
			}
			String newQuery = query + ". "+ newSubject + " "+ newPredicate + " " + newObject;
			
			String completeQuery = String.format("Select distinct ?x where {%s. ?x <%s> ?z. FILTER (%s) }", newQuery, name.getName(), filters);
			newQueryList.add(newQuery);
			
			
			ResultSet tupleIterator = config.getTripleStore().executeQuery(completeQuery, true);
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                	
                    if (retrievedIds.contains(tupleIterator.getItem(0))) {                    
                    	nestedresults.add(tupleIterator.getItem(0));
                    }
                    
                    //threshold for the nested results
                    if(nestedresults.size() == RESULT_THRESHOLD+NESTED_THRESHOLD){
                    	break;
                    }
                    
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
		}
		
		if(nestedresults.hashCode() == hashAnswerSet){
			return score+1;
		}
		return computeH(name,nestedresults.hashCode(), nestedresults.size(), newQueryList, d-1, score+1, config, retrievedIds);		
	}
	
	
	
	
	
	/**
	 * This static method is useful for the evaluation: it calculates the maximal depth, i.e.
	 * the maximum level of nesting of the FacetName 'name'
	 * 
	 * */
	public static int computeMaximalH(FacetName name, int hashAnswerSet, int sizeAnswerSet, List<String> queryList, int d, int score, 
			Configurations config, Set<String> retrievedIds) {
		
		if(sizeAnswerSet <= RESULT_THRESHOLD){
			return score;
		}
				
		Set<String> nestedresults = new HashSet<String>();
		
		List<String> newQueryList = new ArrayList<String>();
		for(String query : queryList){
			String newSubject = "?z";
			String newPredicate = "?y";
			String newObject = "?zz";
			String filters = newPredicate +" !=  <" + config.getCategoryPredicate() + ">";
			// We consider 'z' because is the variable used in checkRelevantFacetNames method
			for(int i=0; i<d; i++){
				newSubject += "z";
				newPredicate += "y";
				newObject += "z";
				filters += " && " + newPredicate +" !=  <" + config.getCategoryPredicate() + ">";
			}
			String newQuery = query + ". "+ newSubject + " "+ newPredicate + " " + newObject;
			
			String completeQuery = String.format("Select distinct ?x where {%s. ?x <%s> ?z. FILTER (%s) }", newQuery, name.getName(), filters);
			newQueryList.add(newQuery);
			
			
			ResultSet tupleIterator = config.getTripleStore().executeQuery(completeQuery, true);
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                	
                    if (retrievedIds.contains(tupleIterator.getItem(0))) {                    
                    	nestedresults.add(tupleIterator.getItem(0));
                    }
                    
                    //threshold for the nested results
                    if(nestedresults.size() == RESULT_THRESHOLD+NESTED_THRESHOLD){
                    	break;
                    }
                    
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
		}
		
		if(nestedresults.hashCode() == hashAnswerSet){
			return score+1;
		}
		return computeMaximalH(name,nestedresults.hashCode(), nestedresults.size(), newQueryList, d+1, score+1, config, retrievedIds);		
	}

	
		

}
