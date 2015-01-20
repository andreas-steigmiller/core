/**
 * This enum is used for different ranking types, so it is easier later on to add new ones and handle them. 
 */
package semfacet.data.enums;

public enum RankingTypeEnum {
    ALPHABETICAL("0"),
    USERCLICK("1");

    private final String text;

    private RankingTypeEnum (final String text) {
        this.text = text;
    }

    public String toString() {
        return text;
    }

}
