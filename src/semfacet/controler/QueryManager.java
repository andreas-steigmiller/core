package semfacet.controler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.google.common.collect.Sets;

import semfacet.data.enums.FacetValueEnum;
import semfacet.data.structures.Configurations;
import semfacet.data.structures.FacetName;
import semfacet.data.structures.FacetValue;
import semfacet.data.structures.Response;
import semfacet.data.structures.Snippet;
import semfacet.data.structures.Triple;
import semfacet.model.QueryExecutor;
import semfacet.ranking.Ranking;
import semfacet.ranking.RankingMap;
import semfacet.search.SearchIndex;

public class QueryManager {

    public static Response getInitialFacetData(String searchKeywords, Configurations config) {
        Response answer = new Response();
        List<String> retrievedIds = getIdsFromKeywordSearch(searchKeywords, config);
        List<String> idsForPage = getIdsForPage(0, retrievedIds);
        List<Snippet> snipets = getSnipets(idsForPage, config);

        String facetId = config.getCategoryPredicate();
        FacetName firstFacetName = new FacetName(facetId, config.getIdLabelMap().get(facetId));
        answer.setFirstFacetName(firstFacetName);
        answer.setSize(retrievedIds.size());
        answer.setSnippets(snipets);
        List<FacetValue> facetValues = getFirstFacetValues(retrievedIds, config);
        facetValues = removeSameLabelFacetValues(facetValues);
        answer.setFacetValues(facetValues);
        return answer;
    }

    /**
     * This method takes a list of facet values an input and removes the ones
     * that have the same label. There are cases where different facet values
     * have different ids, but the same labels. However, the user is presented
     * with the labels on the screen, so we decided to remove duplicates. This
     * is not a perfect solution, better one would change the label in the data.
     * 
     * @param facetValues
     *            is a list of facet values
     * @return a list of facetValues that have distinct labels
     */
    
    private static List<FacetValue> removeSameLabelFacetValues(List<FacetValue> facetValues) {
        Set<String> uniqueLabels = new HashSet<String>();
        List<FacetValue> uniqueFacetValues = new ArrayList<FacetValue>();

        for (FacetValue fv : facetValues) {
            if (!uniqueLabels.contains((fv.getLabel()))) {
                uniqueLabels.add(fv.getLabel());
                uniqueFacetValues.add(fv);
            }
        }

        return uniqueFacetValues;
    }

    public static List<FacetValue> getFirstFacetValues(List<String> retrievedIds, Configurations config) {
        Set<String> facetIds = new HashSet<String>();
        for (String subject : retrievedIds)
            facetIds.addAll(config.getIdCategoryMap().get(subject));

        List<FacetValue> facetValues = getFacetValueHierarchy(config, facetIds, new HashSet<String>(), true);
        Map<String, String> idLabelMap = config.getIdLabelMap();

        for (FacetValue fValue : facetValues) {
            if (idLabelMap.containsKey(fValue.getObject()))
                fValue.setLabel(idLabelMap.get(fValue.getObject()));
        }
        return facetValues;
    }

    // This method is a variation of BSF
    public static List<FacetValue> getFacetValueHierarchy(Configurations config, Set<String> facetClasses, Set<String> facetIndividuals,
            boolean invertType) {
        List<FacetValue> result = new ArrayList<FacetValue>();
        if (config.getHierarchyMap().size() == 0) {
            return getNonHierarcicalFacetValues(facetClasses, facetIndividuals, invertType);
        } else {
            Queue<FacetValue> queue = new LinkedList<FacetValue>();

            for (String item : config.getHierarchyMap().get("owl:Thing")) {
                if (!invertType) {
                    if (facetClasses.contains(item)) {
                        FacetValue root = new FacetValue(item, FacetValueEnum.CLASS.toString());
                        queue.add(root);
                    } else if (facetIndividuals.contains(item)) {
                        FacetValue root = new FacetValue(item, FacetValueEnum.OBJECT.toString());
                        queue.add(root);
                    }
                } else {
                    FacetValue root = new FacetValue(item, FacetValueEnum.OBJECT.toString());
                    queue.add(root);
                }
            }

            for (String individual : facetIndividuals) {
                if (!config.getHierarchyMap().containsValue(individual)) {
                    FacetValue root = new FacetValue(individual, FacetValueEnum.OBJECT.toString());
                    queue.add(root);
                }
            }

            while (!queue.isEmpty()) {
                FacetValue node = queue.remove();
                while (!facetClasses.contains(node.getObject()) && !facetIndividuals.contains(node.getObject()) && !queue.isEmpty())
                    node = queue.remove();
                if (facetClasses.contains(node.getObject()) || facetIndividuals.contains(node.getObject()))
                    result.add(node);
                if (!config.getHierarchyMap().get(node.getObject()).isEmpty()) {
                    for (String item : config.getHierarchyMap().get(node.getObject())) {
                        if (facetClasses.contains(item)) {
                            if (!invertType) {
                                FacetValue fValue = new FacetValue(item, FacetValueEnum.CLASS.toString());
                                fValue.setParent(node.getObject());
                                queue.add(fValue);
                            } else {
                                FacetValue fValue = new FacetValue(item, FacetValueEnum.OBJECT.toString());
                                fValue.setParent(node.getObject());
                                queue.add(fValue);
                            }
                        } else if (facetIndividuals.contains(item)) {
                            FacetValue fValue = new FacetValue(item, FacetValueEnum.OBJECT.toString());
                            fValue.setParent(node.getObject());
                            queue.add(fValue);
                        }
                    }
                }
            }

            /*
             * for (FacetValue fv : result) { System.out.println(fv.getParent()
             * + " " + fv.getObject()); }
             */

            return result;
        }
    }

