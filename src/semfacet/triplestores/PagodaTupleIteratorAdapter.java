package semfacet.triplestores;

import uk.ac.ox.cs.pagoda.query.AnswerTuple;
import uk.ac.ox.cs.pagoda.query.AnswerTuples;

public class PagodaTupleIteratorAdapter implements ResultSet {
    private AnswerTuples iterator;

    public PagodaTupleIteratorAdapter(AnswerTuples answers) {
        this.iterator = answers;
    }

    @Override
    public void dispose() {
        iterator.dispose();
    }

    @Override
    public boolean hasNext() {
        return iterator.isValid();
    }

    @Override
    public void next() {
        iterator.moveNext();
    }

    @Override
    public void open() {
    }

    @Override
    public String getItem(int index) {
        AnswerTuple answer = iterator.getTuple();
        String result = answer.getGroundTerm(index).toString();
        if (result != null)
            result = result.trim();
        if (result.startsWith("<") && result.endsWith(">"))
            return result.replace("<", "").replace(">", "");
        int first = result.indexOf('"');
        int last = result.lastIndexOf('"');
        if (first != last && first > -1) {
            return result.substring(first + 1, last);
        } else {
            return result;
        }
    }
    
    @Override
    public String getNativeItem(int index) {
    	AnswerTuple answer = iterator.getTuple();
    	String result = answer.getGroundTerm(index).toString();
    	if (result != null)
    		result = result.trim();
    	return result;
    }
    
    @Override
    public boolean isIndividual(int index) {
        //TODO: deal with individuals or remove this method
        return true;
    }

}
