package semfacet.data.enums;

public enum FacetValueEnum {
    OBJECT("0"),
    CLASS("1");

    private final String text;

    private FacetValueEnum (final String text) {
        this.text = text;
    }

    public String toString() {
        return text;
    }

}
