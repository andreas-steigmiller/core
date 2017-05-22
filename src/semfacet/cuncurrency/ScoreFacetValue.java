package semfacet.cuncurrency;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import semfacet.controler.FacetQueryConstructionManager;
import semfacet.data.enums.FacetValueEnum;
import semfacet.data.structures.Configurations;
import semfacet.data.structures.FacetValue;
import semfacet.external.operations.ExternalUtils;
import semfacet.model.QueryExecutor;



/**
 * This class allows to attribute the score to each facet values.
 * It implements the Runnable interfacet -> multithreading
 * 
 * */
public class ScoreFacetValue implements Runnable{
	
	static Logger LOG = Logger.getLogger(ScoreFacetValue.class.getName());
	
	private List<FacetValue> valuesToScore;
	List<FacetValue> selectedValues;
	private Set<String> currentResultIds;
	private List<String> keywordSearchIds, queryList;
	private Configurations config;
	
	public ScoreFacetValue(List<FacetValue> values, List<FacetValue> selectedValues, Set<String> currentResultIds,
			List<String> keywordSearchIds, List<String> queryList, Configurations config){
		this.valuesToScore = values;
		this.selectedValues = selectedValues;
		this.currentResultIds = currentResultIds;
		this.keywordSearchIds = keywordSearchIds;
		this.queryList = queryList;
		this.config = config;
	}
	

	public void run(){
		
		if(queryList == null && selectedValues == null){
			computeScoreForTypeValues();
		}
		else if(queryList == null && selectedValues != null){
			computeScoreForDisjunctiveValues();
		}
		else{
			computeScoreForConjunctiveValues();
		}
		
	}



	public void computeScoreForTypeValues() {
		
		for (FacetValue val : valuesToScore) {
			String predicate = val.getPredicate();
			Set<String> retrievedIdsTemp = new HashSet<String>();
			String q = String.format("Select ?x where {?x <%s> <%s> . }", predicate, val.getObject());
			retrievedIdsTemp = new HashSet<String>(QueryExecutor.executeSelectedFacetQuery(config.getTripleStore(),q));
			retrievedIdsTemp.addAll(currentResultIds);
			retrievedIdsTemp.retainAll(keywordSearchIds);
			val.setRanking(retrievedIdsTemp.size());
		}
		
	}
	
	public void computeScoreForDisjunctiveValues(){
		
		for(FacetValue val : valuesToScore){
			String predicate = val.getPredicate();
    		Set<String> retrievedIdsTemp = new HashSet<String>();
    		
    		List<FacetValue> valuesTemp = new ArrayList<FacetValue>();
        	for(FacetValue value : selectedValues){
        		if(!value.getPredicate().equals(predicate))
        			valuesTemp.add(value);
        	}
        	
        	if(valuesTemp.size() == 0){
        		String q = String.format("Select ?x where {?x <%s> <%s> . }", predicate, val.getObject());
    			retrievedIdsTemp = new HashSet<String>(QueryExecutor.executeSelectedFacetQuery(config.getTripleStore(),q));
    			retrievedIdsTemp.addAll(currentResultIds);
    			retrievedIdsTemp.retainAll(keywordSearchIds);
    			val.setRanking(retrievedIdsTemp.size());
        	}
        	else{
        		String facetQuery = FacetQueryConstructionManager.constructQuery(valuesTemp, config);
                List<String> queryListTemp = ExternalUtils.parseQuery(facetQuery);
           		String facetQueryString = "";
        		for(String query : queryListTemp){
        			if (val.getType().equals(FacetValueEnum.OBJECT.toString()))
        				facetQueryString = String.format("Select ?x where {%s. ?x <%s> <%s> . }", query, predicate, val.getObject());
        			
            		else if (val.getType().equals(FacetValueEnum.CLASS.toString()))
            			facetQueryString = String.format("Select ?x where {%s. ?x <%s> ?any . ?any <%s> <%s>. }", query, predicate,config.getCategoryPredicate(), val.getObject());            			
        			retrievedIdsTemp.addAll(new HashSet<String>(QueryExecutor.executeSelectedFacetQuery(config.getTripleStore(),facetQueryString)));
        		}
            	retrievedIdsTemp.addAll(currentResultIds);
            	retrievedIdsTemp.retainAll(keywordSearchIds);
            	val.setRanking(retrievedIdsTemp.size());	
        	}
    	}
    	
 	}
	
	
	public void computeScoreForConjunctiveValues(){
		String hideQuery;
		for(FacetValue val : valuesToScore){
			int number_of_result = 0;
			for (String query : queryList) {
                boolean hide = false;
                
                String parent = "x";
                if (val.getParentId() != null && !val.getParentId().isEmpty())
                    parent = val.getParentId();

                if (val.getType().equals(FacetValueEnum.OBJECT.toString())) {
                    hideQuery = String.format("Select ?x where {%s. ?%s <%s> <%s> . }", query, parent, val.getPredicate(), val.getObject());                    
                    number_of_result += QueryExecutor.getSizeResultSet(hideQuery,currentResultIds, config);
                    val.setRanking(number_of_result);
                    if(number_of_result == 0){
                    	hide = true;
                    }
                } else if (val.getType().equals(FacetValueEnum.CLASS.toString())) {
                    hideQuery = String.format("Select ?x where {%s . ?%s <%s> ?any . ?any <%s> <%s> . }", query, parent, val.getPredicate(),
                            config.getCategoryPredicate(), val.getObject());                    
                    number_of_result += QueryExecutor.getSizeResultSet(hideQuery,currentResultIds, config);
                    val.setRanking(number_of_result);
                    if(number_of_result == 0){
                    	hide = true;
                    }
                }
                
                val.setHidden(hide);
            }
			
		}
		
	}
}
