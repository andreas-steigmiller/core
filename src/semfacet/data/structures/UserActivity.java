package semfacet.data.structures;

import java.sql.Timestamp;

public class UserActivity {

    private int UserId;
    private String ipAddress;
    private String keywords;
    private String facetName;
    private String facetValue;
    private Timestamp insertionTime;

    public int getUserId() {
	return UserId;
    }

    public void setUserId(int userId) {
	UserId = userId;
    }

    public String getIpAddress() {
	return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
	this.ipAddress = ipAddress;
    }

    public String getKeywords() {
	return keywords;
    }

    public void setKeywords(String keywords) {
	this.keywords = keywords;
    }

    public String getFacetName() {
	return facetName;
    }

    public void setFacetName(String facetName) {
	this.facetName = facetName;
    }

    public String getFacetValue() {
	return facetValue;
    }

    public void setFacetValue(String facetValue) {
	this.facetValue = facetValue;
    }

    public Timestamp getInsertionTime() {
	return (Timestamp) insertionTime.clone();
    }

    public void setInsertionTime(Timestamp insertionTime) {
	this.insertionTime = insertionTime != null ? new Timestamp(insertionTime.getTime()) : null;
    }
}
