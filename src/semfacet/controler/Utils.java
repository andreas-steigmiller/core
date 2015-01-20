/**
 * This class contains "quickly" added methods. One should think where to put them better.
 */
package semfacet.controler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import semfacet.data.structures.Configurations;
import semfacet.data.structures.FacetValue;

import com.sun.jersey.core.header.FormDataContentDisposition;

public class Utils {
    static Logger LOG = Logger.getLogger(Utils.class.getName());

    public static void logUserActivity(String searchKeywords, Configurations config, final FacetValue value) {
        try {
            if (value != null) {
                String query = "INSERT INTO user_activity(ip_address,keywords,facet_name,facet_value) VALUES('ip address', '" + searchKeywords
                        + "', '" + value.getPredicate() + "', '" + value.getObject() + "')";
                config.getActivityDatabase().update(query);
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public static File streamTofile(InputStream in, FormDataContentDisposition fileDetails) throws IOException {
        String fileName = fileDetails.getFileName();
        if (fileName != null) {
            final File tempFile = File.createTempFile(fileName, ".ttl");
            tempFile.deleteOnExit();
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                IOUtils.copy(in, out);
            }
            return tempFile;
        }
        return null;
    }
}
