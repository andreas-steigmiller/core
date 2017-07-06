package semfacet.ranking;

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

import org.apache.log4j.Logger;

import com.google.common.collect.Sets;

import semfacet.cuncurrency.RelevantFacetName;
import semfacet.data.structures.Configurations;
import semfacet.data.structures.FacetName;

public class EvaluationRankingBrowsability implements Runnable{
	static Logger LOG = Logger.getLogger(EvaluationRankingBrowsability.class.getName());
	
	static String path = "/Users/lucagiacomelli/Desktop/università/thesis/code/Semfacet/core/evaluation/";
	
	public static enum Rank{
		ALPHABETICAL("A"), OTHER("New"), OPT_BROWSABILITY("OPT-BROWS");
		
		String type;
		private Rank(String type){
			this.type = type;
		}
		
		public String getType(){
			return type;
		}
	} 
	
	private String type;
	private String keywords;
	private List<FacetName> predicates;
	private Set<String> results; 
	private List<String> queryList;
	private Configurations config;
	private BufferedWriter writer;
	private int k_top;
	
	
	
	public EvaluationRankingBrowsability(String type, String keywords, List<FacetName> predicates, Set<String> results,
			Configurations config, List<String> queryList, int k_top){
		this.type = type;
		this.keywords = keywords;
		this.predicates = predicates;
		this.results = results;
		this.config = config;
		this.queryList = queryList;
		this.k_top = k_top;
		
		try {
			String object = "";
			for(String query: queryList){
				object += query.split(" ")[2];
			}
			writer = new BufferedWriter(new FileWriter(new File(path + k_top + keywords + "-"+ object + ".txt"  ), true));
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		
		
	}
	
	@Override
	public void run() {
		try {
			writer.write("###### EVALUATION TOP-"+ k_top+" TYPE:"+ type +" IN THE PAGE: " + keywords + " ##########\n\n");
			writer.write("SPARQL Query: \n");
			for(String query: queryList){
				writer.write(query +"\n");
			}
			writer.write("\nResults size: " + results.size() + "\n");
			
			writer.write("\n\nOrdering of facets:\n");
			for(int i=0; i<predicates.size(); i++){
				writer.write((i+1) + ". "+ predicates.get(i).getName() +"\n");
			}
			
			writer.write("\n\n----- UNIFORM STEPS: Equal-size results clusters ------\n");
			writer.write(getEqualSizeResultsClusters() + "");
			writer.write("\n\n----- MANY STEPS TO SINGLE ITEM: Maximal height ------\n");
			writer.write(getMaximalHeight() + "");
			writer.write("\n\n----- COMPREHENSIBLE RESULT PARTITIONING: Minimum result segment overlap ------\n");
			writer.write(getMinimumResultSegmentOverlap() + "");
			
			writer.write("\n\n\n\n\n");
			writer.close();
			
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
		
		
		
	
	}
	

    /**
     * This code is for evaluation of the ranking according to browse ability 
     * @param  names
     * 		a list of ranked facet predicates
     * @param k
     * 		a non negative integer for the top predicates
     * @param results
     * 		a set of current results from which we compute the score
     * @return the value for the metric of Uniform steps to item of interest
     * @see FacetName
     * 
     * **/
    public double getEqualSizeResultsClusters(){
    	double result = 0.0;
    	
    	double maxResults = 0;
    	double minResults = predicates.get(0).getAnswerSet().size();
    	
    	int end = Math.min(k_top, predicates.size());
    	
    	for(int i=0; i<end; i++){
    		if(predicates.get(i).getAnswerSet().size() > maxResults){
    			maxResults = predicates.get(i).getAnswerSet().size();
    		}
    		if(predicates.get(i).getAnswerSet().size() < minResults){
    			minResults = predicates.get(i).getAnswerSet().size();
    		}
    		
    		
    	}
    	
    	/*
    	try {
			writer.write("min: " + minResults);
			writer.write("max: " + maxResults);
			writer.write("results size: " + results.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
    	LOG.info("min: " + minResults);
		LOG.info("max: " + maxResults);
		LOG.info("results size: " + results.size());
		*/
    	
    	result = 1- (maxResults - minResults)/results.size();
    	return result;
    	
    }
    
    
    /**
     * This code is for evaluation of the ranking according to browse ability 
     * @param  names
     * 		a list of ranked facet predicates
     * @param k
     * 		a non negative integer for the top predicates
     * @param results
     * 		a set of current results from which we compute the score
     * @return the value for the metric of Minimum result segment overlap
     * @see FacetName
     * 
     * **/
    public double getMinimumResultSegmentOverlap(){
    	double result = 0.0;
    	
    	int end = Math.min(k_top, predicates.size());

    	Set<String> temp = new HashSet<String>();
    	Set<String> union = new HashSet<String>(predicates.get(0).getAnswerSet());
    	
    	for(int i=0; i<end; i++){
    		for(int j=0; j<end; j++){
    			if(i < j){
    				temp.addAll(Sets.intersection(predicates.get(i).getAnswerSet(), predicates.get(j).getAnswerSet()));
    			}
    		}
    		union.addAll(predicates.get(i).getAnswerSet());	
    	}
    	
    	//LOG.info("temp: " + temp);
    	//LOG.info("union: "+ union);
    	result = 1.0 - ((double)temp.size()/(double)union.size());
    	
    	return result;
    	
    }
    
    /**
     * This code is for evaluation of the ranking according to browse ability 
     * @param  names
     * 		a list of ranked facet predicates
     * @param k
     * 		a non negative integer for the top predicates
     * @param results
     * 		a set of current results from which we compute the score
     * @return the value for the metric of Maximal height
     * @see FacetName
     * **/
    public double getMaximalHeight(){
    	double result = 0.0;
    	int end = Math.min(k_top, predicates.size());
    	for(int i=0; i<end; i++){
    		FacetName fn = predicates.get(i);
    		result += RelevantFacetName.computeMaximalH(fn, fn.getAnswerSet().hashCode(), fn.getAnswerSet().size(),
    				queryList, 0, 0, config, results);
    	}
    	
       	return result;
    }
    
    
    
    /**
     * This code is for evaluation of the ranking according to browse ability 
     * @param  set of Facets
     * 		a list of facet predicates
     * @param k
     * 		a non negative integer for the top predicates
     * @param results
     * 		a set of current results from which we compute the score
     * @param d
     * 		maximum depth of each facet
     * @return the maximum score of all the possible orderings starting from the current results
     * @see FacetName
     * **/
    public static List<FacetName> maxOrdering(String keywords, List<String> queryList, List<FacetName> predicates, Set<String> results, int k_top){
    	List<FacetName> optimal_ordering = null;
    	
    	try {
			String object = "";
			for(String query: queryList){
				object += query.split(" ")[2];
			}
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(path + "MaxOrderingTOP-" + k_top + "-" + keywords + "-"+ object + ".txt"  ), true));
		
			FacetName[] array = new FacetName[predicates.size()];
	    	for(int i=0; i< predicates.size(); i++){
	    		array[i] = predicates.get(i);
	    	}
	    	ArrayList<ArrayList<FacetName>> permutations = permute(array);
	    	
	    	Map<List<FacetName>,Double> scores = new HashMap<List<FacetName>, Double>();
	    		    	
	    	for(List<FacetName> list : permutations){
	    		double score = calculateScoreObjFunction(k_top, list, results);
	       		scores.put(list, score);
	    	}
	    	
	    	double max = 0.0;
	    	
	    	
	    	//Now we calculate the ordering with the maximum score
	    	for(List<FacetName> facets : scores.keySet()){
	    		
	    		for(FacetName facet : facets)
	    			writer.write(facet.getName() + ", ");
	    		writer.write("--> score: " + scores.get(facets) + "\n");
	    		
	    		
	    		if(scores.get(facets) > max){
	    			max = scores.get(facets);
	    			optimal_ordering = facets;
	    		}
	    	}
	    	
	    	writer.write("\n\n\n#####OPTIMAL ORDERING: \n");
	    	for(FacetName facet : optimal_ordering)
    			writer.write(facet.getName() + ", ");
	    	writer.write(" with score: " + max);
	    	writer.close();
    	
    	} catch (Exception e) {
			LOG.error(e.getMessage());
		}
    	
    	
    	return optimal_ordering;
    } 
    
    
    
