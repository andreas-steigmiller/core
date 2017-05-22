/**
 * 
 * Semfacet(c) Copyright University of Oxford, 2014. All Rights Reserved.
 *
 *
 * This class is the starting point of the SemFacet web service. It can be accessed using the following url "http://localhost:8080/semFacet/rest/WebService/".
 * The web service url can be changed by modifying pom.xml and this class. To check if the service is running correctly one could test it in a web browser,
 * by providing sub urls and parameters of interest. For example, "http://localhost:8080/semFacet/rest/WebService/getInitialFacet?search_keywords=test".
 *
 */

package semfacet.webService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import semfacet.controler.ClientDataManager;
import semfacet.controler.FacetQueryConstructionManager;
import semfacet.controler.QueryManager;
import semfacet.controler.Utils;
import semfacet.data.structures.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import semfacet.data.init.DataContextListener;
import semfacet.external.operations.ExternalUtils;
import semfacet.external.operations.MemoryCheck;
import semfacet.model.QueryExecutor;
import semfacet.triplestores.Store;
import semfacet.triplestores.StoreException;

@Path("/WebService")
public class FeedService {
    @Context
    ServletContext context;

    static Logger LOG = Logger.getLogger(FeedService.class.getName());

    /**
     * This method returns an initial facet and snippets based on the multiple
     * keyword search. First, it retrieves all objects that have searchKeywors
     * in SNIPPET_DESCRIPTION_PREDICATE field(s), which can be specified in
     * sys.config file. Then, the method creates a set of snippets and the
     * initial facet for those object. The initial facet predicate is described
     * in sys.conf file using CATEGORY_PREDICATE variable. The main reason to
     * display only one facet at this phase is to ensure faster performance of
     * the system, because having all facets and their values might add too much
     * complexity for the end users, so there is no point to waste resources in
     * their computation.
     * 
     * @param searchKeywords
     *            a set of keywords provided by a client application
     * @return Response in Json format, which contains initial facet and
     *         snippets
     * @see FacetName
     * @see FacetValue
     * @see Snippet
     * @see Response
     */

    @GET
    @Path("/getInitialFacet")
    @Produces("application/json")
    public String getInitialFacet(@QueryParam("search_keywords") String searchKeywords) {
        Configurations config = (Configurations) context.getAttribute(DataContextListener.CONFIGURATIONS);
        Response result = QueryManager.getInitialFacetData(searchKeywords, config);
        Gson gson = new Gson();
        return gson.toJson(result);
    }

    /**
     * This method returns a set of facet names based on a set of keywords and
     * selected facet values provided by the client application. At this point
     * retrieved facet names do not have any facet values attached, because it
     * might be that some of the facet names are not interesting for the end
     * users, so it is better to save computation time and obtain facet values
     * for selected facet names on demand.
     * 
     * @param searchKeywords
     *            a set of keywords provided by a client application
     * @param selectedValues
     *            a set of facet values provided by a client application
     * @return Response in Json format, which contains a set of facet names
     * @see FacetName
     * @see Response
     */

    @GET
    @Path("/getFacetNames")
    @Produces("application/json")
    public String getFacetNames(@QueryParam("search_keywords") String searchKeywords, @QueryParam("selected_facet_values") String selectedValues,
            @QueryParam("range_sliders") String rangeSliders, @QueryParam("datetime_sliders") String rangedatetimeSliders) {
        Configurations config = (Configurations) context.getAttribute(DataContextListener.CONFIGURATIONS);
        List<FacetValue> values = ClientDataManager.getSelectedCheckboxes(selectedValues);
        List<FacetName> sliders = ClientDataManager.getSliderValues(rangeSliders);
        List<FacetName> datetimesliders = ClientDataManager.getSliderDateTimeValues(rangedatetimeSliders);
        String facetQuery = FacetQueryConstructionManager.constructQuery(values, config);        
        List<String> queryList = ExternalUtils.parseQuery(facetQuery);
        FacetQueryConstructionManager.appendSliderQueries(queryList, sliders);
        FacetQueryConstructionManager.appendDateTimeQueries(queryList, datetimesliders);
        Response result = QueryManager.getInitialFacetNames(queryList, searchKeywords, config);
        Gson gson = new Gson();
        return gson.toJson(result);
    }

