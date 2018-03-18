/**
 * 
 * This class is the main class, which contains various methods deal with logic part for client requests. In MVC this would be call a Controller class. Maybe this class already grew too much and maybe it makes sense to separate it into smaller classes.
 *
 **/

package semfacet.controler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.google.common.collect.Sets;

import greedy.Report;

import org.apache.log4j.Logger;

import semfacet.cuncurrency.RelevantFacetName;
import semfacet.cuncurrency.ScoreFacetValue;
import semfacet.data.enums.FacetValueEnum;
import semfacet.data.structures.Configurations;
import semfacet.data.structures.FacetName;
import semfacet.data.structures.FacetValue;
import semfacet.data.structures.Response;
import semfacet.data.structures.Snippet;
import semfacet.data.structures.Triple;
import semfacet.model.QueryExecutor;
import semfacet.ranking.Ranking;
import semfacet.search.SearchIndex;
import semfacet.triplestores.ResultSet;

public class QueryManager {
	static Logger LOG = Logger.getLogger(QueryManager.class.getName());
	static int K_TOP_PREDICATES = 5;
    static double THRESHOLD_INTERSECTION = 1.2;
	
	
    public static Response getInitialFacetData(String searchKeywords, Configurations config) {
        Response answer = new Response();
        
        List<String> retrievedIds = getIdsFromKeywordSearch(searchKeywords, config);
        List<String> idsForPage = getIdsForPage(0, retrievedIds);
        List<Snippet> snipets = getSnippets(idsForPage, config);

        String facetId = config.getCategoryPredicate();
        FacetName firstFacetName = new FacetName(facetId, config.getIdLabelMap().get(facetId));
        answer.setFirstFacetName(firstFacetName);
        answer.setSize(retrievedIds.size());
        answer.setSnippets(snipets);
        
        List<FacetValue> facetValues = getFirstFacetValues(retrievedIds, config);
        facetValues = removeSameLabelFacetValues(facetValues);
        answer.setFacetValues(facetValues);        
        return answer;
    }

    /**
     * This method takes a list of facet values an input and removes the ones
     * that have the same label. There are cases where different facet values
     * have different ids, but the same labels. However, the user is presented
     * with the labels on the screen, so we decided to remove duplicates. This
     * is not a perfect solution, better one would change the label in the data.
     * 
     * @param facetValues
     *            is a list of facet values
     * @return a list of facetValues that have distinct labels
     */
    
    private static List<FacetValue> removeSameLabelFacetValues(List<FacetValue> facetValues) {
        Set<String> uniqueLabels = new HashSet<String>();
        List<FacetValue> uniqueFacetValues = new ArrayList<FacetValue>();

        for (FacetValue fv : facetValues) {
            if (!uniqueLabels.contains((fv.getLabel()))) {
                uniqueLabels.add(fv.getLabel());
                uniqueFacetValues.add(fv);
            }
        }

        return uniqueFacetValues;
    }
    
    
    public static List<FacetValue> getFirstFacetValues(List<String> retrievedIds, Configurations config) {
    	

    	Map<String, Integer> map_facetClassValues = new HashMap<String, Integer>();
        Map<String, Integer> map_facetObjectValues = new HashMap<String, Integer>();
    	
    	//new version
    	for (String subject : retrievedIds){
    		for(String cat : config.getIdCategoryMap().get(subject)){
    			if( map_facetClassValues.get(cat) == null)
    				map_facetClassValues.put(cat, 1);
    			else{
    				int number_of_individuals = map_facetClassValues.get(cat);
        			number_of_individuals++;
            		map_facetClassValues.put(cat, number_of_individuals);
    			}
    		}
    	}
    	List<FacetValue> facetValues = getFacetValueHierarchy(config, map_facetClassValues, map_facetObjectValues, true);
    	Ranking.sortFacetValuesByRank(facetValues);
    	
    	
    	/* old version
    	Set<String> facetIds = new HashSet<String>();
        for (String subject : retrievedIds)
            facetIds.addAll(config.getIdCategoryMap().get(subject));
       
        List<FacetValue> facetValues = getFacetValueHierarchy(config, facetIds, new HashSet<String>(), true);
        */
        
    	
    	
    	Map<String, String> idLabelMap = config.getIdLabelMap();
        
        
        for (FacetValue fValue : facetValues) {
            if (idLabelMap.containsKey(fValue.getObject()))
                fValue.setLabel(idLabelMap.get(fValue.getObject()));
        }
        return facetValues;
    }
    
    