    private static List<FacetValue> getNonHierarcicalFacetValues(Set<String> facetClasses, Set<String> facetIndividuals, boolean invertType) {
        List<FacetValue> result = new ArrayList<FacetValue>();
        for (String value : facetClasses) {
            if (!invertType) {
                result.add(new FacetValue(value, FacetValueEnum.CLASS.toString()));
            } else {
                result.add(new FacetValue(value, FacetValueEnum.OBJECT.toString()));
            }
        }
        for (String value : facetIndividuals)
            result.add(new FacetValue(value, FacetValueEnum.OBJECT.toString()));
        return result;
    }

    public static Response getInitialFacetNames(List<String> queryList, String searchKeywords, Configurations config) {
        Response answer = new Response();
        Set<String> retrievedIds = new HashSet<String>(QueryManager.getIdsFromFacets(config, queryList));
        Set<String> keywordSearchIds = new HashSet<String>(getIdsFromKeywordSearch(searchKeywords, config));
        retrievedIds = Sets.intersection(retrievedIds, keywordSearchIds);
        List<FacetName> facetNames = getFacetNamesHybridAlg(null, queryList, retrievedIds, config);
        Map<String, FacetName> facetTypeMap = config.getFacetTypeMap();
        Map<String, String> idLabelMap = config.getIdLabelMap();
        for (FacetName fn : facetNames) {
            if (idLabelMap.containsKey(fn.getName()))
                fn.setLabel(idLabelMap.get(fn.getName()));
            fn.setType(facetTypeMap.get(fn.getName()).getType());
            fn.setMin(facetTypeMap.get(fn.getName()).getMin());
            fn.setMax(facetTypeMap.get(fn.getName()).getMax());
        }
        Ranking.sortFacetNamesAlphabetically(facetNames, true);
        answer.setSize(retrievedIds.size());
        answer.setFacetNames(facetNames);
        return answer;
    }

    public static List<FacetValue> getFacetValues(String toggledFacetName, String parentFacetValueId, List<String> queryList, String searchKeywords,
            Configurations config) {
        Set<String> retrievedIds = new HashSet<String>(QueryManager.getIdsFromFacets(config, queryList));
        Set<String> keywordSearchIds = new HashSet<String>(getIdsFromKeywordSearch(searchKeywords, config));
        retrievedIds = Sets.intersection(retrievedIds, keywordSearchIds);
        List<FacetValue> facetValues = getFacetValuesHybridAlg(toggledFacetName, parentFacetValueId, queryList, retrievedIds, config);
        Map<String, Integer> rankingMap = RankingMap.getRankingMap(config, toggledFacetName, searchKeywords);
        Map<String, String> idLabelMap = config.getIdLabelMap();
        for (FacetValue v : facetValues) {
            if (idLabelMap.containsKey(v.getObject()))
                v.setLabel(idLabelMap.get(v.getObject()));
            if (rankingMap.containsKey(v.getObject()))
                v.setRanking(rankingMap.get(v.getObject()));
        }
        return facetValues;
    }