    /**
     * This method returns a set of facet values for a given facetName based on
     * a set of searchKeywords and selectedValues provided by the client
     * application. parentFacetValueId variable is used to retrieve relevant
     * facet values in nested facets.
     * 
     * @param searchKeywords
     *            a set of keywords provided by a client application
     * @param selectedValues
     *            a set of facet values provided by a client application
     * @param facetName
     *            a facet name provided by a client application
     * @param parentFacetValueId
     *            a parent facet value id provided by a client application
     * @return Response in Json format, which contains a set of facetValues
     * @see FacetValue
     * @see Response
     */

    @GET
    @Path("/getFacetValues")
    @Produces("application/json")
    public String getFacetValues(@QueryParam("search_keywords") String searchKeywords, @QueryParam("selected_facet_values") String selectedValues,
            @QueryParam("range_sliders") String rangeSliders, @QueryParam("datetime_sliders") String rangedatetimeSliders, @QueryParam("toggled_facet_name") String facetName,
            @QueryParam("parent_checkbox_id") String parentFacetValueId) {
        if (parentFacetValueId.equals("null"))
            parentFacetValueId = null;
        Configurations config = (Configurations) context.getAttribute(DataContextListener.CONFIGURATIONS);
        List<FacetValue> values = ClientDataManager.getSelectedCheckboxes(selectedValues);
        List<FacetName> sliders = ClientDataManager.getSliderValues(rangeSliders);
        List<FacetName> datetimesliders = ClientDataManager.getSliderDateTimeValues(rangedatetimeSliders);
        String facetQuery = FacetQueryConstructionManager.constructQuery(values, config);
        List<String> queryList = ExternalUtils.parseQuery(facetQuery);
        FacetQueryConstructionManager.appendSliderQueries(queryList, sliders);
        FacetQueryConstructionManager.appendDateTimeQueries(queryList, datetimesliders);
        List<FacetValue> result = QueryManager.getFacetValues(facetName, parentFacetValueId, queryList, searchKeywords, config);        
        Gson gson = new Gson();
        return gson.toJson(result);
    }

    /**
     * This method returns a set of snippets based on a set of searchKeywords
     * and selectedValues provided by the client application. If toggledValueId
     * is "class" and nesting is enabled, then this method also returns a list
     * of facet names associated to toggledValueId. Selected facet value is
     * loged in user activity table, later this information can be used to rank
     * and order facet values.
     * 
     * @param searchKeywords
     *            a set of keywords provided by a client application
     * @param selectedValues
     *            a set of facet values provided by a client application
     * @param toggledValueId
     *            a toggled facet value id provided by a client application
     * @return Response in Json format, which contains a set of snippets. If
     *         toggledValueId is "class" and nesting is enabled, then this
     *         method also returns a list of facet names associated to
     *         toggledValueId
     * @see FacetName
     * @see Snippet
     * @see Response
     */

    @GET
    @Path("/getSelectedFacetValueData")
    @Produces("application/json")
    public String getSelectedFacetValueData(@QueryParam("search_keywords") String searchKeywords,
            @QueryParam("selected_facet_values") String selectedValues, @QueryParam("range_sliders") String rangeSliders,@QueryParam("datetime_sliders") String rangedatetimeSliders,
            @QueryParam("toggled_facet_value_id") String toggledValueId) {
        Configurations config = (Configurations) context.getAttribute(DataContextListener.CONFIGURATIONS);
        List<FacetValue> values = ClientDataManager.getSelectedCheckboxes(selectedValues);
        List<FacetName> sliders = ClientDataManager.getSliderValues(rangeSliders);
        List<FacetName> datetimesliders = ClientDataManager.getSliderDateTimeValues(rangedatetimeSliders);
        String facetQuery = FacetQueryConstructionManager.constructQuery(values, config);
        List<String> queryList = ExternalUtils.parseQuery(facetQuery);
        FacetQueryConstructionManager.appendSliderQueries(queryList, sliders);
        FacetQueryConstructionManager.appendDateTimeQueries(queryList, datetimesliders);
        FacetValue selectedFacetValue = ClientDataManager.getFacetValueFromId(toggledValueId, values);
        Response result = QueryManager.getDataForSelectedValue(selectedFacetValue, searchKeywords, queryList, config);
        Utils.logUserActivity(searchKeywords, config, selectedFacetValue);
        Gson gson = new Gson();
        LOG.info("result: " + result);
        return gson.toJson(result);
    }

