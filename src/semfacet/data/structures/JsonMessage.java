/**
 * Some of the client side requests do not require any response. This class should inform if the request was successful or not.
 */

package semfacet.data.structures;

public class JsonMessage {

    private String success;
    private String error;

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
