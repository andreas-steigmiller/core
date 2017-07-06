/**
 * This class represents SemFacet facet value. It changes quite frequently over time.
 */

package semfacet.data.structures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// TODO: pick better names for facet value properties
public class FacetValue {
    private String id;// id provided by the client side
    private String parentId; // parent in nesting provided by the client side
    private String predicate; // facet name - predicate retrieved from triple
                              // store
    private String object; // facet value name - uri retrieved from triple store
    private String parent; // parent facet value name in hierarchical structure
    private String label;
    private String type; // see FacetValueEnum
    private boolean isHidden;
    private int ranking = 0; //we adopt count-based ranking
    private List<String> queryList_reachability;
    private Set<String> answer_set_on_selection;



	public FacetValue(String object, String type) {
        this.object = object;
        this.type = type;
        if (this.label == null) {
            this.label = createLabel(object);
        }
        this.queryList_reachability = new ArrayList<String>();
        this.answer_set_on_selection = new HashSet<String>();
    }
	
    public FacetValue() {}

    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }
    
    public List<String> getQuery_reachability() {
		return queryList_reachability;
	}
    
    public void addQueryReachability(String query){
		this.queryList_reachability.add(query);
	}

	public void setQuery_reachability(List<String> query_reachability) {
		this.queryList_reachability = query_reachability;
	}
	
	public Set<String> getAnswer_set_on_selection() {
		return answer_set_on_selection;
	}

	public void answerSetOnSelection(Set<String> answer_set_on_selection) {
		this.answer_set_on_selection = answer_set_on_selection;
	}

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean isHidden) {
        this.isHidden = isHidden;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
        if (this.label == null) {
            this.label = createLabel(object);
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    private String createLabel(String object) {
        String[] temp = object.split("/");
        String[] temp1 = temp[temp.length - 1].split("#");
        return temp1[temp1.length - 1];
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }
    
    @Override
    public String toString(){
    	return object +  " type: " + type + " count: " +ranking;
    }
    
    @Override
    public int hashCode() {
        return this.getObject().hashCode() + this.getLabel().hashCode();
    }

    
    @Override
    public boolean equals(Object o){
    	if(o == null || !(o instanceof FacetValue))
    		return false;
    	FacetValue fv = (FacetValue) o;
    	if(this.getParent() == null && fv.getParent() == null)
    		return fv.getObject().equals(this.getObject()) && fv.getLabel().equals(this.getLabel());
    	else if (this.getParent() == null &&  fv.getParent() != null)
    		return false;
    	else if (this.getParent() != null &&  fv.getParent() == null)
    		return false;
    	
    	return fv.getObject().equals(this.getObject()) && fv.getLabel().equals(this.getLabel())
    			&& this.getParent().equals(fv.getParent());
    }
    
}