    /**
     * This method returns a set of snippets based on a set of searchKeywords
     * and selectedValues provided by the client application.
     * 
     * @param searchKeywords
     *            a set of keywords provided by a client application
     * @param selectedValues
     *            a set of facet values provided by a client application
     * @return Response in Json format, which contains a set of snippets.
     * @see Snippet
     * @see Response
     */

    @GET
    @Path("/getUnselectedFacetValueData")
    @Produces("application/json")
    public String getUnselectedFacetValueData(@QueryParam("search_keywords") String searchKeywords,
            @QueryParam("selected_facet_values") String selectedValues, @QueryParam("range_sliders") String rangeSliders,@QueryParam("datetime_sliders") String rangedatetimeSliders) {
        Configurations config = (Configurations) context.getAttribute(DataContextListener.CONFIGURATIONS);
        List<FacetValue> values = ClientDataManager.getSelectedCheckboxes(selectedValues);
        List<FacetName> sliders = ClientDataManager.getSliderValues(rangeSliders);
        List<FacetName> datetimesliders = ClientDataManager.getSliderDateTimeValues(rangedatetimeSliders);
        String facetQuery = FacetQueryConstructionManager.constructQuery(values, config);
        List<String> queryList = ExternalUtils.parseQuery(facetQuery);
        FacetQueryConstructionManager.appendSliderQueries(queryList, sliders);
        FacetQueryConstructionManager.appendDateTimeQueries(queryList, datetimesliders);
        Response result = QueryManager.getDataForUnselectedValue(searchKeywords, queryList, config);
        Gson gson = new Gson();
        return gson.toJson(result);
    }

    /**
     * This method changes the focus variable in the query and retrieves the
     * relevant snippets.
     * 
     * @param focusVals
     *            a set of values which should be displayed in the result set.
     * @param searchKeywords
     *            a set of keywords provided by a client application.
     * @param selectedValues
     *            a set of facet values provided by a client application
     * @return Response in Json format, which contains a set of snippets.
     * @see Snippet
     * @see Response
     */

    @GET
    @Path("/getDataForFocus")
    @Produces("application/json")
    public String getDataForFocus(@QueryParam("focus_values") String focusVals, @QueryParam("search_keywords") String searchKeywords,
            @QueryParam("selected_facet_values") String selectedValues, @QueryParam("range_sliders") String rangeSliders, @QueryParam("datetime_sliders") String rangedatetimeSliders) {
        Configurations config = (Configurations) context.getAttribute(DataContextListener.CONFIGURATIONS);
        List<FacetValue> focusValues = ClientDataManager.getSelectedCheckboxes(focusVals);
        List<FacetValue> values = ClientDataManager.getSelectedCheckboxes(selectedValues);
        List<FacetName> sliders = ClientDataManager.getSliderValues(rangeSliders);
        List<FacetName> datetimesliders = ClientDataManager.getSliderDateTimeValues(rangedatetimeSliders);
        
        String facetQuery = FacetQueryConstructionManager.constructQuery(values, config);
        List<String> queryList = ExternalUtils.parseQuery(facetQuery);
        FacetQueryConstructionManager.appendSliderQueries(queryList, sliders);
        FacetQueryConstructionManager.appendDateTimeQueries(queryList, datetimesliders);
        
        Response result = QueryManager.getDataForFocus(focusValues, searchKeywords, queryList, config);
        Gson gson = new Gson();
        return gson.toJson(result);
    }

    /**
     * This method return a set of snippets that are displayed for the end user
     * of the application.
     * 
     * @param activePage
     *            is a result page number provided by a client application. It
     *            starts from 0.
     * @param searchKeywords
     *            a set of keywords provided by a client application.
     * @param selectedValues
     *            a set of facet values provided by a client application.
     * @return Response in Json format, which contains a set of snippets.
     * @see Response
     */

