/*
 * This class mainly deals with json objects sent from a client and converts them to a java objects.
 */
package semfacet.controler;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import semfacet.data.structures.Configurations;
import semfacet.data.structures.FacetName;
import semfacet.data.structures.FacetValue;
import semfacet.data.structures.Response;

public class ClientDataManager {
    static Logger LOG = Logger.getLogger(ClientDataManager.class.getName());

    public static List<FacetValue> getSelectedCheckboxes(String selectedValues) {
        List<FacetValue> values = new ArrayList<FacetValue>();
        JSONArray array;
        try {
            array = new JSONArray(selectedValues);
            for (int i = 0; i < array.length(); i++) {
                JSONObject ob = array.getJSONObject(i);
                FacetValue value = new FacetValue();
                value.setId(ob.get("id").toString());
                value.setPredicate(ob.get("predicate").toString());
                value.setObject(encodeFacetValues(ob.get("object").toString()));
                value.setType(ob.get("type").toString());
                value.setParent(ob.get("parent").toString());
                if (ob.has("parent_id"))
                    value.setParentId(ob.get("parent_id").toString());
                values.add(value);
            }
        } catch (JSONException e) {
            LOG.error(e.getMessage());
        }
        return values;
    }

    public static List<FacetName> getSliderValues(String rangeSliders) {
        List<FacetName> sliders = new ArrayList<FacetName>();
        if (rangeSliders == null)
            return sliders;
        JSONArray array;
        try {
            array = new JSONArray(rangeSliders);
            for (int i = 0; i < array.length(); i++) {
                JSONObject ob = array.getJSONObject(i);
                FacetName fName = new FacetName();
                fName.setId(ob.get("facet_id").toString());
                fName.setName(ob.get("facet_name").toString());
                fName.setSliderValue(ob.get("value").toString());
                sliders.add(fName);
            }
        } catch (JSONException e) {
            LOG.error(e.getMessage());
        }
        return sliders;
    }

    private static String encodeFacetValues(String value) {
        String temp = value.replace("(", "DDDDD");
        temp = temp.replace(")", "EEEEE");
        temp = temp.replace("&", "FFFFF");
        temp = temp.replace("'", "IIIII");
        return temp;
    }

    public static String getCompleteQuery(List<String> categories, List<FacetValue> values, Configurations config) {
        String facetQuery = "";
        if (!categories.isEmpty() && !values.isEmpty()) {
            facetQuery = FacetQueryConstructionManager.constructQuery(values, config) + " AND "
                    + FacetQueryConstructionManager.constructSubQueryFromCategories(categories, config);
        } else if (categories.isEmpty() && !values.isEmpty()) {
            facetQuery = FacetQueryConstructionManager.constructQuery(values, config);
        } else if (!categories.isEmpty() && values.isEmpty()) {
            facetQuery = FacetQueryConstructionManager.constructSubQueryFromCategories(categories, config);
        }
        return facetQuery;
    }

    public static List<FacetName> getFacetNames(String predicates) {
        List<FacetName> facetNames = new ArrayList<FacetName>();
        JSONArray array;
        try {
            array = new JSONArray(predicates);
            for (int i = 0; i < array.length(); i++) {
                JSONObject ob = array.getJSONObject(i);
                FacetName facetName = new FacetName();
                facetName.setId(ob.get("id").toString());
                facetName.setName(ob.get("name").toString());
                facetNames.add(facetName);
            }
        } catch (JSONException e) {
            LOG.error(e.getMessage());
        }
        return facetNames;
    }

    public static boolean isSelectedValue(FacetValue toggledFacetValue, List<FacetValue> values) {
        boolean selected = false;
        for (FacetValue v : values)
            if (toggledFacetValue.getId().equals(v.getId())) {
                selected = true;
                break;
            }
        return selected;
    }

    public static Response unhideAllValues(List<FacetValue> targetValues, List<FacetName> facetNames) {
        List<FacetValue> values = new ArrayList<FacetValue>();
        for (FacetValue val : targetValues) {
            val.setHidden(false);
            values.add(val);
        }
        List<FacetName> names = new ArrayList<FacetName>();
        for (FacetName n : facetNames) {
            n.setHidden(false);
            names.add(n);
        }
        return new Response(names, values);
    }

    public static FacetValue getFacetValueFromId(String toggledValueId, List<FacetValue> values) {
        FacetValue selectedFacetValue = null;
        for (FacetValue val : values) {
            if (val.getId().equals(toggledValueId)) {
                selectedFacetValue = val;
                break;
            }
        }
        return selectedFacetValue;
    }

}
