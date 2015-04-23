/**
 *  This class represents various information that is passed among different methods. In general, it captures most common parameters of different methods. In order, to keep the parameter count low, I added all of them in this class. 
 *  Maybe it is not the best way to deal with the situation, but I could not figure out anything better.
 */
package semfacet.data.structures;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.google.gson.annotations.Expose;

import semfacet.relational.db.Database;
import semfacet.search.SearchIndex;
import semfacet.triplestores.Store;

public class Configurations {

    @Expose
    private String snippetImagePredicate;
    @Expose
    private String snippetURLPredicate;
    @Expose
    private String snippetDescriptionPredicate;
    @Expose
    private String snippetTitlePredicate;
    @Expose
    private String snippetExtra1Predicate;
    @Expose
    private String snippetExtra2Predicate;
    @Expose
    private String snippetExtra3Predicate;
    @Expose
    private String categoryPredicate;
    @Expose
    private String hierarchyPredicate;
    @Expose
    private String labelPredicate;
    @Expose
    private String longitudePredicate;
    @Expose
    private String latitudePredicate;
    @Expose
    private String storeType;
    @Expose
    private int maxSearchResuls = Integer.MAX_VALUE;
    @Expose
    private boolean nesting;
    @Expose
    private Set<String> conjunctivePredicates;
    @Expose
    private Set<String> excludedPredicates;
    @Expose
    private Set<String> allPredicates;
    @Expose
    private long allTriples;
    @Expose
    private String memory;

    private String dataPath;
    private String ontologyPath;
    private Store tripleStore;
    private Database activityDatabase;
    private Multimap<String, String> idCategoryMap;
    private Multimap<String, String> hierarchyMap;
    private Map<String, String> idLabelMap;
    private Map<String, FacetName> facetTypeMap;
    private SearchIndex searchIndex;

    public String getLabelPredicate() {
        return labelPredicate;
    }

    public void setLabelPredicate(String labelPredicate) {
        this.labelPredicate = labelPredicate;
    }

    public Database getActivityDatabase() {
        return activityDatabase;
    }

    public void setActivityDatabase(Database activityDatabase) {
        this.activityDatabase = activityDatabase;
    }

    public Store getTripleStore() {
        return tripleStore;
    }

    public void setTripleStore(Store activeDatabase) {
        this.tripleStore = activeDatabase;
    }

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public Set<String> getAllPredicates() {
        return allPredicates;
    }

    public void setAllPredicates(Set<String> allPredicates) {
        this.allPredicates = allPredicates;
    }

    public long getAllTriples() {
        return allTriples;
    }

    public void setAllTriples(long allTriples) {
        this.allTriples = allTriples;
    }

    public Set<String> getConjunctivePredicates() {
        return conjunctivePredicates;
    }

    public void setConjunctivePredicates(Set<String> conjunctivePredicates) {
        this.conjunctivePredicates = conjunctivePredicates;
    }

    public boolean isNesting() {
        return nesting;
    }

    public void setNesting(boolean nesting) {
        this.nesting = nesting;
    }

    public SearchIndex getSearchIndex() {
        return searchIndex;
    }

    public void setSearchIndex(SearchIndex searchIndex) {
        this.searchIndex = searchIndex;
    }

    public String getCategoryPredicate() {
        return categoryPredicate;
    }

