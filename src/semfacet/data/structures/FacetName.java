/**
 * This class represents SemFacet facet name. It changes quite frequently over time.
 */

package semfacet.data.structures;

public class FacetName {
    private String id; // id is retrieved from the client side, currently it is
                       // not used in the server side
    private String name;
    private String label;
    private String type;
    private Float min;
    private Float max;
    private String sliderValue;
    private boolean hidden;

    public FacetName() {

    }

    public FacetName(String name, String label) {
        this.name = name;
        this.label = label;
        if (this.label == null) {
            String[] temp = name.split("/");
            this.label = temp[temp.length - 1];
        }
    }

    public FacetName(String name) {
        this.name = name;
        if (this.label == null) {
            String[] temp = name.split("/");
            this.label = temp[temp.length - 1];
        }
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (this.label == null) {
            String[] temp = name.split("/");
            this.label = temp[temp.length - 1];
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Float getMin() {
        return min;
    }

    public void setMin(Float min) {
        this.min = min;
    }

    public Float getMax() {
        return max;
    }

    public void setMax(Float max) {
        this.max = max;
    }

    public String getSliderValue() {
        return sliderValue;
    }

    public void setSliderValue(String sliderValue) {
        this.sliderValue = sliderValue;
    }

}