    public static Response getDataForSelectedValue(FacetValue toggledFacetValue, String searchKeywords, List<String> queryList, Configurations config) {
        Response answer = new Response();
        Set<String> retrievedIds = getIdsFromFacets(config, queryList);
        List<String> keywordSearchIds = getIdsFromKeywordSearch(searchKeywords, config);
        retrievedIds.retainAll(keywordSearchIds);
        List<FacetName> relevantFacetNames = new ArrayList<FacetName>();

        if (config.isNesting() && toggledFacetValue.getType().equals(FacetValueEnum.CLASS.toString()))
            relevantFacetNames = getFacetNamesHybridAlg(toggledFacetValue, queryList, retrievedIds, config);

        List<String> idsForPage = getIdsForPage(0, new ArrayList<String>(retrievedIds));
        List<Snippet> snipets = getSnipets(idsForPage, config);
        answer.setSize(retrievedIds.size());
        Map<String, FacetName> facetTypeMap = config.getFacetTypeMap();
        Map<String, String> idLabelMap = config.getIdLabelMap();
        for (FacetName fn : relevantFacetNames) {
            if (idLabelMap.containsKey(fn.getName()))
                fn.setLabel(idLabelMap.get(fn.getName()));
            fn.setType(facetTypeMap.get(fn.getName()).getType());
            fn.setMin(facetTypeMap.get(fn.getName()).getMin());
            fn.setMax(facetTypeMap.get(fn.getName()).getMax());
        }
        
        Ranking.sortFacetNamesAlphabetically(relevantFacetNames, false);
        answer.setFacetNames(relevantFacetNames);
        answer.setSnippets(snipets);
        return answer;
    }

    public static Response getDataForUnselectedValue(String searchKeywords, List<String> queryList, Configurations config) {
        Response answer = new Response();
        List<String> retrievedIds = new ArrayList<String>(getIdsFromFacets(config, queryList));
        List<String> keywordSearchIds = getIdsFromKeywordSearch(searchKeywords, config);
        retrievedIds.retainAll(keywordSearchIds);
        List<String> idsForPage = getIdsForPage(0, retrievedIds);
        List<Snippet> snipets = getSnipets(idsForPage, config);
        answer.setSize(retrievedIds.size());
        answer.setSnippets(snipets);
        return answer;
    }

    public static Response getDataForFocus(List<FacetValue> focusValues, String searchKeywords, List<String> queryList, Configurations config) {
        Response answer = new Response();
        List<String> retrievedIds = new ArrayList<String>();
        Set<FacetValue> specialCases = new HashSet<FacetValue>();
        for (FacetValue val : focusValues) {
            if (val.getType().equals(FacetValueEnum.OBJECT.toString())) {
                retrievedIds.add(val.getObject());
            } else if (val.getType().equals(FacetValueEnum.CLASS.toString())) {
                specialCases.add(val);
            }
        }
        if (specialCases.size() > 0) {
            Set<String> idsFromSpecialCases = getFocusIdsSpecialCases(specialCases, searchKeywords, queryList, config);
            for (String el : idsFromSpecialCases) {
                if (!retrievedIds.contains(el))
                    retrievedIds.add(el);
            }
        }
        List<String> idsForPage = getIdsForPage(0, retrievedIds);
        List<Snippet> snipets = getSnipets(idsForPage, config);
        answer.setSize(retrievedIds.size());
        answer.setSnippets(snipets);
        return answer;
    }

    public static Response getSnippets(int activePage, String searchKeywords, List<String> queryList, Configurations config) {
        Response answer = new Response();
        List<String> retrievedIds = new ArrayList<String>();
        List<String> keywordSearchIds = getIdsFromKeywordSearch(searchKeywords, config);
        if (queryList.size() > 0) {
            retrievedIds.addAll(getIdsFromFacets(config, queryList));
            retrievedIds.retainAll(keywordSearchIds);
        } else {
            retrievedIds = keywordSearchIds;
        }
        List<String> idsForPage = getIdsForPage(activePage, retrievedIds);
        List<Snippet> snipets = getSnipets(idsForPage, config);
        answer.setSize(retrievedIds.size());
        answer.setActivePage(activePage);
        answer.setSnippets(snipets);
        return answer;
    }

