/**
 * Some of the external libraries might have memory leaks, this class is used to print out memory consumption. This should help to realize if there are some problems with the external libraries or not.
 * This class prints very basic memory consumption information, it can be easily extended if needed. 
 */

package semfacet.external.operations;

import java.text.NumberFormat;

public class MemoryCheck {

    public static String getMemoryUsageInformation() {
        Runtime runtime = Runtime.getRuntime();
        NumberFormat format = NumberFormat.getInstance();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        return format.format((totalMemory - freeMemory) / 1024) + "Kb out of " + format.format(totalMemory / 1024) + "Kb";
    }

}
