package semfacet.controler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import semfacet.data.enums.FacetValueEnum;
import semfacet.data.structures.Configurations;
import semfacet.data.structures.FacetName;
import semfacet.data.structures.FacetValue;

public class FacetQueryConstructionManager {

    // This function checks if a first is has more siblings given the second id
    // test: checkbox_0_3 checkbox_0_2_3_5 -> true (it means that checkbox_0_3
    // has a sibling checkbox_0_2)
    // test: checkbox_5_1 checkbox_0_3 ->false
    protected static boolean hasSiblings(String id1, String id2) {
        // remove everything after last _
        String s1 = id1.substring(0, id1.lastIndexOf("_"));
        String s2 = id2.substring(0, id2.lastIndexOf("_"));
        return s2.startsWith(s1);
    }

    // This function checks if given ids are siblings
    // test: checkbox_0_3 checkbox_0_2> true
    // test: checkbox_5_1 checkbox_0_3 ->false
    protected static boolean areSiblings(String id1, String id2) {
        String s1 = id1.substring(0, id1.lastIndexOf("_"));
        String s2 = id2.substring(0, id2.lastIndexOf("_"));
        return s1.equals(s2);
    }

    // This function returns the given level of the selected element
    // test: checkbox_0_3 -> 1 (root level)
    // test: checkbox_0_2_3_5 -> 2
    protected static int getLevel(String element) {
        int count = element.length() - element.replaceAll("_", "").length();
        return count / 2;
    }

    // This function adds missing given number of brackets (left/right) for a
    // given query
    // test: true, 2 -> "(("
    // test: false, 2-> "))"
    protected static String makeBrackets(boolean left, int n) {
        // since 0 is default value of char we can do as follows
        if (left) {
            return new String(new char[n]).replace("\0", "(");
        } else {
            return new String(new char[n]).replace("\0", ")");
        }
    }

    // This function checks if given id hadSiblings
    protected static boolean hadSiblings(List<FacetValue> elements, String element) {
        boolean siblings = false;
        for (int k = elements.size() - 1; k > 0; k--) {
            if (elements.get(k).getId().equals(element)) {
                break;
            }
            if (hasSiblings(elements.get(k).getId(), element)) {
                siblings = true;
                break;
            }
        }
        return siblings;
    }

    // This function adds missing brackets to query
    // test (a(b(c -> (a(b(c)))
    // test a)b)c) - >(((a)b)c)
    protected static String fixBrackets(String query) {
        int left = query.length() - query.replaceAll("\\(", "").length();
        int right = query.length() - query.replaceAll("\\)", "").length();
        if (left > right) {
            query += makeBrackets(false, left - right);
        } else if (right > left) {
            query = makeBrackets(true, right - left) + query;
        }
        return query;
    }

    // This function returns parent id of a given id
    // test: checkbox_1_0_1_2 -> checkbox_1_0
    protected static String getParentId(String id) {
        String s1 = id.substring(0, id.lastIndexOf("_"));
        return s1.substring(0, s1.lastIndexOf("_"));
    }

    protected static FacetValue getParentById(String id, List<FacetValue> values) {
        FacetValue result = null;
        for (FacetValue v : values) {
            if (v.getId().equals(id)) {
                result = v;
                break;
            }
        }
        return result;
    }

    protected static String getSubQuery(FacetValue value, List<FacetValue> values, Configurations config) {
        String query = "";
        String predicate = value.getPredicate();

        String object = value.getObject();
        String current_type = value.getType();
        String first_arg = "x";
        if (getLevel(value.getId()) != 1) {
            String parent_id = getParentId(value.getId());
            FacetValue parent = getParentById(parent_id, values);
            String parent_type = parent.getType();
            if (FacetValueEnum.OBJECT.toString().equals(parent_type)) {
                first_arg = "<" + parent.getObject() + ">";
            } else {
                first_arg = "?" + parent_id;
            }
        }

        if (getLevel(value.getId()) == 1) {
            query = "?";
        }
        if (current_type.equals(FacetValueEnum.OBJECT.toString())) {
            query += first_arg + "AAAAA<" + predicate + ">AAAAA<" + object + ">";
        } else if (current_type.equals("1")) {
            query += first_arg + "AAAAA<" + predicate + ">AAAAA?" + value.getId() + "AAAAAANDAAAAA?" + value.getId() + "AAAAA<"
                    + config.getCategoryPredicate() + ">AAAAA<" + object + ">";
        } else if (current_type.equals("2")) {
            query += first_arg + "AAAAA<" + predicate + ">AAAAA?" + value.getId();
        }
        return query;
    }

    protected static String[] getAllids(ArrayList<FacetValue> values) {
        String[] ids = new String[values.size()];
        int i = 0;
        for (FacetValue value : values) {
            ids[i] = value.getId();
            i++;
        }
        return ids;
    }

