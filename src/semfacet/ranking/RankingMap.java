/**
 * This class should retrieve various information related to facet ranking based on user activity. Maybe it should be renamed. Currently, it supports very basic methods. 
 */

package semfacet.ranking;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import semfacet.data.structures.Configurations;

public class RankingMap {
    public static Map<String, Integer> getRankingMap(Configurations config, String facetName, String keywords) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        try {
            String query = "SELECT facet_value, count(*) FROM user_activity WHERE facet_name ='" + facetName + "' AND keywords ='" + keywords
                    + "' GROUP BY facet_value";
            ResultSet rs = config.getActivityDatabase().executeQuery(query);
            for (; rs.next();) {
                String facetValue = rs.getString(1);
                Integer ranking = rs.getInt(2);
                map.put(facetValue, ranking);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

}