    @GET
    @Path("/getSnippets")
    @Produces("application/json")
    public String getSnippets(@QueryParam("active_page") String activePage, @QueryParam("search_keywords") String searchKeywords,
            @QueryParam("selected_facet_values") String selectedValues, @QueryParam("range_sliders") String rangeSliders, @QueryParam("datetime_sliders") String rangedatetimeSliders) {
        Configurations config = (Configurations) context.getAttribute(DataContextListener.CONFIGURATIONS);
        List<FacetValue> values = ClientDataManager.getSelectedCheckboxes(selectedValues);
        List<FacetName> sliders = ClientDataManager.getSliderValues(rangeSliders);
        List<FacetName> datetimesliders = ClientDataManager.getSliderDateTimeValues(rangedatetimeSliders);
        
        String facetQuery = FacetQueryConstructionManager.constructQuery(values, config);
        List<String> queryList = ExternalUtils.parseQuery(facetQuery);
        FacetQueryConstructionManager.appendSliderQueries(queryList, sliders);
        FacetQueryConstructionManager.appendDateTimeQueries(queryList, datetimesliders);
        
        Response result = QueryManager.getSnippets(Integer.parseInt(activePage), searchKeywords, queryList, config);
        Gson gson = new Gson();
        return gson.toJson(result);
    }

    /**
     * This method takes a list of facet names and values as an input and
     * verifies if they have to be hidden or shown for the end user.
     * 
     * @param searchKeywords
     *            a set of keywords provided by a client application.
     * @param selectedValues
     *            a set of facet values provided by a client application.
     * @param toggledValueId
     *            is the last value which was selected or unselected by the
     *            client.
     * @param valuesToHideUnhide
     *            a set of facet values provided by a client application that
     *            have to be verified.
     * @param predicates
     *            a set of the facet names provided by the client application
     *            that have to be verified.
     * @return Response in Json format, which contains a set of facet names and
     *         values with the information if they should be visible or hidden.
     * @see FacetName
     * @see FacetValue
     */

    @POST
    @Path("/hideUnhideFacetValues")
    @Produces("application/json")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String hideUnhideFacetValues(@FormParam("search_keywords") String searchKeywords,
            @FormParam("selected_facet_values") String selectedValues, @QueryParam("range_sliders") String rangeSliders,@QueryParam("datetime_sliders") String rangedatetimeSliders,
            @FormParam("toggled_facet_value_id") String toggledValueId, @FormParam("same_level_siblings") String valuesToHideUnhide,
            @FormParam("facet_names") String predicates) {
        Gson gson = new Gson();
        Configurations config = (Configurations) context.getAttribute(DataContextListener.CONFIGURATIONS);
        // This disables hiding for pagoda and hermit, because they do not
        // support asynchronous queries
        if (config.getStoreType().equals("HERMIT") || config.getStoreType().equals("PAGODA"))
            return gson.toJson(null);
        List<FacetValue> values = ClientDataManager.getSelectedCheckboxes(selectedValues);
        List<FacetName> sliders = ClientDataManager.getSliderValues(rangeSliders);
 
        List<FacetName> datetimesliders = ClientDataManager.getSliderDateTimeValues(rangedatetimeSliders);
        String facetQuery = FacetQueryConstructionManager.constructQuery(values, config);
        List<String> queryList = ExternalUtils.parseQuery(facetQuery);
        FacetQueryConstructionManager.appendSliderQueries(queryList, sliders);
        FacetQueryConstructionManager.appendDateTimeQueries(queryList, datetimesliders);        
        List<FacetValue> targetValues = ClientDataManager.getSelectedCheckboxes(valuesToHideUnhide);
        FacetValue toggledFacetValue = ClientDataManager.getFacetValueFromId(toggledValueId, targetValues);
        Response result;
        List<FacetName> facetNames = ClientDataManager.getFacetNames(predicates);
        if (values.size() > 0)
            result = QueryManager.getDataForHiding(toggledFacetValue, targetValues, facetNames, values, searchKeywords, queryList, config);
        else
            result = ClientDataManager.unhideAllValues(targetValues, facetNames);        
        return gson.toJson(result);
    }

    /**
     * This method returns active system configurations
     * 
     * @return Configurations in Json format
     * @see Configurations
     */

    @GET
    @Path("/getConfigurations")
    @Produces("application/json")
    public String getConfigurations() {
        Configurations config = (Configurations) context.getAttribute(DataContextListener.CONFIGURATIONS);
        config.setAllPredicates(QueryExecutor.getAllPredicates(config));
        config.setAllTriples(config.getTripleStore().getItemsCount());
        config.setMemory(MemoryCheck.getMemoryUsageInformation());
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(config);
    }

