package semfacet.data.structures;

import java.util.List;

public class Response {
    private List<Snippet> snippets;
    private List<FacetName> facetNames;
    private List<FacetValue> facetValues;
    private FacetName firstFacetName;
    private int size;
    private int activePage;

    public Response() {
	this.activePage = 0;
    }

    public Response(List<FacetName> facetNames, List<FacetValue> facetValues) {
	this.facetNames = facetNames;
	this.facetValues = facetValues;
    }

    public int getActivePage() {
	return activePage;
    }

    public void setActivePage(int activePage) {
	this.activePage = activePage;
    }

    public List<Snippet> getSnippets() {
	return snippets;
    }

    public void setSnippets(List<Snippet> snippets) {
	this.snippets = snippets;
    }

    public int getSize() {
	return size;
    }

    public void setSize(int size) {
	this.size = size;
    }

    public List<FacetName> getFacetNames() {
	return facetNames;
    }

    public void setFacetNames(List<FacetName> facetNames) {
	this.facetNames = facetNames;
    }

    public List<FacetValue> getFacetValues() {
	return facetValues;
    }

    public void setFacetValues(List<FacetValue> facetValues) {
	this.facetValues = facetValues;
    }

    public FacetName getFirstFacetName() {
	return firstFacetName;
    }

    public void setFirstFacetName(FacetName firstFacetName) {
	this.firstFacetName = firstFacetName;
    }

}