    // Here we calculate the score of each ordering according to the ranking function
    public static double calculateScoreObjFunction(int k_top, List<FacetName> list,
    		Set<String> results){
    	double score = 0.0;
		double dist = 0.0;
		int end = Math.min(k_top, list.size());
		
		for(int i=0; i<end; i++){
			FacetName facet = list.get(i);
			dist = dist(facet, list, results);
			score += ((facet.getRanking()/(double)(i+1))*dist);
			//LOG.info("dist of " + facet.getName() +": "+ dist);
			//LOG.info("h of " + facet.getName() +": "+ facet.getRanking());
			//LOG.info("partial final score:" + score);
		}
		
    	return score;
    }
    
    public static ArrayList<ArrayList<FacetName>> permute(FacetName[] num) {
    	ArrayList<ArrayList<FacetName>> result = new ArrayList<ArrayList<FacetName>>();
    	permute(num, 0, result);
    	return result;
    }
     
    static void permute(FacetName[] num, int start, ArrayList<ArrayList<FacetName>> result) {
    	if (start >= num.length) {
    		ArrayList<FacetName> item = convertArrayToList(num);
    		result.add(item);
    	}
     
    	for (int j = start; j <= num.length - 1; j++) {
    		swap(num, start, j);
    		permute(num, start + 1, result);
    		swap(num, start, j);
    	}
    }
     
    private static ArrayList<FacetName> convertArrayToList(FacetName[] num) {
    	ArrayList<FacetName> item = new ArrayList<FacetName>();
    	for (int h = 0; h < num.length; h++) {
    		item.add(num[h]);
    	}
    	return item;
    }
     
    private static void swap(FacetName[] a, int i, int j) {
    	FacetName temp = a[i];
    	a[i] = a[j];
    	a[j] = temp;
    }
    
    
    private static double dist(FacetName f, List<FacetName> predicates, Set<String> results){
    	Set<String> uncovered = new HashSet<String>(results);
    	
    	Set<String> answerSet = f.getAnswerSet();
    	Set<String> union = new HashSet<String>();
    	
    	double sum = 0.0;
    	double numerator = 0.0;
		double denominator = 0.0;
		double result = 0.0;

    	for(FacetName predicate : predicates){
    		
    		if(predicate.getName() != f.getName()){
    			union.addAll(predicate.getAnswerSet());
    			uncovered.removeAll(union);
    		}

    		numerator = Sets.intersection(answerSet, uncovered).size();
			denominator = answerSet.size();
			sum += numerator/denominator;
    	}
    	result = (sum/(double)predicates.size());
    	return result;
    }

     
   

}