    public static Response getDataForHiding(FacetValue selectedValue, List<FacetValue> targetValues, List<FacetName> facetNames,
            List<FacetValue> selectedValues, String searchKeywords, List<String> queryList, Configurations config) {
        Set<String> retrievedIds = getIdsFromFacets(config, queryList);
        List<String> keywordSearchIds = getIdsFromKeywordSearch(searchKeywords, config);
        retrievedIds.retainAll(keywordSearchIds);
        String hideQuery;

        for (FacetName facetName : facetNames)
            facetName.setHidden(true);

        List<FacetName> fNames = new ArrayList<FacetName>();
        for (FacetName facetName : facetNames) {
            for (String query : queryList) {
                if (!isNested(selectedValue.getId()))
                    hideQuery = String.format("Select ?x where {%s . ?x <%s> ?z }", query, facetName.getName());
                else
                    hideQuery = String.format("Select ?x where {%s. ?%s <%s> ?z }", query, selectedValue.getParentId(), facetName.getName());
                boolean hide = QueryExecutor.hideValue(hideQuery, retrievedIds, config);
                if (!hide) {
                    facetName.setHidden(hide);
                }
            }
            fNames.add(facetName);
        }

        List<FacetValue> conjunctiveValues = new ArrayList<FacetValue>();
        List<FacetValue> fValues = new ArrayList<FacetValue>();
        Set<String> facetsWithTicks = new HashSet<String>();

        for (FacetValue val : selectedValues)
            facetsWithTicks.add(val.getPredicate());

        for (FacetValue val : targetValues) {
            if (config.getConjunctivePredicates().contains(val.getPredicate()) || !facetsWithTicks.contains(val.getPredicate())) {
                val.setHidden(true);
                conjunctiveValues.add(val);
            } else {
                val.setHidden(false);
                fValues.add(val);
            }
        }

        for (FacetValue val : conjunctiveValues) {
            for (String query : queryList) {
                boolean hide = false;
                String parent = "x";
                if (val.getParentId() != null && !val.getParentId().isEmpty())
                    parent = val.getParentId();
                if (val.getType().equals(FacetValueEnum.OBJECT.toString())) {
                    hideQuery = String.format("Select ?x where {%s. ?%s <%s> <%s> . }", query, parent, val.getPredicate(), val.getObject());
                    hide = QueryExecutor.hideValue(hideQuery, retrievedIds, config);

                } else if (val.getType().equals(FacetValueEnum.CLASS.toString())) {
                    hideQuery = String.format("Select ?x where {%s . ?%s <%s> ?any . ?any <%s> <%s> . }", query, parent, val.getPredicate(),
                            config.getCategoryPredicate(), val.getObject());
                    hide = QueryExecutor.hideValue(hideQuery, retrievedIds, config);
                }
                if (!hide) {
                    val.setHidden(hide);
                }
            }
            fValues.add(val);
        }

        Response result = new Response(fNames, fValues);

        return result;
    }

    public static List<String> getIdsFromKeywordSearch(String searchString, Configurations config) {
        if (searchString.isEmpty() || searchString == null)
            return QueryExecutor.getAllSubjects(config);
        Set<String> allIds = new HashSet<String>();
        Set<String> intersectingIds = new HashSet<String>();
        String[] keywords = searchString.split(" ");
        SearchIndex searchIndex = config.getSearchIndex();

        for (String keyword : keywords) {
            allIds.addAll(searchIndex.getIdsForSearchKeyword(keyword));
        }

        intersectingIds.addAll(allIds);

        for (String keyword : keywords) {
            intersectingIds.retainAll(searchIndex.getIdsForSearchKeyword(keyword));
        }

        if (intersectingIds.size() > 0)
            allIds = intersectingIds;

        if (allIds.size() > config.getMaxSearchResuls()) {
            List<String> ids = new ArrayList<String>();
            int i = 0;
            for (String id : allIds) {
                if (i < config.getMaxSearchResuls())
                    ids.add(id);
                i++;
            }

            return ids;
        } else
            return new ArrayList<String>(allIds);
    }

    private static boolean isNested(String id) {
        return id.split("_").length > 3;
    }

    private static List<FacetName> getFacetNamesHybridAlg(FacetValue toggledFacetValue, List<String> queryList, Set<String> retrievedIds,
            Configurations config) {
        List<String> initialIds = new ArrayList<String>(retrievedIds);
        List<FacetName> relevantFacetNames;
        Set<String> allFacetNames;

        if (toggledFacetValue != null)
            allFacetNames = QueryExecutor.getNestedPredicatesFromStore(queryList, toggledFacetValue, config);
        else
            allFacetNames = QueryExecutor.getPredicatesFromStore(queryList, config);

        int oracle = 100;
        if (retrievedIds.size() > oracle) {
            relevantFacetNames = getFacetNamesMinimizeAlg(toggledFacetValue, queryList, retrievedIds, config, allFacetNames);
        } else {
            relevantFacetNames = getFacetNamesExaustiveAlg(toggledFacetValue, queryList, config, initialIds, allFacetNames);
        }
        return relevantFacetNames;
    }

