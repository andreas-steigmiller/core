package semfacet.triplestores;

public interface ResultSet {
    void dispose();

    boolean hasNext();

    void next();

    void open();

    String getItem(int index);

    boolean isIndividual(int index);
}