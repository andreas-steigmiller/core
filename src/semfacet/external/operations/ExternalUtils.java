package semfacet.external.operations;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import aima.core.logic.propositional.parsing.PLParser;
import aima.core.logic.propositional.parsing.ast.Sentence;
import aima.core.logic.propositional.visitors.ConvertToDNF;

public class ExternalUtils {
    static Logger LOG = Logger.getLogger(ExternalUtils.class.getName());

    public static ArrayList<String> parseQuery(String facetQuery) {
        if (facetQuery == null || facetQuery.isEmpty())
            return new ArrayList<String>();
        try {
            ConvertToDNF dnf = new ConvertToDNF();
            PLParser parser = new PLParser();
            Sentence s = parser.parse(prepareStringForDNF(facetQuery));
            @SuppressWarnings("static-access")
            String dnfquery = dnf.convert(s).toString();
            String[] queryList = dnfquery.split("\\|");
            ArrayList<String> parsedQueries = new ArrayList<String>();

            for (int i = 0; i < queryList.length; i++)
                parsedQueries.add(restoreStringAfterDNF(queryList[i]));

            return parsedQueries;
        } catch (Exception e) {
            LOG.error(e.getMessage());
            return new ArrayList<String>();
        }

    }

    private static String prepareStringForDNF(String s) {
        // replace replaces characters, replaceAll replaces substrings
        String temp = s.replace("-", "MMMMM");
        temp = temp.replace(":", "CCCCC");
        temp = temp.replace("#", "HHHHH");
        temp = temp.replace("/", "SSSSS");
        temp = temp.replace(".", "PPPPP");
        temp = temp.replace("_", "UUUUU");
        temp = temp.replace("?", "QQQQQ");
        temp = temp.replace("<", "LLLLL");
        temp = temp.replace(">", "GGGGG");
        temp = temp.replace(",", "KKKKK");
        temp = temp.replace(";", "BBBBB");
        temp = temp.replaceAll("AND", " & ");
        temp = temp.replaceAll("OR", " | ");
        return temp;
    }

    private static String restoreStringAfterDNF(String s) {
        String temp = s.replaceAll("AAAAA", " ");
        temp = temp.replaceAll("MMMMM", "-");
        temp = temp.replaceAll("CCCCC", ":");
        temp = temp.replaceAll("HHHHH", "#");
        temp = temp.replaceAll("SSSSS", "/");
        temp = temp.replaceAll("PPPPP", ".");
        temp = temp.replaceAll("UUUUU", "_");
        temp = temp.replaceAll("QQQQQ", "?");
        temp = temp.replaceAll("LLLLL", "<");
        temp = temp.replaceAll("GGGGG", ">");
        temp = temp.replaceAll("KKKKK", ",");
        temp = temp.replaceAll("BBBBB", ";");
        temp = temp.replaceAll("DDDDD", "(");
        temp = temp.replaceAll("EEEEE", ")");
        temp = temp.replaceAll("&", " . ");
        temp = temp.replaceAll("FFFFF", "&"); // this operation must be executed
        // after replacing & to .
        temp = temp.replaceAll("IIIII", "'");
        return temp;
    }

}