    public static String constructQuery(List<FacetValue> values, Configurations config) {
        selectLeafValuesFromHierarchy(values);
        String query = "";
        boolean root = true;
        int level = 1;
        String last = "";
        String qr = "";
        for (int i = values.size() - 1; i >= 0; i--) {
            qr = getSubQuery(values.get(i), values, config);
            if (root && values.size() > 1 && (last.equals("AND") || last.equals("")) && i > 0 && !hadSiblings(values, values.get(i).getId())) {
                query += makeBrackets(true, getLevel(values.get(i).getId()));
                root = false;
            }
            if (i > 0 && !hasSiblings(values.get(i).getId(), values.get(i - 1).getId())) {
                if (last.equals("AND") && hadSiblings(values, values.get(i).getId()) && (i < values.size())
                        && !areSiblings(values.get(i).getId(), values.get(i + 1).getId())) {
                    query += qr + "))";
                } else {
                    query += qr + ")";
                }
                if (!hadSiblings(values, values.get(i).getId()) && getLevel(values.get(i).getId()) == 1 && level == 1) {
                    query = "(" + query;
                    query = fixBrackets(query);
                }
                query += " AND ";
                if (!hadSiblings(values, values.get(i).getId()) && getLevel(values.get(i).getId()) == 1 && level == 1) {
                    query += "(";
                }
                last = "AND";
            } else if (i > 0 && areSiblings(values.get(i).getId(), values.get(i - 1).getId())) {
                if (getLevel(values.get(i).getId()) < level && last.equals("OR")) {
                    query += qr + ") " + getOperator(config.getConjunctivePredicates(), values.get(i).getPredicate()) + " ";
                } else {
                    query += qr + " " + getOperator(config.getConjunctivePredicates(), values.get(i).getPredicate()) + " ";
                }
                last = getOperator(config.getConjunctivePredicates(), values.get(i).getPredicate());
            } else if (i > 0 && hasSiblings(values.get(i).getId(), values.get(i - 1).getId())) {
                if (last.equals("AND")) {
                    query += qr + ") " + getOperator(config.getConjunctivePredicates(), values.get(i).getPredicate()) + " "
                            + makeBrackets(true, getLevel(values.get(i - 1).getId()));
                } else {
                    query += qr + " " + getOperator(config.getConjunctivePredicates(), values.get(i).getPredicate()) + " "
                            + makeBrackets(true, getLevel(values.get(i - 1).getId()));
                }
                last = getOperator(config.getConjunctivePredicates(), values.get(i).getPredicate());
            } else if (i == 0) {
                if (getLevel(values.get(i).getId()) <= level && values.size() > 2) {
                    if (getLevel(values.get(i).getId()) == level && last.equals("AND")) {
                        query += qr;
                    } else {
                        query += qr + ")";
                    }
                } else {
                    query += qr;
                }
                if (last.equals("AND") && hadSiblings(values, values.get(i).getId()) && (i < values.size())
                        && !hasSiblings(values.get(i).getId(), values.get(i + 1).getId())) {
                    query += ")";
                }
            }

            // initialize variables for condition checks
            if (getLevel(values.get(i).getId()) == 1) {
                root = true;
            }
            level = getLevel(values.get(i).getId());
        }

        query = fixBrackets(query);

        return query;
    }

    // This method ignores selected root values in hierarchy when leaves are
    // ticked
    private static void selectLeafValuesFromHierarchy(List<FacetValue> values) {
        Set<String> parents = new HashSet<String>();
        for (FacetValue fv : values) {
            parents.add(fv.getParent());
        }
        List<FacetValue> vals = new ArrayList<FacetValue>();
        for (FacetValue fv : values) {
            if (!parents.contains(fv.getObject()))
                vals.add(fv);
        }
        values.clear();
        values.addAll(vals);
    }

    private static String getOperator(Set<String> conjunctivePredicates, String predicate) {
        if (conjunctivePredicates.contains(predicate))
            return "AND";
        else
            return "OR";
    }

    public static String constructSubQueryFromCategories(List<String> categories, Configurations config) {
        StringBuffer subQuery = new StringBuffer();
        subQuery.append("( ");
        for (int i = 0; i < categories.size(); i++) {
            if (i != categories.size() - 1) {
                subQuery.append("?x <" + config.getCategoryPredicate() + "> <" + categories.get(i) + "> OR ");
            } else {
                subQuery.append("?x <" + config.getCategoryPredicate() + "> <" + categories.get(i) + "> )");
            }
        }
        return subQuery.toString();
    }

    public static void appendSliderQueries(List<String> queryList, List<FacetName> sliders) {
        for (FacetName fName : sliders) {
            String facetParent = getFacetParentId(fName.getId());
            for (int i = 0; i<queryList.size(); i++) {
                String query = queryList.get(i);
                if (query.contains(facetParent)) {
                    query += " . " + facetParent + " <" + fName.getName() + "> ?" + fName.getId() + " . FILTER (?" + fName.getId() + " >= \""
                            + fName.getSliderValue() + "\") ";
                    queryList.set(i, query);
                }
            }
        }

    }

    private static String getFacetParentId(String id) {
        int endIndex = id.lastIndexOf("_");
        if (endIndex == -1)
            return "?x";
        else
            return "?checkbox_" + id.substring(0, endIndex);
    }

}
