package semfacet.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import semfacet.data.enums.PredicateTypeEnum;
import semfacet.data.structures.Configurations;
import semfacet.data.structures.FacetName;
import semfacet.data.structures.FacetValue;
import semfacet.data.structures.Triple;
import semfacet.search.SearchIndex;
import semfacet.triplestores.ResultSet;
import semfacet.triplestores.Store;

public class QueryExecutor {
    static Logger LOG = Logger.getLogger(QueryExecutor.class.getName());

    public static String getAttributeForId(Store store, String id, String attributeName) {
        String result = "";
        String query = String.format("SELECT ?z WHERE { <%s> <%s> ?z }", id, attributeName);
        ResultSet tupleIterator = store.executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            if (tupleIterator.hasNext()) {
                result = tupleIterator.getItem(0);
            }

            tupleIterator.dispose();
        }
        return result;
    }

    public static List<String> executeFocusOnClass(String query, Set<String> luceneAnswers, Configurations config) {
        List<String> subjects = new ArrayList<String>();
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, false);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                if (luceneAnswers.contains(tupleIterator.getItem(1))) {
                    subjects.add(tupleIterator.getItem(0));
                }
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
        return subjects;
    }

    public static List<String> executeSelectedFacetQuery(Store store, String facetQueryString) {

        List<String> subjects = new ArrayList<String>();
        ResultSet tupleIterator = store.executeQuery(facetQueryString, false);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                subjects.add(tupleIterator.getItem(0));
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
        return subjects;
    }

    public static Multimap<String, String> mapCategoriesToIds(Configurations config) {
        Multimap<String, String> multimap = HashMultimap.create();
        String query = String.format("select ?x ?z where { ?x <%s> ?z } ", config.getCategoryPredicate());
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                multimap.put(tupleIterator.getItem(0), tupleIterator.getItem(1));
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
        return multimap;
    }

    public static Map<String, String> mapLabelsToIds(Configurations config) {
        Map<String, String> multimap = new HashMap<String, String>();
        ResultSet tupleIterator = config.getTripleStore().executeQuery(
                String.format("select ?x ?z where { ?x <%s> ?z } ", config.getLabelPredicate()), true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                multimap.put(tupleIterator.getItem(0), tupleIterator.getItem(1));
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
        return multimap;
    }

    public static Multimap<String, String> getHierarchyMap(Configurations config) {
        Set<String> roots = new HashSet<String>();
        Set<String> allElements = new HashSet<String>();
        Multimap<String, String> multimap = HashMultimap.create();

        ResultSet tupleIterator = config.getTripleStore().executeQuery(
                String.format("SELECT ?x ?z WHERE { ?x <%s> ?z } ", config.getHierarchyPredicate()), true);

        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                String subject = tupleIterator.getItem(0);
                String object = tupleIterator.getItem(1);
                multimap.put(object, subject);
                roots.add(object);
                if (roots.contains(subject)) {
                    roots.remove(subject);
                }
                allElements.add(subject);
                allElements.add(object);
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }

        tupleIterator = config.getTripleStore()
                .executeQuery(String.format("SELECT ?x ?z WHERE { ?x <%s> ?z } ", config.getCategoryPredicate()), true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                String subject = tupleIterator.getItem(0);
                String object = tupleIterator.getItem(1);
                if (!allElements.contains(object))
                    roots.add(object);
                multimap.put(object, subject);
                allElements.add(subject);
                allElements.add(object);
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }

        for (String root : roots) {
            if (!root.contains("owl:Thing") && !root.contains("owl#Thing"))
                multimap.put("owl:Thing", root);
        }

        // System.out.println(multimap.get("owl:Thing").toString());
        LOG.info("Number of objects in hierarchy: " + multimap.size());
        return multimap;
    }

    public static List<String> getSnippetIdsForCategories(List<String> categories, Configurations config) {
        ResultSet tupleIterator = null;
        List<String> snippetIds = new ArrayList<String>();

        for (String cat : categories) {
            tupleIterator = config.getTripleStore().executeQuery(
                    String.format("select distinct ?x where { ?x <%s> <%s> } ", config.getCategoryPredicate(), cat), true);
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                    snippetIds.add(tupleIterator.getItem(0));
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
        }

        return snippetIds;

    }

    public static void addDataToSearchIndex(SearchIndex searchIndex, String predicate, Store store) {
        Set<String> uniqueIds = new HashSet<String>();
        String query = String.format("select ?x ?y where { ?x <%s> ?y . } ", predicate);
        ResultSet tupleIterator = store.executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                try {
                    if (!uniqueIds.contains(tupleIterator.getItem(0)))
                        searchIndex.addTextToSearchIndex(tupleIterator.getItem(0), tupleIterator.getItem(1), " ");
                    uniqueIds.add(tupleIterator.getItem(0));
                } catch (Exception e) {
                    LOG.error("Corrupt data: " + e.getMessage());
                }
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
    }

    public static boolean hideValue(String query, Set<String> retrievedIds, Configurations config) {
        boolean hide = true;
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                if (retrievedIds.contains(tupleIterator.getItem(0))) {
                    hide = false;
                    break;
                }
                tupleIterator.next();
            }

            tupleIterator.dispose();
        }
        return hide;
    }

    public static Set<String> getPredicatesFromStore(List<String> queryList, Configurations config) {
        Set<String> facetNames = new HashSet<String>();
        ResultSet tupleIterator = null;
        for (String q : queryList) {
            String query = String.format("select DISTINCT ?y where { ?x ?y ?z . %s } ", q);
            tupleIterator = config.getTripleStore().executeQuery(query, true);
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                    String predicate = tupleIterator.getItem(0);
                    if (!config.getExcludedPredicates().contains(predicate)) {
                        facetNames.add(predicate);
                    }
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
        }
        return facetNames;
    }

    public static Set<String> getNestedPredicatesFromStore(List<String> queryList, FacetValue toggledFacetValue, Configurations config) {
        Set<String> facetNames = new HashSet<String>();
        ResultSet tupleIterator = null;
        for (String q : queryList) {
            if (q.contains(toggledFacetValue.getId())) {
                String query = String.format("select DISTINCT ?y where { ?%s ?y ?z . %s } ", toggledFacetValue.getId(), q);
                tupleIterator = config.getTripleStore().executeQuery(query, true);
                if (tupleIterator != null) {
                    tupleIterator.open();
                    while (tupleIterator.hasNext()) {
                        String predicate = tupleIterator.getItem(0);
                        if (!config.getExcludedPredicates().contains(predicate)) {
                            facetNames.add(predicate);
                        }
                        tupleIterator.next();
                    }
                    tupleIterator.dispose();
                }
            }
        }

        return facetNames;
    }

    public static Set<String> getObjectsFromStore(List<String> queryList, String toggledFacetName, Configurations config) {
        Set<String> facetValues = new HashSet<String>();
        ResultSet tupleIterator = null;
        for (String q : queryList) {
            String query = String.format("select DISTINCT ?z where { ?x <%s> ?z . %s } ", toggledFacetName, q);
            tupleIterator = config.getTripleStore().executeQuery(query, true);
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                    if (tupleIterator.isIndividual(0)) {
                        facetValues.add(tupleIterator.getItem(0));
                    }
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
        }
        return facetValues;
    }

    public static Set<String> getClassesFromStore(List<String> queryList, String toggledFacetName, Configurations config) {
        Set<String> facetValues = new HashSet<String>();
        ResultSet tupleIterator = null;
        for (String q : queryList) {
            String query = String.format("select DISTINCT ?z where { ?x <%s> ?any . ?any <%s> ?z . %s . } ", toggledFacetName,
                    config.getCategoryPredicate(), q);
            tupleIterator = config.getTripleStore().executeQuery(query, true);
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                    if (tupleIterator.isIndividual(0)) {
                        facetValues.add(tupleIterator.getItem(0));
                    }
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
        }
        return facetValues;
    }

    public static Set<String> getFacetObjectValues(List<String> queryList, String subject, String toggledFacetName, String parentFacetValueId,
            Configurations config) {
        Set<String> facetValues = new HashSet<String>();
        ResultSet tupleIterator = null;
        String objectQuery;
        for (String q : queryList) {
            if (parentFacetValueId == null)
                objectQuery = String.format("select DISTINCT ?z where { <%s> <%s> ?z . " + q.replace("?x", "<" + subject + ">") + "} ", subject,
                        toggledFacetName);
            else
                objectQuery = String.format("select DISTINCT ?z where { ?%s <%s> ?z . " + q.replace("?x", "<" + subject + ">") + "} ",
                        parentFacetValueId, toggledFacetName);

            if (parentFacetValueId == null || q.contains(parentFacetValueId)) {
                tupleIterator = config.getTripleStore().executeQuery(objectQuery, true);
                if (tupleIterator != null) {
                    tupleIterator.open();
                    while (tupleIterator.hasNext()) {
                        facetValues.add(tupleIterator.getItem(0));
                        tupleIterator.next();
                    }
                    tupleIterator.dispose();
                }
            }

        }
        return facetValues;
    }

    public static Set<String> getFacetClassValues(List<String> queryList, String subject, String toggledFacetName, String parentFacetValueId,
            Configurations config) {
        Set<String> facetValues = new HashSet<String>();
        ResultSet tupleIterator = null;
        String classQuery;
        for (String q : queryList) {
            if (parentFacetValueId == null)
                classQuery = String.format("select DISTINCT ?z where { <%s> <%s> ?x . ?x <%s> ?z . " + q.replace("?x", "<" + subject + ">") + "}",
                        subject, toggledFacetName, config.getCategoryPredicate());
            else
                classQuery = String.format("select DISTINCT ?z where { ?%s <%s> ?x . ?x <%s> ?z . " + q.replace("?x", "<" + subject + ">") + "}",
                        parentFacetValueId, toggledFacetName, config.getCategoryPredicate());
            if (parentFacetValueId == null || q.contains(parentFacetValueId)) {
                tupleIterator = config.getTripleStore().executeQuery(classQuery, true);
                if (tupleIterator != null) {
                    tupleIterator.open();
                    while (tupleIterator.hasNext()) {
                        facetValues.add(tupleIterator.getItem(0));
                        tupleIterator.next();
                    }
                    tupleIterator.dispose();
                }
            }
        }

        return facetValues;
    }

    public static boolean isQueryEmpty(String query, Configurations config) {
        boolean empty = true;
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            if (tupleIterator.hasNext()) {
                empty = false;
            }
            tupleIterator.dispose();
        }
        return empty;
    }

    public static Set<String> getAllPredicates(Configurations config) {
        Set<String> allPredicates = new HashSet<String>();
        String query = "SELECT DISTINCT ?y WHERE {?x ?y ?z}";
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                allPredicates.add(tupleIterator.getItem(0));
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
        return allPredicates;
    }

    public static void getDistinctSubjects(Configurations config) {
        String query = "SELECT DISTINCT ?x WHERE {?x ?y ?z}";
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            int count = 0;
            while (tupleIterator.hasNext()) {
                count++;
                // tupleIterator.getItem(0);
                tupleIterator.next();
            }
            LOG.info("Number of distinct subjects: " + count);
            tupleIterator.dispose();
        }
    }

    public static void setTripleInformation(Triple triple, Configurations config) {
        String query = String.format("SELECT ?x ?z WHERE {?x <%s> ?z}", triple.getPredicate());
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            if (tupleIterator.hasNext()) {
                triple.setSubject(tupleIterator.getItem(0));
                triple.setObject(tupleIterator.getItem(1));
            }
            tupleIterator.dispose();
        }

    }

    public static void addComments(Configurations config, String path) {
        String query = "SELECT DISTINCT ?x WHERE {?x ?y ?any . ?any <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?z}";
        //
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        File file = new File(path + "fly-extra-data.nt");
        BufferedWriter output;
        try {
            output = new BufferedWriter(new FileWriter(file));
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                    String subject = tupleIterator.getItem(0);
                    if (subject.startsWith("http://www.virtualflybrain.org/ontologies/individuals/")) {
                        output.write("<" + subject + ">  <http://www.w3.org/2000/01/rdf-schema#comment> \"random comment\"@en .\n");
                    }
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
            query = "SELECT ?x ?z WHERE {?x <http://xmlns.com/foaf/0.1/depicts> ?z}";
            tupleIterator = config.getTripleStore().executeQuery(query, true);
            if (tupleIterator != null) {
                tupleIterator.open();
                while (tupleIterator.hasNext()) {
                    String subject = tupleIterator.getItem(0);
                    String object = tupleIterator.getItem(1);
                    output.write("<" + object + ">  <http://dbpedia.org/ontology/thumbnail> <" + subject + "> .\n");
                    tupleIterator.next();
                }
                tupleIterator.dispose();
            }
            output.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static List<String> getAllSubjects(Configurations config) {
        List<String> allSubjects = new ArrayList<String>();
        String query = String.format("SELECT DISTINCT ?x WHERE {?x <%s> ?z}", config.getSnippetDescriptionPredicate());
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            int count = 0;
            while (tupleIterator.hasNext() && count < config.getMaxSearchResuls()) {
                allSubjects.add(tupleIterator.getItem(0));
                tupleIterator.next();
                count++;
            }
            tupleIterator.dispose();
        }
        return allSubjects;
    }

    public static void iterateResults(Configurations config, int iterations) {
        String query = String.format("SELECT ?x ?y ?z WHERE {?x ?y ?z}", config.getSnippetDescriptionPredicate());
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            int count = 0;
            while (tupleIterator.hasNext() && count < iterations) {
                tupleIterator.getItem(0);
                count++;
                tupleIterator.next();
            }
            System.out.println(count);
            tupleIterator.dispose();
        }
    }

    public static FacetName createPredicateWithDataType(String predicate, Configurations config) {
        FacetName facetName = new FacetName();
        facetName.setName(predicate);
        PredicateTypeEnum type = PredicateTypeEnum.INTEGER;
        String query = String.format("SELECT DISTINCT ?z WHERE {?x <%s> ?z}", predicate);
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        Float min = Float.POSITIVE_INFINITY;
        Float max = Float.NEGATIVE_INFINITY;
        Float number;
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext() && !type.equals(PredicateTypeEnum.UNKNOWN)) {
                String object = tupleIterator.getItem(0);
                if (type.equals(PredicateTypeEnum.INTEGER)) {
                    try {
                        number = (float) Integer.parseInt(object);
                        if (number > max)
                            max = number;
                        if (number < min)
                            min = number;
                    } catch (NumberFormatException e) {
                        type = PredicateTypeEnum.FLOAT;
                    }
                } else if (type.equals(PredicateTypeEnum.FLOAT)) {
                    try {
                        number = Float.parseFloat(object);
                        if (number > max)
                            max = number;
                        if (number < min)
                            min = number;
                    } catch (NumberFormatException e) {
                        type = PredicateTypeEnum.UNKNOWN;
                    }
                }
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
        if (!type.equals(PredicateTypeEnum.UNKNOWN)) {
            facetName.setMax(max);
            facetName.setMin(min);
        }
        facetName.setType(type.toString());
        return facetName;
    }

    public static void testQuery(Configurations config) {
        String query = "SELECT ?x ?z WHERE {?x <long> ?z . FILTER (?z >= \"0\")}";
        ResultSet tupleIterator = config.getTripleStore().executeQuery(query, true);
        if (tupleIterator != null) {
            tupleIterator.open();
            while (tupleIterator.hasNext()) {
                System.out.println(tupleIterator.getItem(0));
                tupleIterator.next();
            }
            tupleIterator.dispose();
        }
    }

}
