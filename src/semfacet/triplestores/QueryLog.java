package semfacet.triplestores;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.log4j.Logger;

import semfacet.data.init.DataContextListener;

public class QueryLog {

	int count = 0; 
	String lastQuery = null; 
	
    static Logger LOG = Logger.getLogger(DataContextListener.class.getName());

	BufferedWriter writer = null; 
	
	public QueryLog(String queryLogPath) {
		if (queryLogPath == null) return ;
		try {
			if (writer != null) writer.close(); 
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(queryLogPath)));
			LOG.info("Query log path: " + queryLogPath);
		} catch (IOException e) {
			e.printStackTrace();
			writer = null; 
		}  
	}
	
	protected void add(String query) {
		if (writer == null) return ;
		
		if (query.isEmpty()) return ;
		if (lastQuery != null && query.equals(lastQuery)) return ;
		
		try {
			writer.write(query);
			writer.newLine();
			lastQuery = query; 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void dispose() {
		if (writer == null) return ;  
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			writer = null; 
		}
	}
	
	public void finalize() {
		dispose();
	}
	
}