    // TODO: revise code and documentation from here
    /**
     * This method sets active system configurations
     * 
     * @param image
     *            predicate provided by the client application.
     * @param url
     *            predicate provided by the client application.
     * @param description
     *            predicate provided by the client application.
     * @param category
     *            predicate provided by the client application.
     * @param label
     *            predicate provided by the client application.
     * @param latitude
     *            predicate provided by the client application.
     * @param longitude
     *            predicate provided by the client application.
     * @param max
     *            predicate provided by the client application.
     * @param separator
     *            predicate provided by the client application.
     * @param conFacets
     *            predicate provided by the client application.
     * @param exclPredicates
     *            predicate provided by the client application.
     * @param nesting
     *            provided by the client application.
     * @return Json message if the operation was successful or not.
     * @see Configurations
     */

    @GET
    @Path("/setConfigurations")
    @Produces("application/json")
    public String setConfigurations(@QueryParam("max") int max, @QueryParam("image") String image, @QueryParam("url") String url,
            @QueryParam("title") String title, @QueryParam("description") String description, @QueryParam("extra1") String extra1,
            @QueryParam("extra2") String extra2, @QueryParam("extra3") String extra3, @QueryParam("category") String category,
            @QueryParam("label") String label, @QueryParam("latitude") String latitude, @QueryParam("longitude") String longitude,
            @QueryParam("hierarchy") String hierarchy, @QueryParam("conjunctive_predicates") String conFacets,
            @QueryParam("excluded_predicates") String exclPredicates, @QueryParam("predicates_browsing_order") boolean browsing_order, @QueryParam("nesting") boolean nesting) {
        LOG.info("Activating new settings");
        Configurations config = (Configurations) context.getAttribute(DataContextListener.CONFIGURATIONS);

        config.setSnippetImagePredicate(image);
        config.setSnippetURLPredicate(url);
        config.setLatitudePredicate(latitude);
        config.setLongitudePredicate(longitude);
        config.setMaxSearchResuls(max);
        config.setNesting(nesting);
        config.setBrowsingOrder(browsing_order);

        String snippetDescription = config.getSnippetDescriptionPredicate();
        String snippetTitle = config.getSnippetTitlePredicate();
        String snippetExtra1 = config.getSnippetExtra1Predicate();
        String snippetExtra2 = config.getSnippetExtra2Predicate();
        String snippetExtra3 = config.getSnippetExtra3Predicate();
        if (!snippetDescription.equals(description) || !snippetTitle.equals(title) || !snippetExtra1.equals(extra1) || !snippetExtra2.equals(extra2)
                || !snippetExtra3.equals(extra3)) {
            config.setSnippetTitlePredicate(title);
            config.setSnippetDescriptionPredicate(description);
            config.setSnippetExtra1Predicate(extra1);
            config.setSnippetExtra2Predicate(extra2);
            config.setSnippetExtra3Predicate(extra3);
            config.setSearchIndex(DataContextListener.loadDataToSearchIndex(config));
        }

        String categoryPredicate = config.getCategoryPredicate();
        if (!categoryPredicate.equals(category)) {
            config.setCategoryPredicate(category);
            config.setIdCategoryMap(QueryExecutor.mapCategoriesToIds(config));
            LOG.info("Number of ids that have categories : " + config.getIdCategoryMap().size());
        }

        String labelPredicate = config.getLabelPredicate();
        if (!labelPredicate.equals(label)) {
            config.setLabelPredicate(label);
            config.setIdLabelMap(QueryExecutor.mapLabelsToIds(config));
            LOG.info("Number of ids that have labels : " + config.getIdLabelMap().size());
        }

        String hierarchyPredicate = config.getHierarchyPredicate();
        if (!hierarchyPredicate.equals(hierarchy)) {
            config.setHierarchyPredicate(hierarchy);
            config.setHierarchyMap(QueryExecutor.getHierarchyMap(config));
            LOG.info("Hierarchy map was created.");
        }

        String[] conjunctive = conFacets.split(",");
        Set<String> conjunctivePredicates = new HashSet<String>();
        for (String predicate : conjunctive)
            conjunctivePredicates.add(predicate);
        config.setConjunctivePredicates(conjunctivePredicates);

        String[] excluded = exclPredicates.split(",");
        Set<String> excludedPredicates = new HashSet<String>();
        for (String predicate : excluded)
            excludedPredicates.add(predicate);

        config.setExcludedPredicates(excludedPredicates);
        DataContextListener.setDefaultExcludedPredicates(excludedPredicates, config);

        context.setAttribute(DataContextListener.CONFIGURATIONS, config);
        JsonMessage message = new JsonMessage();
        message.setSuccess("Settings were successfully updated.");
        Gson gson = new Gson();
        return gson.toJson(message);
    }
    