    // This method is a variation of BSF
    public static List<FacetValue> getFacetValueHierarchy(Configurations config, Map<String, Integer> map_facetClassValues, Map<String, Integer> map_facetObjectValues,
            boolean invertType) {
        List<FacetValue> result = new ArrayList<FacetValue>();
        
        Set<String> facetClasses = map_facetClassValues.keySet();
        Set<String> facetIndividuals = map_facetObjectValues.keySet();
        if(facetIndividuals == null)
        	facetIndividuals = new HashSet<String>();
        
        if (config.getHierarchyMap().size() == 0) {
            return getNonHierarcicalFacetValues(facetClasses, facetIndividuals, invertType);
        } else {
            Queue<FacetValue> queue = new LinkedList<FacetValue>();
            
            for (String item : config.getHierarchyMap().get("owl:Thing")) {
                if (!invertType) {
                    if (facetClasses.contains(item)) {
                        FacetValue root = new FacetValue(item, FacetValueEnum.CLASS.toString());
                        queue.add(root);
                    } else if (facetIndividuals.contains(item)) {
                        FacetValue root = new FacetValue(item, FacetValueEnum.OBJECT.toString());
                        queue.add(root);
                    }
                } else {
                    FacetValue root = new FacetValue(item, FacetValueEnum.OBJECT.toString());
                    queue.add(root);
                }
            }

            for (String individual : facetIndividuals) {
                if (!config.getHierarchyMap().containsValue(individual)) {
                    FacetValue root = new FacetValue(individual, FacetValueEnum.OBJECT.toString());
                    queue.add(root);
                }
            }

            while (!queue.isEmpty()) {
                FacetValue node = queue.remove();
                while (!facetClasses.contains(node.getObject()) && !facetIndividuals.contains(node.getObject()) && !queue.isEmpty())
                    node = queue.remove();
                if (facetClasses.contains(node.getObject()) || facetIndividuals.contains(node.getObject())){
                	
                	if (facetClasses.contains(node.getObject()))
                		node.setRanking(map_facetClassValues.get(node.getObject()));
                	
                	if (facetIndividuals.contains(node.getObject()))
                		node.setRanking(map_facetObjectValues.get(node.getObject()));
                		
                	result.add(node);
                }
                    
                if (!config.getHierarchyMap().get(node.getObject()).isEmpty()) {
                    for (String item : config.getHierarchyMap().get(node.getObject())) {
                        if (facetClasses.contains(item)) {
                            if (!invertType) {
                                FacetValue fValue = new FacetValue(item, FacetValueEnum.CLASS.toString());
                                
                                //for the ranking
                                fValue.setRanking(map_facetClassValues.get(item));
                                
                                
                                fValue.setParent(node.getObject());
                                queue.add(fValue);
                            } else {
                                FacetValue fValue = new FacetValue(item, FacetValueEnum.OBJECT.toString());
                                fValue.setParent(node.getObject());
                                queue.add(fValue);
                            }
                        } else if (facetIndividuals.contains(item)) {
                            FacetValue fValue = new FacetValue(item, FacetValueEnum.OBJECT.toString());
                            
                            
                            //for the ranking
                            fValue.setRanking(map_facetObjectValues.get(item));
                            
                            fValue.setParent(node.getObject());
                            queue.add(fValue);
                        }
                    }
                }
            }

            /*
             * for (FacetValue fv : result) { System.out.println(fv.getParent()
             * + " " + fv.getObject()); }
             */

            return result;
        }
    }

    private static List<FacetValue> getNonHierarcicalFacetValues(Set<String> facetClasses, Set<String> facetIndividuals, boolean invertType) {
        List<FacetValue> result = new ArrayList<FacetValue>();
        for (String value : facetClasses) {
            if (!invertType) {
                result.add(new FacetValue(value, FacetValueEnum.CLASS.toString()));
            } else {
                result.add(new FacetValue(value, FacetValueEnum.OBJECT.toString()));
            }
        }
        for (String value : facetIndividuals)
            result.add(new FacetValue(value, FacetValueEnum.OBJECT.toString()));
        return result;
    }

    
    
    
    
    
    /*
     *  If we have the evaluation mode, we have to rank the facet names according to:
     *  1. Basic Set Cover algorithm for the diversified predicates
     *  2. Greedy approaches of Set Cover algorithm for the diversified predicates
     *  3. Oxford getDiversifiedPredicates
     *  4. Introduce reachability (config.isBrowsingOrder()) with depth MAX_INTEGER
     *  5. Introduce reachability (config.isBrowsingOrder()) with depth d
     */
    

    
    public static Response getInitialFacetNames(List<String> queryList, String searchKeywords, 
    		Configurations config, boolean evaluation, int max_depth) {
    	Response answer = new Response();
        Set<String> retrievedIds = new HashSet<String>(QueryManager.getIdsFromFacets(config, queryList));
        Set<String> keywordSearchIds = new HashSet<String>(getIdsFromKeywordSearch(searchKeywords, config));
        retrievedIds = Sets.intersection(retrievedIds, keywordSearchIds);
        
        List<FacetName> facetNames = getFacetNamesHybridAlg(null, queryList, retrievedIds, config, max_depth);
        
        List<Set<String>> answerSets = new ArrayList<Set<String>>();
        
        Map<String, FacetName> facetTypeMap = config.getFacetTypeMap();
        Map<String, String> idLabelMap = config.getIdLabelMap();
        for (FacetName fn : facetNames) {
            if (idLabelMap.containsKey(fn.getName()))
                fn.setLabel(idLabelMap.get(fn.getName()));
            fn.setType(facetTypeMap.get(fn.getName()).getType());
            fn.setMin(facetTypeMap.get(fn.getName()).getMin());
            fn.setMax(facetTypeMap.get(fn.getName()).getMax());
            fn.setNumberOfNumerics(facetTypeMap.get(fn.getName()).getNumberOfNumerics());
            fn.setSliderDateTimeMaxValue(facetTypeMap.get(fn.getName()).getSliderDateTimeMaxValue());
            fn.setSliderDateTimeMinValue(facetTypeMap.get(fn.getName()).getSliderDateTimeMinValue());
            
            LOG.info("nesting score of facet name: " + fn.getName() +": "+ fn.getRanking());
               
            //In this phase getRanking returns the nesting score of each facet name 
            if(retrievedIds.size() > 1 && config.isBrowsingOrder())
            	fn.setRanking( fn.getRanking()* (1.0 -  Math.log((double)fn.getAnswerSet().size()) / Math.log((double)retrievedIds.size()) ) );
            
            answerSets.add(fn.getAnswerSet());
        }
        
        
        LOG.info("Current results: " + retrievedIds);
        for (Set<String> as : answerSets)
        	LOG.info("Answer set: " + as);
        if (evaluation){
        	Report.getGreedyAlgorithms(retrievedIds, answerSets);
        }
        
        
        
        
        
        
        if(config.isBrowsingOrder()){
        	Ranking.sortFacetNamesByRank(facetNames);
        	double start = System.currentTimeMillis();
            List<FacetName> facetNames2 = getDiversifiedPredicates(facetNames, retrievedIds, K_TOP_PREDICATES, THRESHOLD_INTERSECTION);        
            double end = System.currentTimeMillis();
            LOG.info("Time diversified predicates (ms): " + (end-start));
            
            answer.setSize(retrievedIds.size());
            answer.setFacetNames(facetNames2);
        }
        else{
        	Ranking.sortFacetNamesAlphabetically(facetNames, true);
            answer.setSize(retrievedIds.size());
            answer.setFacetNames(facetNames);
        }
        
        
        
        /* old version
        Ranking.sortFacetNamesAlphabetically(facetNames, true);
        answer.setSize(retrievedIds.size());
        answer.setFacetNames(facetNames);
        */

        /*  This code is for the evaluation of browse-ability strategy
        int k_tops[] = {3,4,5,7,10};
        for(int i=0; i< k_tops.length; i++){   
            String type = EvaluationRankingBrowsability.Rank.OTHER.getType();
            EvaluationRankingBrowsability erb = new EvaluationRankingBrowsability(type,searchKeywords,facetNames2, retrievedIds, config, queryList, k_tops[i]);
            Thread t = new Thread(erb);
            t.start();
            
            Ranking.sortFacetNamesAlphabetically(facetNames, true);
            String type2 = EvaluationRankingBrowsability.Rank.ALPHABETICAL.getType();
            EvaluationRankingBrowsability erb2 = 
            		new EvaluationRankingBrowsability(type2,searchKeywords,facetNames, retrievedIds, config, queryList, k_tops[i]);
            Thread t2 = new Thread(erb2);
            t2.start();
        }
        
        /* 
        double scoreALG = EvaluationRankingBrowsability.calculateScoreObjFunction(K_TOP_PREDICATES, facetNames, retrievedIds);
        LOG.info("score ALG: " + scoreALG);
        
        List<FacetName> optimal_ordering = EvaluationRankingBrowsability.maxOrdering(searchKeywords, queryList, facetNames, retrievedIds, K_TOP_PREDICATES);
        double end2 = System.currentTimeMillis();
        LOG.info("time OPT Rank: " + (end2-end1 + (end-start)));
        
        
        String type = EvaluationRankingBrowsability.Rank.OTHER.getType();
        EvaluationRankingBrowsability erb = new EvaluationRankingBrowsability(type,searchKeywords,facetNames2, retrievedIds, config, queryList, K_TOP_PREDICATES);
        Thread t = new Thread(erb);
        t.start();
        
        Ranking.sortFacetNamesAlphabetically(facetNames, true);
        String type2 = EvaluationRankingBrowsability.Rank.ALPHABETICAL.getType();
        EvaluationRankingBrowsability erb2 = 
        		new EvaluationRankingBrowsability(type2,searchKeywords,facetNames, retrievedIds, config, queryList, K_TOP_PREDICATES);
        Thread t2 = new Thread(erb2);
        t2.start();
        
        
        String type3 = EvaluationRankingBrowsability.Rank.OPT_BROWSABILITY.getType();
        EvaluationRankingBrowsability erb3 = 
        		new EvaluationRankingBrowsability(type3,searchKeywords,optimal_ordering, retrievedIds, config, queryList, K_TOP_PREDICATES);
        Thread t3 = new Thread(erb3);
        t3.start();
        */
        

        
        return answer;
    }
    
    
    
