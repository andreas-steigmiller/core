/**
 * This class was added to create search index. At the moment it is very basic. In general, it should be extended with more advanced functionality. 
 */

package semfacet.search;

import java.util.Collection;

import org.apache.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class SearchIndex {
    static Logger LOG = Logger.getLogger(SearchIndex.class.getName());
    protected Multimap<String, String> multimap = HashMultimap.create();

    public void addTextToSearchIndex(String id, String text, String delimiter) {
        String[] words = cleanText(text).split(delimiter);
        for (String word : words)
            if (!word.isEmpty())
                multimap.put(word, id);
    }

    protected static String cleanText(String text) {
        return text.replaceAll("[^a-zA-Z0-9 ]", " ").toLowerCase().trim();
    }

    public Collection<String> getIdsForSearchKeyword(String keyword) {
        return multimap.get(keyword.toLowerCase());
    }

    public void getSearchIndexStatistics() {
        LOG.info("Number of indexed words: " + multimap.keySet().size());
    }

}
