/**
 * This class represents SemFacet facet value. It changes quite frequently over time.
 */

package semfacet.data.structures;

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
    private int ranking = 0; // ranking can be obtained from external db and
                             // computed using user clicks, then it can be used
                             // to order facet values

    public FacetValue(String object, String type) {
        this.object = object;
        this.type = type;
        if (this.label == null) {
            this.label = createLabel(object);
        }
    }

    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public FacetValue() {

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
    	return "object: " + object + " label: " + label + " predicate: " + predicate + " type: " + type + " ranking: " +ranking;
    }
}
