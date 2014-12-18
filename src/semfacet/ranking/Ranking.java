package semfacet.ranking;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import semfacet.data.structures.FacetName;
import semfacet.data.structures.FacetValue;

public class Ranking {
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