    /* Old version
    @GET
    @Path("/setConfigurations")
    @Produces("application/json")
    public String setConfigurations(@QueryParam("max") int max, @QueryParam("image") String image, @QueryParam("url") String url,
            @QueryParam("title") String title, @QueryParam("description") String description, @QueryParam("extra1") String extra1,
            @QueryParam("extra2") String extra2, @QueryParam("extra3") String extra3, @QueryParam("category") String category,
            @QueryParam("label") String label, @QueryParam("latitude") String latitude, @QueryParam("longitude") String longitude,
            @QueryParam("hierarchy") String hierarchy, @QueryParam("conjunctive_predicates") String conFacets,
            @QueryParam("excluded_predicates") String exclPredicates, @QueryParam("nesting") boolean nesting) {
        LOG.info("Activating new settings");
        Configurations config = (Configurations) context.getAttribute(DataContextListener.CONFIGURATIONS);

        config.setSnippetImagePredicate(image);
        config.setSnippetURLPredicate(url);
        config.setLatitudePredicate(latitude);
        config.setLongitudePredicate(longitude);
        config.setMaxSearchResuls(max);
        config.setNesting(nesting);

        String snippetDescription = config.getSnippetDescriptionPredicate();
        String snippetTitle = config.getSnippetTitlePredicate();
        String snippetExtra1 = config.getSnippetExtra1Predicate();
        String snippetExtra2 = config.getSnippetExtra2Predicate();
        String snippetExtra3 = config.getSnippetExtra3Predicate();
        if (!snippetDescription.equals(description) || !snippetTitle.equals(title) || !snippetExtra1.equals(extra1) || !snippetExtra2.equals(extra2)
                || !snippetExtra3.equals(extra3)) {
            config.setSnippetTitlePredicate(title);
            config.setSnippetDescriptionPredicate(description);
            config.setSnippetExtra1Predicate(extra1);
            config.setSnippetExtra2Predicate(extra2);
            config.setSnippetExtra3Predicate(extra3);
            config.setSearchIndex(DataContextListener.loadDataToSearchIndex(config));
        }

        String categoryPredicate = config.getCategoryPredicate();
        if (!categoryPredicate.equals(category)) {
            config.setCategoryPredicate(category);
            config.setIdCategoryMap(QueryExecutor.mapCategoriesToIds(config));
            LOG.info("Number of ids that have categories : " + config.getIdCategoryMap().size());
        }

        String labelPredicate = config.getLabelPredicate();
        if (!labelPredicate.equals(label)) {
            config.setLabelPredicate(label);
            config.setIdLabelMap(QueryExecutor.mapLabelsToIds(config));
            LOG.info("Number of ids that have labels : " + config.getIdLabelMap().size());
        }

        String hierarchyPredicate = config.getHierarchyPredicate();
        if (!hierarchyPredicate.equals(hierarchy)) {
            config.setHierarchyPredicate(hierarchy);
            config.setHierarchyMap(QueryExecutor.getHierarchyMap(config));
            LOG.info("Hierarchy map was created.");
        }

        String[] conjunctive = conFacets.split(",");
        Set<String> conjunctivePredicates = new HashSet<String>();
        for (String predicate : conjunctive)
            conjunctivePredicates.add(predicate);
        config.setConjunctivePredicates(conjunctivePredicates);

        String[] excluded = exclPredicates.split(",");
        Set<String> excludedPredicates = new HashSet<String>();
        for (String predicate : excluded)
            excludedPredicates.add(predicate);

        config.setExcludedPredicates(excludedPredicates);
        DataContextListener.setDefaultExcludedPredicates(excludedPredicates, config);

        context.setAttribute(DataContextListener.CONFIGURATIONS, config);
        JsonMessage message = new JsonMessage();
        message.setSuccess("Settings were successfully updated.");
        Gson gson = new Gson();
        return gson.toJson(message);
    }
    */

