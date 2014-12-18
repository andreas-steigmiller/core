package semfacet.triplestores;

import java.util.List;

import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

public class SesameTupleIteratorAdapter implements ResultSet {
    private TupleQueryResult queryResult;
    private List<String> bindingNames;
    private BindingSet bindingSet;
    private boolean hasNext;

    public SesameTupleIteratorAdapter(TupleQueryResult queryResult) {
	this.queryResult = queryResult;
	try {
	    this.bindingNames = queryResult.getBindingNames();
	} catch (QueryEvaluationException e) {
	    e.printStackTrace();
	}
    }

    @Override
    public void dispose() {
	try {
	    queryResult.close();
	} catch (QueryEvaluationException e) {
	    e.printStackTrace();
	}
    }

    @Override
    public boolean hasNext() {
	return hasNext;
    }

    @Override
    public void next() {
	try {
	    hasNext = queryResult.hasNext();
	    if (hasNext) {
		bindingSet = queryResult.next();
	    }
	} catch (QueryEvaluationException e) {
	    e.printStackTrace();
	}
    }

    @Override
    public void open() {
	try {
	    if (queryResult.hasNext()) {
		hasNext = true;
		bindingSet = queryResult.next();
	    } else {
		hasNext = false;
	    }
	} catch (QueryEvaluationException e) {
	    e.printStackTrace();
	}
    }

    @Override
    public String getItem(int index) {
	String name = bindingNames.get(index);
	return bindingSet.getValue(name).stringValue();
    }

    @Override
    public boolean isIndividual(int index) {
	String name = bindingNames.get(index);
	String value = bindingSet.getValue(name).stringValue();
	if (value.startsWith("<") && value.endsWith(">"))
	    return true;
	else
	    return false;

    }

}
