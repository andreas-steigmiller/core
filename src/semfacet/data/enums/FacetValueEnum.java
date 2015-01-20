/**
 * This enum is used for different facet value types, so it is easier later on to add new ones and handle them. 
 */
package semfacet.data.enums;

public enum FacetValueEnum {
    OBJECT("0"), CLASS("1");

    private final String text;

    private FacetValueEnum(final String text) {
        this.text = text;
    }

    public String toString() {
        return text;
    }

}