    /**
     * This method allows user to upload new data and ontology files and set
     * active triple store type.
     * 
     * @param dataInputStream
     *            provided by the client application.
     * @param dFileDetail
     *            provided by the client application.
     * @param ontologyInputStream
     *            provided by the client application.
     * @param oFileDetail
     *            provided by the client application.
     * @param storeType
     *            provided by the client application.
     * @return Json message if the operation was successful or not.
     */

    @POST
    @Path("/uploadData")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/json")
    public String uploadFile(@FormDataParam("data_file") InputStream dataInputStream,
            @FormDataParam("data_file") FormDataContentDisposition dFileDetail, @FormDataParam("ontology_file") InputStream ontologyInputStream,
            @FormDataParam("ontology_file") FormDataContentDisposition oFileDetail, @FormDataParam("store_type") String storeType) {
        LOG.info("New file upload started");
        Configurations config = (Configurations) context.getAttribute(DataContextListener.CONFIGURATIONS);
        JsonMessage message = new JsonMessage();
        Gson gson = new Gson();
        try {
            config.getTripleStore().dispose();
            config.setStoreType(storeType);
            String queryLogPath = config.getQueryLogPath();
            if (queryLogPath != null)
            	if (queryLogPath.endsWith(")")) {
            		String prefix = queryLogPath.substring(0, queryLogPath.lastIndexOf("("));
            		int index = Integer.parseInt(queryLogPath.substring(queryLogPath.lastIndexOf("(")+1, queryLogPath.length() - 1));
            		config.setQueryLogPath(prefix + "(" + (index + 1) + ")");
            	}
            	else {
            		config.setQueryLogPath(queryLogPath + "(1)");
            	}
            File ontologyFile = Utils.streamTofile(ontologyInputStream, oFileDetail);
            File dataFile = Utils.streamTofile(dataInputStream, dFileDetail);

            if (ontologyFile != null)
                config.setOntologyPath(ontologyFile.getAbsolutePath());
            else
                config.setOntologyPath("");
            if (dataFile != null)
                config.setDataPath(dataFile.getAbsolutePath());
            else
                config.setDataPath("");

            Store store = DataContextListener.getStore(config);
            store.loadOntology(config.getOntologyPath());
            store.loadData(dataFile);
            LOG.info("Number of tuples after import: " + store.getItemsCount());
            config.setTripleStore(store);

            DataContextListener.loadInMemoryIndexes(config);

        } catch (IOException e) {
            LOG.error("Failed to load file: " + e.getMessage());
            message.setError("Failed to load file: " + e.getMessage());
            return gson.toJson(message);
        } catch (StoreException e) {
            LOG.error("Failed to load file: " + e.getMessage());
            message.setError("Failed to load file: " + e.getMessage());
            return gson.toJson(message);
        }

        context.setAttribute(DataContextListener.CONFIGURATIONS, config);
        message.setSuccess("File was successfully loaded.");
        return gson.toJson(message);
    }

    /**
     * This method returns a triple store view including all distinct
     * predicates.
     * 
     * @return a set of triples containing distinct predicates
     * @see Triple
     */

    @GET
    @Path("/getDataPreview")
    @Produces("application/json")
    public String getDataView() {
        Configurations config = (Configurations) context.getAttribute(DataContextListener.CONFIGURATIONS);
        List<Triple> dataView = QueryManager.getDataView(config);
        Gson gson = new Gson();
        return gson.toJson(dataView);
    }

    /**
     * This method returns all user activity data from user activity table.
     * 
     * @return all user activity rows
     * @see UserActivity
     */

    @GET
    @Path("/getUserActivityData")
    @Produces("application/json")
    public String getUsersActivities() {
        Configurations config = (Configurations) context.getAttribute(DataContextListener.CONFIGURATIONS);
        List<UserActivity> activities = new ArrayList<UserActivity>();
        try {
            ResultSet rs = config.getActivityDatabase().executeQuery("SELECT * FROM user_activity");
            for (; rs.next();) {
                UserActivity activity = new UserActivity();
                activity.setUserId(rs.getInt(1));
                activity.setIpAddress(rs.getString(2));
                activity.setKeywords(rs.getString(3));
                activity.setFacetName(rs.getString(4));
                activity.setFacetValue(rs.getString(5));
                activity.setInsertionTime(rs.getTimestamp(6));
                activities.add(activity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Gson gson = new Gson();
        return gson.toJson(activities);
    }

}