    /**
     * return the set of more diversified facet predicates
     * starting from a sorted list by ranking and a threshold k of the number of top k diversified
     */
    public static List<FacetName> getDiversifiedPredicates(List<FacetName> predicates, Set<String> retrievedIds, 
    		int k_top, double t_overlapped){
    	List<FacetName> selected = new ArrayList<FacetName>();
    	List<FacetName> temp = new ArrayList<FacetName>();
    	Set<String> uncovered = new HashSet<String>(retrievedIds);
    	
    	for(FacetName predicate : predicates){
    		Set<String> answerSet =predicate.getAnswerSet();
    		int sizeAnswerSet = answerSet.size();
    		
    		if(predicate.getRanking() > 0 && (selected.size() < k_top || uncovered.size() != 0)){
    			
    			if(Sets.intersection(answerSet, uncovered).size() > ((double)sizeAnswerSet/t_overlapped)){
    				selected.add(predicate);
    				uncovered.removeAll(answerSet);
    			}
    			else{
    				temp.add(predicate);
    			}    			
    		}
    		else{
    			temp.add(predicate);
    		}
    	}
    	selected.addAll(temp);
    	return selected;
    }
    
    
    
    /*This is for the approach of linear combination */
    public static List<FacetName> getDiversifiedPredicatesLinearCombination(List<FacetName> predicates, Set<String> retrievedIds, 
    		int k_top, double t_overlapped, double beta){
    	List<FacetName> selected = new ArrayList<FacetName>();
    	List<FacetName> temp = new ArrayList<FacetName>();
    	Set<String> uncovered = new HashSet<String>(retrievedIds);
    	
    	
    	for(FacetName predicate : predicates){
    		
    		Set<String> answerSet =predicate.getAnswerSet();
    		int sizeAnswerSet = answerSet.size();
    		
    		if(predicate.getRanking() > 0 && (selected.size() < k_top || uncovered.size() != 0)){
    			
    			if(predicate.getRanking() + beta*Sets.intersection(answerSet, uncovered).size() > ((double)sizeAnswerSet/t_overlapped)){
    				selected.add(predicate);
    				uncovered.removeAll(answerSet);
    			}
    			else{
    				temp.add(predicate);
    			}    			
    		}
    		else{
    			temp.add(predicate);
    		}
    	}
    	selected.addAll(temp);
    	return selected;
    }
    
    
    public static Response getDataForSelectedValue(FacetValue toggledFacetValue, String searchKeywords, 
    		List<String> queryList, Configurations config, int max_depth) {
        Response answer = new Response();
        Set<String> retrievedIds = getIdsFromFacets(config, queryList);
        List<String> keywordSearchIds = getIdsFromKeywordSearch(searchKeywords, config);
        retrievedIds.retainAll(keywordSearchIds);
        List<FacetName> relevantFacetNames = new ArrayList<FacetName>();
        
        LOG.info("toggledFacetValue: "+ toggledFacetValue);
       // LOG.info("toggledFacetValue type: "+ toggledFacetValue.getType());
        LOG.info("QUERY LIST: " + queryList);

        //Here the nested facet names
        if (config.isNesting() && toggledFacetValue.getType().equals(FacetValueEnum.CLASS.toString()))
            relevantFacetNames = getFacetNamesHybridAlg(toggledFacetValue, queryList, retrievedIds, config,max_depth);
        
        List<String> idsForPage = getIdsForPage(0, new ArrayList<String>(retrievedIds));
        List<Snippet> snippets = getSnippets(idsForPage, config);
        answer.setSize(retrievedIds.size());
        Map<String, FacetName> facetTypeMap = config.getFacetTypeMap();
        Map<String, String> idLabelMap = config.getIdLabelMap();
        for (FacetName fn : relevantFacetNames) {
            if (idLabelMap.containsKey(fn.getName()))
                fn.setLabel(idLabelMap.get(fn.getName()));
            fn.setType(facetTypeMap.get(fn.getName()).getType());
            fn.setMin(facetTypeMap.get(fn.getName()).getMin());
            fn.setMax(facetTypeMap.get(fn.getName()).getMax());
            fn.setNumberOfNumerics(facetTypeMap.get(fn.getName()).getNumberOfNumerics());
            fn.setSliderDateTimeMaxValue(facetTypeMap.get(fn.getName()).getSliderDateTimeMaxValue());
            fn.setSliderDateTimeMinValue(facetTypeMap.get(fn.getName()).getSliderDateTimeMinValue());
            
            
            if(retrievedIds.size() > 1 && config.isBrowsingOrder())
            	fn.setRanking( fn.getRanking()* (1.0 -  Math.log((double)fn.getAnswerSet().size()) / Math.log((double)retrievedIds.size()) ) );
        }
        
        
        
        if(config.isBrowsingOrder()){
        	Ranking.sortFacetNamesByRank(relevantFacetNames);
            List<FacetName> facetNames2 = getDiversifiedPredicates(relevantFacetNames, retrievedIds, K_TOP_PREDICATES, THRESHOLD_INTERSECTION);        
            answer.setSize(retrievedIds.size());
            answer.setFacetNames(facetNames2);
        }
        else{
        	Ranking.sortFacetNamesAlphabetically(relevantFacetNames, true);
            answer.setSize(retrievedIds.size());
            answer.setFacetNames(relevantFacetNames);
        }
        
 
        answer.setSnippets(snippets);
        return answer;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    /*
     * This method return a List<FacetValue> and for each facet value
     * the query list as the path to reach that facet value in the graph
     * 
     * */
    
    static int DEPTH_REACHABILITY = 3;
    public static List<FacetValue> getReachableFacetValues(String facetName, List<String> queryList, String searchKeywords,
            Configurations config) {
        Set<String> retrievedIds = new HashSet<String>(QueryManager.getIdsFromFacets(config, queryList));
        Set<String> keywordSearchIds = new HashSet<String>(getIdsFromKeywordSearch(searchKeywords, config));
        retrievedIds = Sets.intersection(retrievedIds, keywordSearchIds);
        
        
        List<FacetValue> facet_values = new ArrayList<FacetValue>();
        
        for(String query : queryList){
            facet_values  = QueryExecutor.getReachableFacetValues(facetName, query, retrievedIds,DEPTH_REACHABILITY,DEPTH_REACHABILITY,facet_values,config);
        }
         
        
        
        
        //for removing the bug in the counters we perform the query and we set the ranking here
        
        
        for(FacetValue fv : facet_values){
        	Set<String> results = new HashSet<String>();
        	for(String query : fv.getQuery_reachability()){
        		String Query = String.format("Select distinct ?x where {%s }", query);
            	ResultSet tupleIterator = config.getTripleStore().executeQuery(Query, true);
                if (tupleIterator != null) {
                    tupleIterator.open();
                    
                    //LOG.info("RESULTS FROM THE QUERY ####");
                    while (tupleIterator.hasNext()) {
                    	if (retrievedIds.contains(tupleIterator.getItem(0))) {
                    		results.add(tupleIterator.getItem(0));
                    	}
                    	tupleIterator.next();
                    }
                    tupleIterator.dispose();  
                 }
        	}
        	fv.answerSetOnSelection(results);
        	/*
        	LOG.info("facet value: " + fv + "\nQueryList: " + fv.getQuery_reachability() +
        			"\nAnswer set: " + fv.getAnswer_set_on_selection());
        			*/
        	fv.setRanking(results.size());
        }
        
        
        List<FacetValue> result = new ArrayList<FacetValue>();
        result = generateSortedFacetValues(facet_values, config);
        
        return result;
    }
    

    
    
    

    
    

    public static List<FacetValue> getFacetValues(String toggledFacetName, String parentFacetValueId, List<String> queryList, String searchKeywords,
            Configurations config) {
        Set<String> retrievedIds = new HashSet<String>(QueryManager.getIdsFromFacets(config, queryList));
        Set<String> keywordSearchIds = new HashSet<String>(getIdsFromKeywordSearch(searchKeywords, config));
        retrievedIds = Sets.intersection(retrievedIds, keywordSearchIds);
        
        List<FacetValue> facetValues = getFacetValuesHybridAlg(toggledFacetName, parentFacetValueId, queryList, retrievedIds, config);
        List<FacetValue> result = generateSortedFacetValues(facetValues, config);
        
        return result;
    }
    
    
    public static List<FacetValue> generateSortedFacetValues(List<FacetValue> values, Configurations config){
    	//new version
        List<FacetValue> facetObjectValue = new ArrayList<FacetValue>();
        List<FacetValue> facetClassValue = new ArrayList<FacetValue>();
        List<FacetValue> result = new ArrayList<FacetValue>();
        
        //old version
        //Map<String, Integer> rankingMap = RankingMap.getRankingMap(config, toggledFacetName, searchKeywords);
        
        
        Map<String, String> idLabelMap = config.getIdLabelMap();
        for (FacetValue v : values) {
            if (idLabelMap.containsKey(v.getObject()))
                v.setLabel(idLabelMap.get(v.getObject()));
            
            //new version
            if(v.getType().equals(FacetValueEnum.OBJECT.toString()))
            	facetObjectValue.add(v);
            else
            	facetClassValue.add(v);
            	
            //old version
            // if (rankingMap.containsKey(v.getObject()))
            //    v.setRanking(rankingMap.get(v.getObject()));
        }
        
        Ranking.sortFacetValuesByRank(facetObjectValue);
        Ranking.sortFacetValuesByRank(facetClassValue);
        for (FacetValue v : facetClassValue)
        	result.add(v);

        for (FacetValue v : facetObjectValue)
        	result.add(v);
        
        return result;
    }
    
    
  

    public static Response getDataForUnselectedValue(String searchKeywords, List<String> queryList, Configurations config) {
        Response answer = new Response();
        List<String> retrievedIds = new ArrayList<String>(getIdsFromFacets(config, queryList));
        List<String> keywordSearchIds = getIdsFromKeywordSearch(searchKeywords, config);
        retrievedIds.retainAll(keywordSearchIds);
        List<String> idsForPage = getIdsForPage(0, retrievedIds);
        List<Snippet> snipets = getSnippets(idsForPage, config);
        answer.setSize(retrievedIds.size());
        answer.setSnippets(snipets);
        //LOG.info("querylist: " + queryList.toString());
        return answer;
    }

    public static Response getDataForFocus(List<FacetValue> focusValues, String searchKeywords, List<String> queryList, Configurations config) {
        Response answer = new Response();
        List<String> retrievedIds = new ArrayList<String>();
        Set<FacetValue> specialCases = new HashSet<FacetValue>();
        for (FacetValue val : focusValues) {
            if (val.getType().equals(FacetValueEnum.OBJECT.toString())) {
                retrievedIds.add(val.getObject());
            } else if (val.getType().equals(FacetValueEnum.CLASS.toString())) {
                specialCases.add(val);
            }
        }
        if (specialCases.size() > 0) {
            Set<String> idsFromSpecialCases = getFocusIdsSpecialCases(specialCases, searchKeywords, queryList, config);
            for (String el : idsFromSpecialCases) {
                if (!retrievedIds.contains(el))
                    retrievedIds.add(el);
            }
        }
        List<String> idsForPage = getIdsForPage(0, retrievedIds);
        List<Snippet> snipets = getSnippets(idsForPage, config);
        answer.setSize(retrievedIds.size());
        answer.setSnippets(snipets);
        return answer;
    }

    public static Response getSnippets(int activePage, String searchKeywords, List<String> queryList, Configurations config) {
        Response answer = new Response();
        List<String> retrievedIds = new ArrayList<String>();
        List<String> keywordSearchIds = getIdsFromKeywordSearch(searchKeywords, config);
        if (queryList.size() > 0) {
            retrievedIds.addAll(getIdsFromFacets(config, queryList));
            retrievedIds.retainAll(keywordSearchIds);
        } else {
            retrievedIds = keywordSearchIds;
        }
        List<String> idsForPage = getIdsForPage(activePage, retrievedIds);
        List<Snippet> snipets = getSnippets(idsForPage, config);
        answer.setSize(retrievedIds.size());
        answer.setActivePage(activePage);
        answer.setSnippets(snipets);
        return answer;
    }

    public static Response getDataForHiding(FacetValue selectedValue, List<FacetValue> targetValues, List<FacetName> facetNames,
            List<FacetValue> selectedValues, String searchKeywords, List<String> queryList, Configurations config) {
        Set<String> retrievedIds = getIdsFromFacets(config, queryList);
        List<String> keywordSearchIds = getIdsFromKeywordSearch(searchKeywords, config);
        retrievedIds.retainAll(keywordSearchIds);
        String hideQuery;

        for (FacetName facetName : facetNames)
            facetName.setHidden(true);
                
        LOG.info("facet names in dataForHiding:");
        for(int i=0; i<facetNames.size(); i++){
        	LOG.info(facetNames.get(i));
        }
        
        List<FacetName> fNames = new ArrayList<FacetName>();
        for (FacetName facetName : facetNames) {
            for (String query : queryList) {
                //if (!isNested(selectedValue.getId()))
                    hideQuery = String.format("Select ?x where {%s . ?x <%s> ?z }", query, facetName.getName());
                //else
                  //  hideQuery = String.format("Select ?x where {%s. ?%s <%s> ?z }", query, selectedValue.getParentId(), facetName.getName());
                
                
                LOG.info("Query for hiding facet name " + facetName.getName() + ": " + hideQuery);
                boolean hide = QueryExecutor.hideValue(hideQuery, retrievedIds, config);
                if (!hide) {
                    facetName.setHidden(hide);
                }
            }
            fNames.add(facetName);
        }
        
        List<FacetValue> conjunctiveValues = new ArrayList<FacetValue>();
        List<FacetValue> fValues = new ArrayList<FacetValue>();
        Set<String> facetsWithTicks = new HashSet<String>();
        Set<String> facetValuesWithTicks = new HashSet<String>();
        List<FacetValue> valuesWithType = new ArrayList<FacetValue>();
        List<FacetValue> disjunctiveValues = new ArrayList<FacetValue>();

        
        for (FacetValue val : selectedValues)
            facetsWithTicks.add(val.getPredicate());
        
        for (FacetValue val : selectedValues)
        	facetValuesWithTicks.add(val.getObject());
        
        LOG.info("target (same level) facet values in dataForHiding: " + targetValues);
        LOG.info("selected facet values in dataForHiding: " + selectedValues);
    	//LOG.info("conjunctive predicates " + config.getConjunctivePredicates());
    	//LOG.info("selected values " + facetValuesWithTicks);

        for (FacetValue val : targetValues) {
            if ((!facetValuesWithTicks.contains(val.getObject()) && config.getConjunctivePredicates().contains(val.getPredicate())) || !facetsWithTicks.contains(val.getPredicate()) ) {
                val.setHidden(true);
                conjunctiveValues.add(val);
            }
            else {
            	
            	// Consider the facet values with "type" predicate
            	if(val.getPredicate().equals(config.getCategoryPredicate()))
            		valuesWithType.add(val);
            	else
            		disjunctiveValues.add(val);
            	
                val.setHidden(false);
    			fValues.add(val);
            }
        }
        
        for (FacetValue val : conjunctiveValues)
        	fValues.add(val);

        
        /*
         * Here the multithreading for updating the frequencies of the relevant facet values 
         * We split the list of facet values into three sub lists (Type, Disjunctive and Conjunctive).
         * Each sub list is partitioned based on the number of threads, exploiting the already present sorting
         * 
         * */
        int numberOfThreads = 4;
        int oracleSize = 0; 	// limit for updating counters in type predicate
        int oracleSize2 = 1000;	// limit for updating counters in Dis
        
        List<List<FacetValue>> matrixTypeValues = new ArrayList<List<FacetValue>>();
        List<List<FacetValue>> matrixConjunctiveValues = new ArrayList<List<FacetValue>>();
        List<List<FacetValue>> matrixDisjunctiveValues = new ArrayList<List<FacetValue>>();

        List<Thread> threadsTypeValues = new ArrayList<Thread>();
        List<Thread> threadsConjunctiveValues = new ArrayList<Thread>();
        List<Thread> threadsDisjunctiveValues = new ArrayList<Thread>();
        
        int sizeTypeValues = valuesWithType.size();
        int sizeDisjunctiveValues = disjunctiveValues.size();
        int sizeConjunctiveValues = conjunctiveValues.size();
        
        int count = 0;
        int count2 = 0;
        int count3 = 0;
        
        int partitionSizeTypeValues = sizeTypeValues/numberOfThreads;
        int restTypeValues = sizeTypeValues%numberOfThreads;
        int partitionSizeDisjunctiveValues = sizeDisjunctiveValues/numberOfThreads;
        int restDisjunctiveValues = sizeTypeValues%numberOfThreads;
        int partitionSizeConjunctiveValues = sizeConjunctiveValues/numberOfThreads;
        int restConjunctiveValues = sizeTypeValues%numberOfThreads;
        
        
        //LOG.info("number of type values: " + sizeTypeValues);
        //LOG.info("number of disjunctive values: " + sizeDisjunctiveValues);
        //LOG.info("number of conjunctive values: " + sizeConjunctiveValues);
        
        //We can consider 'sizeTypeValues' instead of retrievedIds
        if(retrievedIds.size() < oracleSize){
        	int index = sizeTypeValues-restTypeValues;
        	
        	//Type facet values
            for (int i = 0; i <= sizeTypeValues-numberOfThreads; i += partitionSizeTypeValues) {
            	if(i==0){
            		List<FacetValue> temp = new ArrayList<FacetValue>(
                        	valuesWithType.subList(index-partitionSizeTypeValues+1 , sizeTypeValues));
            		temp.add(valuesWithType.get(count)); 
            		matrixTypeValues.add(temp);
            	}
            	else{
            		List<FacetValue> temp = new ArrayList<FacetValue>(
                        	valuesWithType.subList(index-i-partitionSizeTypeValues+count+1 , index-i+count));
            		temp.add(valuesWithType.get(count));
            		matrixTypeValues.add(temp);
            	}
            	count++;
            }
        }
            
        if(sizeDisjunctiveValues < oracleSize2){

            //Disjunctive values
            int index2 = sizeDisjunctiveValues-restDisjunctiveValues;
            for (int i = 0; i <= sizeDisjunctiveValues-numberOfThreads; i += partitionSizeDisjunctiveValues) {
            	
            	if(i==0){
            		List<FacetValue> temp = new ArrayList<FacetValue>(
                        	disjunctiveValues.subList(index2- partitionSizeDisjunctiveValues +1 , sizeDisjunctiveValues));
                        	temp.add(disjunctiveValues.get(count2));
                        	matrixDisjunctiveValues.add(temp);
            	}
            	else{
                   	List<FacetValue> temp = new ArrayList<FacetValue>(
                        	disjunctiveValues.subList(index2-i-partitionSizeDisjunctiveValues+count2+1 , index2-i+count2));
                        	temp.add(disjunctiveValues.get(count2));
                        	matrixDisjunctiveValues.add(temp);
            	}
            	count2++;
            }
        }
         
            
            //Conjunctive values: they can be hidden after selection of the user
            int index3 = sizeConjunctiveValues-restConjunctiveValues;
            for (int i = 0; i <= sizeConjunctiveValues-numberOfThreads; i += partitionSizeConjunctiveValues) {
            	
                if(i==0){
            		List<FacetValue> temp = new ArrayList<FacetValue>(
            				conjunctiveValues.subList(index3- partitionSizeConjunctiveValues+1 , sizeConjunctiveValues));
                        	temp.add(conjunctiveValues.get(count3));
                        	matrixConjunctiveValues.add(temp);
            	}
            	else{
                   	List<FacetValue> temp = new ArrayList<FacetValue>(
                   			conjunctiveValues.subList(index3-i-partitionSizeConjunctiveValues+count3+1 , index3-i+count3));
                        	temp.add(conjunctiveValues.get(count3));
                        	matrixConjunctiveValues.add(temp);
            	}
            	count3++;
            }
        
        
        
        for(int i=0; i<numberOfThreads; i++){
        	
        	if(i < matrixTypeValues.size()){
        		Runnable r = new ScoreFacetValue(matrixTypeValues.get(i), selectedValues, retrievedIds,keywordSearchIds, null, config);
        		threadsTypeValues.add(new Thread(r));
        	}
        	if(i < matrixDisjunctiveValues.size()){
        		Runnable r2 = new ScoreFacetValue(matrixDisjunctiveValues.get(i), selectedValues, retrievedIds,keywordSearchIds, null, config);
            	threadsDisjunctiveValues.add(new Thread(r2));
        	}
        	if(i < matrixConjunctiveValues.size()){
            	Runnable r3 = new ScoreFacetValue(matrixConjunctiveValues.get(i),null, retrievedIds,keywordSearchIds, queryList, config);
            	threadsConjunctiveValues.add(new Thread(r3));
        	}        	
        }
        
        for(int i=0; i<numberOfThreads; i++){
        	if(i < threadsTypeValues.size())
        		threadsTypeValues.get(i).start();
        	if(i < threadsDisjunctiveValues.size())
        		threadsDisjunctiveValues.get(i).start();
        	if(i < threadsConjunctiveValues.size())
        		threadsConjunctiveValues.get(i).start();
        }

                
        try {
        	for(int i=0; i<numberOfThreads; i++){
            	if(i < threadsTypeValues.size())
            		threadsTypeValues.get(i).join();
            	if(i < threadsDisjunctiveValues.size())
            		threadsDisjunctiveValues.get(i).join();
            	if(i < threadsConjunctiveValues.size())
            		threadsConjunctiveValues.get(i).join();
            }
       	} catch (InterruptedException e) {
       		LOG.error(e);;
		}
        
        Response result = new Response(fNames, fValues);
        return result;
    }
    
        
    
    
    public static List<String> getIdsFromKeywordSearch(String searchString, Configurations config) {
        if (searchString.isEmpty() || searchString == null)
            return QueryExecutor.getAllSubjects(config);
        Set<String> allIds = new HashSet<String>();
        Set<String> intersectingIds = new HashSet<String>();
        String[] keywords = searchString.split(" ");
        SearchIndex searchIndex = config.getSearchIndex();

        for (String keyword : keywords) {
            allIds.addAll(searchIndex.getIdsForSearchKeyword(keyword));
        }

        intersectingIds.addAll(allIds);

        for (String keyword : keywords) {
            intersectingIds.retainAll(searchIndex.getIdsForSearchKeyword(keyword));
        }

        if (intersectingIds.size() > 0)
            allIds = intersectingIds;
        
        
        if (allIds.size() > config.getMaxSearchResuls()) {
            List<String> ids = new ArrayList<String>();
            int i = 0;
            for (String id : allIds) {
                if (i < config.getMaxSearchResuls())
                    ids.add(id);
                i++;
            }

            return ids;
        } else
            return new ArrayList<String>(allIds);
    }

    private static boolean isNested(String id) {
        return id.split("_").length > 3;
    }

    private static List<FacetName> getFacetNamesHybridAlg(FacetValue toggledFacetValue, List<String> queryList, Set<String> retrievedIds,
            Configurations config, int max_depth) {
        //List<String> initialIds = new ArrayList<String>(retrievedIds);
        List<FacetName> relevantFacetNames;
        Set<String> allFacetNames;
 
        if (toggledFacetValue != null)
            allFacetNames = QueryExecutor.getNestedPredicatesFromStore(queryList, toggledFacetValue, config);
        else
            allFacetNames = QueryExecutor.getPredicatesFromStore(queryList, config);
        
        relevantFacetNames = getFacetNamesMinimizeAlg(toggledFacetValue, queryList, retrievedIds, config, allFacetNames, max_depth);

        return relevantFacetNames;
    }
    
    
    
    
    //new version: multithreading integration
    /*
     * This methods allows to get the facet names in the facetes interface.
     * The RelevantFacetName thread allows to calculate the nesting feature of each facet name
     * and this nesting depth is stored in the ranking attributed of the FacetName
     * @return List<FacetName> where each facet predicate has the nesting score associated
     */
    public static List<FacetName> getFacetNamesMinimizeAlg(FacetValue toggledFacetValue, List<String> queryList, Set<String> retrievedIds,
            Configurations config, Set<String> allFacetNames, int max_depth) {
        
    	List<FacetName> relevantFacetNames = new ArrayList<FacetName>();
        List<String> allNames = new ArrayList<String>(allFacetNames);
        
        
        //	we split the facet Names for the threads
        // 'partition' is the number of facetNames per thread
        int size = allFacetNames.size();
        int partition = (int) Math.sqrt(size);
        
        List<List<String>> matrixNames = new ArrayList<List<String>>();
        List<Thread> threads = new ArrayList<Thread>();
        
        for (int i = 0; i < size; i += partition) {
        	matrixNames.add(allNames.subList(i,
                    Math.min(i + partition, size)));
        }
        
        for(List<String> list : matrixNames){
        	Runnable r = new RelevantFacetName(toggledFacetValue, list, relevantFacetNames, retrievedIds, queryList, config, max_depth);
        	Thread t = new Thread(r);
        	threads.add(t);
        	t.start();
        }
        
        for(Thread thread : threads){
        	try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
        return relevantFacetNames;
    }
    
    
    
    
    
    public static List<FacetName> getFacetNamesExaustiveAlg(FacetValue toggledFacetValue, List<String> queryList, Configurations config,
            List<String> initialIds, Set<String> allFacetNames) {
        List<FacetName> relevantFacetNames = new ArrayList<FacetName>();
        String completeQuery;
        
        for (String name : allFacetNames) {
            boolean empty = true;
            for (String query : queryList) {
                empty = true;
                for (String subject : initialIds) {
                    if (toggledFacetValue != null) {
                    	if (!query.contains(toggledFacetValue.getId()))
                            break;
                        completeQuery = String.format("SELECT DISTINCT ?z WHERE {%s . ?%s <%s> ?z . }", query.replace("?x", "<" + subject + ">"),
                                toggledFacetValue.getId(), name);
                        empty = QueryExecutor.isQueryEmpty(completeQuery, config);
                    } else {
                        completeQuery = String.format("SELECT DISTINCT ?z WHERE {%s. <%s> <%s> ?z }", query.replace("?x", "<" + subject + ">"),
                                subject, name);
                        empty = QueryExecutor.isQueryEmpty(completeQuery, config);
                    }
                    if (!empty) {
                        relevantFacetNames.add(new FacetName(name));
                        break;
                    }
                }
                if (!empty)
                    break;
            }

        }
        return relevantFacetNames;
    }
    
    
    private static List<FacetValue> getFacetValuesExaustiveAlg(String toggledFacetName, String parentFacetValueId, List<String> queryList,
            Set<String> retrievedIds, Configurations config) {
        List<FacetValue> facetValues = new ArrayList<FacetValue>();
        
        Map<String, Integer> map_facetClassValues = new HashMap<String, Integer>();
        Map<String, Integer> map_facetObjectValues = new HashMap<String, Integer>();
        
        for (String subject : retrievedIds) {
        	
        	for(String objectValue : QueryExecutor.getFacetObjectValues(queryList, subject, toggledFacetName, parentFacetValueId, config)){
        		if( map_facetObjectValues.get(objectValue) == null)
        			map_facetObjectValues.put(objectValue, 1);
    			else{
    				int number_of_individuals = map_facetObjectValues.get(objectValue);
        			number_of_individuals++;
        			map_facetObjectValues.put(objectValue, number_of_individuals);
    			}
        	}
        	
        	for(String classValues : QueryExecutor.getFacetClassValues(queryList, subject, toggledFacetName, parentFacetValueId, config)){
        		if( map_facetClassValues.get(classValues) == null)
    				map_facetClassValues.put(classValues, 1);
    			else{
    				int number_of_individuals = map_facetClassValues.get(classValues);
        			number_of_individuals++;
            		map_facetClassValues.put(classValues, number_of_individuals);
    			}
        	}
        	
        }
        
        facetValues.addAll(getFacetValueHierarchy(config, map_facetClassValues, map_facetObjectValues, false));
                
        return facetValues;
    }

    private static List<FacetValue> getFacetValuesHybridAlg(String toggledFacetName, String parentFacetValueId, List<String> queryList,
            Set<String> retrievedIds, Configurations config) {
        List<FacetValue> facetValues;
        /*
        int oracle = 10;
        if (retrievedIds.size() > oracle) {
            facetValues = getFacetValuesMinimizeAlg(toggledFacetName, queryList, retrievedIds, config);
        } else {
            facetValues = getFacetValuesExaustiveAlg(toggledFacetName, parentFacetValueId, queryList, retrievedIds, config);
        }
        */
        
        facetValues = getFacetValuesExaustiveAlg(toggledFacetName, parentFacetValueId, queryList, retrievedIds, config);
        return facetValues;
    }
    
    /*
    public static List<FacetValue> getFacetValuesMinimizeAlg(String toggledFacetName, List<String> queryList, Set<String> retrievedIds,
            Configurations config) {
    	LOG.info("querylist: " + queryList.get(0));
    	LOG.info("size of querylist: " + queryList.size());
    	LOG.info("size of retrieved ids: " + retrievedIds.size());
    	LOG.info("some retrieved ids: " + retrievedIds.iterator().next());
        List<FacetValue> facetValues = new ArrayList<FacetValue>();
        Set<String> allObjects = QueryExecutor.getObjectsFromStore(queryList, toggledFacetName, config);
        LOG.info("Done objects: " + allObjects.size());
        LOG.info("some objects: " + retrievedIds.iterator().next());
        Set<String> allClasses = QueryExecutor.getClassesFromStore(queryList, toggledFacetName, config);
        LOG.info("Done classes: " + allClasses.size());
        Set<String> facetClasses = new HashSet<String>();
        Set<String> facetObjects = new HashSet<String>();
        for (String value : allClasses) {
            boolean hide = true;
            for (String query : queryList) {
                hide = QueryExecutor.hideValue(
                        String.format("Select ?x where {%s. ?x <%s> ?any . ?any <%s> <%s> . }", query, toggledFacetName,
                                config.getCategoryPredicate(), value), retrievedIds, config);
                if (!hide) {
                    facetClasses.add(value);
                    break;
                }
            }
        }
        LOG.info("Done itereting over classes: ");
        for (String value : allObjects) {
            boolean hide = true;
            for (String query : queryList) {
                hide = QueryExecutor.hideValue(String.format("Select ?x where {%s. ?x <%s> <%s> }", query, toggledFacetName, value), retrievedIds,
                        config);
                if (!hide) {
                    facetObjects.add(value);
                    break;
                }
            }
        }
        LOG.info("Done iterating over objects: ");

        facetValues.addAll(getFacetValueHierarchy(config, facetClasses, facetObjects, false));
        return facetValues;
    }
    */
    

    private static Set<String> getFocusIdsSpecialCases(Set<FacetValue> classes, String searchKeywords, List<String> queryList, Configurations config) {
        Set<String> retrievedIds = new HashSet<String>();
        Set<String> keywordSearchIds = new HashSet<String>();
        keywordSearchIds.addAll(getIdsFromKeywordSearch(searchKeywords, config));
        for (FacetValue cl : classes) {
            for (String q : queryList) {
                if (q.contains(cl.getId())) {
                    String query = String.format("select distinct ?%s ?x where {  %s . }", cl.getId(), q);
                    retrievedIds.addAll(QueryExecutor.executeFocusOnClass(query, keywordSearchIds, config));
                }
            }
        }
        return retrievedIds;
    }

    private static List<String> getIdsForPage(int activePage, List<String> retrievedIds) {
        List<String> idsForPage = new ArrayList<String>();
        int i = 0;
        while (i != 10) {
            if (retrievedIds.size() > activePage * 10 + i)
                idsForPage.add(retrievedIds.get(activePage * 10 + i));
            else
                break;
            i++;
        }
        return idsForPage;
    }

    private static Set<String> getIdsFromFacets(Configurations config, List<String> queryList) {
        Set<String> result = new LinkedHashSet<String>();
        for (String q : queryList) {
            String facetQueryString = String.format("select ?x where { %s . }", q);
            result.addAll(QueryExecutor.executeSelectedFacetQuery(config.getTripleStore(), facetQueryString));
        }
        return result;
    }

    private static List<Snippet> getSnippets(List<String> idsForPage, Configurations config) {
        List<Snippet> snippets = new ArrayList<Snippet>();
        for (String subject : idsForPage) {
            Snippet snippet = new Snippet();
            snippet.setImage(QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getSnippetImagePredicate()));
            snippet.setUrl(QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getSnippetURLPredicate()));
            snippet.setDescription(QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getSnippetDescriptionPredicate()));
            snippet.setTitle(QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getSnippetTitlePredicate()));
            snippet.setExtra1(QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getSnippetExtra1Predicate()));
            snippet.setExtra2(QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getSnippetExtra2Predicate()));
            snippet.setExtra3(QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getSnippetExtra3Predicate()));
            snippet.setId(subject);
            try {

                String latitude = QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getLatitudePredicate());
                Float lat = Float.valueOf(latitude);
                snippet.setLat(lat);
                String longitude = QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getLongitudePredicate());
                Float lng = Float.valueOf(longitude);
                snippet.setLng(lng);
            } catch (Exception e) {
                snippet.setLat(null);
                snippet.setLng(null);
            }
            snippets.add(snippet);
        }
        return snippets;
    }

    public static List<Triple> getDataView(Configurations config) {
        List<Triple> allTriples = new ArrayList<Triple>();
        Set<String> predicates = QueryExecutor.getAllPredicates(config);
        for (String predicate : predicates) {
            Triple triple = new Triple();
            triple.setPredicate(predicate);
            QueryExecutor.setTripleInformation(triple, config);
            allTriples.add(triple);
        }

        return allTriples;
    }

}