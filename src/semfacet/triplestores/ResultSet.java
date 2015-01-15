/**
 * This is a result set interface of the triple store, it has the most common methods among different triple stores used by SemFacet. This interface must be implemented if one wants to add a new triple store.
 * To add a new triple store we use adapter design pattern.  
 */

package semfacet.triplestores;

public interface ResultSet {
    void dispose();

    boolean hasNext();

    void next();

    void open();

    String getItem(int index);

    // most of the triple stores do not have this method. So it either should be
    // removed or its usage should be limited, or implemented from scratch using
    // some parser.
    boolean isIndividual(int index);
}