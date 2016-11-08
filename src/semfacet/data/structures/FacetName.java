/**
 * This class represents SemFacet facet name. It changes quite frequently over time.
 */

package semfacet.data.structures;

import java.time.*;

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
    private LocalDateTime minDateTime;
    private LocalDateTime maxDateTime;
    private String DateTimeMinValue;
    private String DateTimeMaxValue;
    private Integer numberOfDateTime;
    private Integer numberOfNumerics;
    private String IntgerMinValue;
    private String IntgerMaxValue;

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
    
    public Integer getNumberOfNumerics(){
    	return numberOfNumerics;
    }
    
    public void setNumberOfNumerics(Integer numberOfNumerics){
    	this.numberOfNumerics = numberOfNumerics;
    }

    public String getSliderValue() {
        return sliderValue;
    }

    public void setSliderValue(String sliderValue) {
        this.sliderValue = sliderValue;
    }
     
    
    public String getSliderIntegerMinValue() {
    	return IntgerMinValue;
    }
    
    public void setSliderIntegerMinValue(String IntegerMinValue) {
    	this.IntgerMinValue = IntegerMinValue;
    }
    
    public String getSliderIntegerMaxValue() {
    	return IntgerMaxValue;
    }
    
    public void setSliderIntegerMaxValue(String IntegerMaxValue) {
    	this.IntgerMaxValue = IntegerMaxValue;
    }
    
    
    public LocalDateTime getMinDateTime(){
    	return minDateTime;
    }
    
    public void setMinDateTime(LocalDateTime minDateTime){
    	this.minDateTime = minDateTime;
    }
        
    public LocalDateTime getMaxDateTime(){
    	return maxDateTime;
    }
    
    public void setMaxDateTime(LocalDateTime maxDateTime){
    	this.maxDateTime = maxDateTime;
    }
    
    public String getSliderDateTimeMaxValue(){
    	return DateTimeMaxValue;
    }
    
    public void setSliderDateTimeMaxValue(String DateTimeMaxValue){
    	this.DateTimeMaxValue = DateTimeMaxValue;
    }
    
    public String getSliderDateTimeMinValue(){
    	return DateTimeMinValue;
    }
    
    public void setSliderDateTimeMinValue(String DateTimeMinValue){
    	this.DateTimeMinValue = DateTimeMinValue;
    }
    
    public Integer getNumberOfDateTime(){
    	return numberOfDateTime;
    }
    
    public void setNumberOfDateTime(Integer numberOfDateTime){
    	this.numberOfDateTime = numberOfDateTime;
    }
    
}