    public static List<FacetName> getFacetNamesMinimizeAlg(FacetValue toggledFacetValue, List<String> queryList, Set<String> retrievedIds,
            Configurations config, Set<String> allFacetNames) {
        List<FacetName> relevantFacetNames = new ArrayList<FacetName>();
        for (String name : allFacetNames) {
            boolean hide = true;
            for (String query : queryList) {
                if (toggledFacetValue != null) {
                    if (!query.contains(toggledFacetValue.getId()))
                        break;
                    hide = QueryExecutor.hideValue(String.format("Select ?x where {%s. ?%s <%s> ?z . }", query, toggledFacetValue.getId(), name),
                            retrievedIds, config);
                } else
                    hide = QueryExecutor.hideValue(String.format("Select ?x where {%s . ?x <%s> ?z }", query, name), retrievedIds, config);
                if (!hide) {
                    relevantFacetNames.add(new FacetName(name));
                    break;
                }
            }
        }
        return relevantFacetNames;
    }

    public static List<FacetName> getFacetNamesExaustiveAlg(FacetValue toggledFacetValue, List<String> queryList, Configurations config,
            List<String> initialIds, Set<String> allFacetNames) {
        List<FacetName> relevantFacetNames = new ArrayList<FacetName>();
        String completeQuery;
        for (String name : allFacetNames) {
            boolean empty = true;
            for (String query : queryList) {
                empty = true;
                for (String subject : initialIds) {
                    if (toggledFacetValue != null) {
                        completeQuery = String.format("SELECT DISTINCT ?z WHERE {%s . ?%s <%s> ?z . }", query.replace("?x", "<" + subject + ">"),
                                toggledFacetValue.getId(), name);
                        empty = QueryExecutor.isQueryEmpty(completeQuery, config);
                    } else {
                        completeQuery = String.format("SELECT DISTINCT ?z WHERE {%s. <%s> <%s> ?z }", query.replace("?x", "<" + subject + ">"),
                                subject, name);
                        empty = QueryExecutor.isQueryEmpty(completeQuery, config);
                    }
                    if (!empty) {
                        relevantFacetNames.add(new FacetName(name));
                        break;
                    }
                }
                if (!empty)
                    break;
            }

        }
        return relevantFacetNames;
    }

    private static List<FacetValue> getFacetValuesExaustiveAlg(String toggledFacetName, String parentFacetValueId, List<String> queryList,
            Set<String> retrievedIds, Configurations config) {
        List<FacetValue> facetValues = new ArrayList<FacetValue>();
        Set<String> facetValuesObjects = new HashSet<String>();
        Set<String> facetValuesClasses = new HashSet<String>();

        // Multiset would give counts
        for (String subject : retrievedIds) {
            facetValuesObjects.addAll(QueryExecutor.getFacetObjectValues(queryList, subject, toggledFacetName, parentFacetValueId, config));
            facetValuesClasses.addAll(QueryExecutor.getFacetClassValues(queryList, subject, toggledFacetName, parentFacetValueId, config));
        }

        /*
         * for (String value : facetValuesObjects) facetValues.add(new
         * FacetValue(value, FacetValueEnum.OBJECT.toString()));
         */
        facetValues.addAll(getFacetValueHierarchy(config, facetValuesClasses, facetValuesObjects, false));
        return facetValues;
    }

    private static List<FacetValue> getFacetValuesHybridAlg(String toggledFacetName, String parentFacetValueId, List<String> queryList,
            Set<String> retrievedIds, Configurations config) {
        List<FacetValue> facetValues;
        int oracle = 100;
        if (retrievedIds.size() > oracle) {
            facetValues = getFacetValuesMinimizeAlg(toggledFacetName, queryList, retrievedIds, config);
        } else {
            facetValues = getFacetValuesExaustiveAlg(toggledFacetName, parentFacetValueId, queryList, retrievedIds, config);
        }
        return facetValues;
    }

