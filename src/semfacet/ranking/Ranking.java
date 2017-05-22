/**
 * This class was created to rank facet names and facet values using some objective rankings i.e. alphabetically. One could extend this class to add different rankings if needed. 
 */

package semfacet.ranking;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import semfacet.data.structures.FacetName;
import semfacet.data.structures.FacetValue;

public class Ranking {
	static Logger LOG = Logger.getLogger(Ranking.class.getName());
	
    public static void sortFacetNamesAlphabetically(List<FacetName> facetNames, final boolean ascOrder) {
        if (facetNames.size() > 0) {
            Collections.sort(facetNames, new Comparator<FacetName>() {
                public int compare(final FacetName v1, final FacetName v2) {
                    if (ascOrder)
                        return v1.getName().compareTo(v2.getName());
                    else
                        return v2.getName().compareTo(v1.getName());
                }
            });
        }
    }
    
    public static void sortFacetNamesByRank(List<FacetName> facetNames) {
        if (facetNames.size() > 0) {
            Collections.sort(facetNames, new Comparator<FacetName>() {
                public int compare(final FacetName v1, final FacetName v2) {
                    if(v2.getRanking() > v1.getRanking()){
                    	return 1;
                    }
                    if(v2.getRanking() == v1.getRanking()){
                    	return 0;
                    }
                    else return -1;
                }
            });
        }
        
    }

    public static void sortFacetValuesAlphabetically(List<FacetValue> facetValues) {
        if (facetValues.size() > 0) {
            Collections.sort(facetValues, new Comparator<FacetValue>() {
                public int compare(final FacetValue v1, final FacetValue v2) {
                    return v1.getLabel().compareTo(v2.getLabel());
                }
            });
        }
    }

    public static void sortFacetValuesByRank(List<FacetValue> facetValues) {
        if (facetValues.size() > 0) {
            Collections.sort(facetValues, new Comparator<FacetValue>() {
                public int compare(final FacetValue v1, final FacetValue v2) {
                    return v2.getRanking() - v1.getRanking();
                }
            });
        }
        
    }
    
    
    
}
