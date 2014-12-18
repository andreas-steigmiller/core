package semfacet.external.operations;

import java.text.NumberFormat;

public class MemoryCheck {

    public static String getMemoryUsageInformation() {
	Runtime runtime = Runtime.getRuntime();
	NumberFormat format = NumberFormat.getInstance();
	long totalMemory = runtime.totalMemory();
	long freeMemory = runtime.freeMemory();
	return format.format((totalMemory - freeMemory) / 1024) +"Kb out of "+ format.format(totalMemory / 1024)+"Kb";
    }

}