    public static List<FacetValue> getFacetValuesMinimizeAlg(String toggledFacetName, List<String> queryList, Set<String> retrievedIds,
            Configurations config) {
        List<FacetValue> facetValues = new ArrayList<FacetValue>();
        Set<String> allObjects = QueryExecutor.getObjectsFromStore(queryList, toggledFacetName, config);
        Set<String> allClasses = QueryExecutor.getClassesFromStore(queryList, toggledFacetName, config);
        Set<String> facetClasses = new HashSet<String>();
        Set<String> facetObjects = new HashSet<String>();
        for (String value : allClasses) {
            boolean hide = true;
            for (String query : queryList) {
                hide = QueryExecutor.hideValue(
                        String.format("Select ?x where {%s. ?x <%s> ?any . ?any <%s> <%s> . }", query, toggledFacetName,
                                config.getCategoryPredicate(), value), retrievedIds, config);
                if (!hide) {
                    facetClasses.add(value);
                    break;
                }
            }
        }
        for (String value : allObjects) {
            boolean hide = true;
            for (String query : queryList) {
                hide = QueryExecutor.hideValue(String.format("Select ?x where {%s. ?x <%s> <%s> }", query, toggledFacetName, value), retrievedIds,
                        config);
                if (!hide) {
                    facetObjects.add(value);
                    break;
                }
            }
        }

        facetValues.addAll(getFacetValueHierarchy(config, facetClasses, facetObjects, false));
        return facetValues;
    }

    private static Set<String> getFocusIdsSpecialCases(Set<FacetValue> classes, String searchKeywords, List<String> queryList, Configurations config) {
        Set<String> retrievedIds = new HashSet<String>();
        Set<String> keywordSearchIds = new HashSet<String>();
        keywordSearchIds.addAll(getIdsFromKeywordSearch(searchKeywords, config));
        for (FacetValue cl : classes) {
            for (String q : queryList) {
                if (q.contains(cl.getId())) {
                    String query = String.format("select distinct ?%s ?x where {  %s . }", cl.getId(), q);
                    retrievedIds.addAll(QueryExecutor.executeFocusOnClass(query, keywordSearchIds, config));
                }
            }
        }
        return retrievedIds;
    }

    private static List<String> getIdsForPage(int activePage, List<String> retrievedIds) {
        List<String> idsForPage = new ArrayList<String>();
        int i = 0;
        while (i != 10) {
            if (retrievedIds.size() > activePage * 10 + i)
                idsForPage.add(retrievedIds.get(activePage * 10 + i));
            else
                break;
            i++;
        }
        return idsForPage;
    }

    private static Set<String> getIdsFromFacets(Configurations config, List<String> queryList) {
        Set<String> result = new LinkedHashSet<String>();
        for (String q : queryList) {
            String facetQueryString = String.format("select ?x where { %s . }", q);
            result.addAll(QueryExecutor.executeSelectedFacetQuery(config.getTripleStore(), facetQueryString));
        }
        return result;
    }

    private static List<Snippet> getSnipets(List<String> idsForPage, Configurations config) {
        List<Snippet> snippets = new ArrayList<Snippet>();
        for (String subject : idsForPage) {
            Snippet snippet = new Snippet();
            snippet.setImage(QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getSnippetImagePredicate()));
            snippet.setUrl(QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getSnippetURLPredicate()));
            snippet.setDescription(QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getSnippetDescriptionPredicate()));
            snippet.setTitle(QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getSnippetTitlePredicate()));
            snippet.setExtra1(QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getSnippetExtra1Predicate()));
            snippet.setExtra2(QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getSnippetExtra2Predicate()));
            snippet.setExtra3(QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getSnippetExtra3Predicate()));
            snippet.setId(subject);
            try {

                String latitude = QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getLatitudePredicate());
                Float lat = Float.valueOf(latitude);
                snippet.setLat(lat);
                String longitude = QueryExecutor.getAttributeForId(config.getTripleStore(), subject, config.getLongitudePredicate());
                Float lng = Float.valueOf(longitude);
                snippet.setLng(lng);
            } catch (Exception e) {
                snippet.setLat(null);
                snippet.setLng(null);
            }
            snippets.add(snippet);
        }
        return snippets;
    }

    public static List<Triple> getDataView(Configurations config) {
        List<Triple> allTriples = new ArrayList<Triple>();
        Set<String> predicates = QueryExecutor.getAllPredicates(config);
        for (String predicate : predicates) {
            Triple triple = new Triple();
            triple.setPredicate(predicate);
            QueryExecutor.setTripleInformation(triple, config);
            allTriples.add(triple);
        }

        return allTriples;
    }

}