    public void setCategoryPredicate(String categoryPredicate) {
        this.categoryPredicate = categoryPredicate;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public int getMaxSearchResuls() {
        return maxSearchResuls;
    }

    public void setMaxSearchResuls(int maxSearchResuls) {
        this.maxSearchResuls = maxSearchResuls;
    }

    public String getSnippetImagePredicate() {
        return snippetImagePredicate;
    }

    public void setSnippetImagePredicate(String snippetImagePredicate) {
        this.snippetImagePredicate = snippetImagePredicate;
    }

    public String getSnippetURLPredicate() {
        return snippetURLPredicate;
    }

    public void setSnippetURLPredicate(String snippetURLPredicate) {
        this.snippetURLPredicate = snippetURLPredicate;
    }

    public String getSnippetDescriptionPredicate() {
        return snippetDescriptionPredicate;
    }

    public void setSnippetDescriptionPredicate(String snippetDescriptionPredicate) {
        this.snippetDescriptionPredicate = snippetDescriptionPredicate;
    }

    public Multimap<String, String> getIdCategoryMap() {
        return idCategoryMap;
    }

    public void setIdCategoryMap(Multimap<String, String> idCategoryMap) {
        this.idCategoryMap = idCategoryMap;
    }

    public Map<String, String> getIdLabelMap() {
        return idLabelMap;
    }

    public void setIdLabelMap(Map<String, String> idLabelMap) {
        this.idLabelMap = idLabelMap;
    }

    public String getLongitudePredicate() {
        return longitudePredicate;
    }

    public void setLongitudePredicate(String longitudePredicate) {
        this.longitudePredicate = longitudePredicate;
    }

    public String getLatitudePredicate() {
        return latitudePredicate;
    }

    public void setLatitudePredicate(String latitudePredicate) {
        this.latitudePredicate = latitudePredicate;
    }

    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    public String getOntologyPath() {
        return ontologyPath;
    }

    public void setOntologyPath(String ontologyPath) {
        this.ontologyPath = ontologyPath;
    }

    public Set<String> getExcludedPredicates() {
        return excludedPredicates;
    }

    public void setExcludedPredicates(Set<String> excludedPredicates) {
        this.excludedPredicates = excludedPredicates;
    }

    public String getHierarchyPredicate() {
        return hierarchyPredicate;
    }

    public void setHierarchyPredicate(String hierarchyPredicate) {
        this.hierarchyPredicate = hierarchyPredicate;
    }

    public Multimap<String, String> getHierarchyMap() {
        return hierarchyMap;
    }

    public void setHierarchyMap(Multimap<String, String> hierarchyMap) {
        this.hierarchyMap = hierarchyMap;
    }

    public String getSnippetTitlePredicate() {
        return snippetTitlePredicate;
    }

    public void setSnippetTitlePredicate(String snippetTitlePredicate) {
        this.snippetTitlePredicate = snippetTitlePredicate;
    }

    public String getSnippetExtra1Predicate() {
        return snippetExtra1Predicate;
    }

    public void setSnippetExtra1Predicate(String snippetExtra1Predicate) {
        this.snippetExtra1Predicate = snippetExtra1Predicate;
    }

    public String getSnippetExtra2Predicate() {
        return snippetExtra2Predicate;
    }

    public void setSnippetExtra2Predicate(String snippetExtra2Predicate) {
        this.snippetExtra2Predicate = snippetExtra2Predicate;
    }

    public String getSnippetExtra3Predicate() {
        return snippetExtra3Predicate;
    }

    public void setSnippetExtra3Predicate(String snippetExtra3Predicate) {
        this.snippetExtra3Predicate = snippetExtra3Predicate;
    }

    public Map<String, FacetName> getFacetTypeMap() {
        return facetTypeMap;
    }

    public void setFacetTypeMap(Map<String, FacetName> facetTypeMap) {
        this.facetTypeMap = facetTypeMap;
    }

    boolean pagodaToClassify = false, pagodaToCallHermiT = true; 
    
	public boolean getPAGOdAToClassify() {
		return pagodaToClassify;
	}

	public void setPAGOdAToClassify(boolean flag) {
		pagodaToClassify = flag;
	}

	public boolean getPAGOdAToCallHermiT() {
		return pagodaToCallHermiT;
	}

	public void setPAGOdAToCallHermiT(boolean flag) {
		pagodaToCallHermiT = flag;
	}
	
	String queryLogPath = "querylog"; 
	
	public String getQueryLogPath() {
		return queryLogPath;
	}

	public void setQueryLogPath(String path) {
		queryLogPath = path; 
	}

}
