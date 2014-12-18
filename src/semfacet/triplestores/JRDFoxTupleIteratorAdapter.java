package semfacet.triplestores;

import uk.ac.ox.cs.JRDFox.JRDFStoreException;
import uk.ac.ox.cs.JRDFox.model.Individual;
import uk.ac.ox.cs.JRDFox.store.TupleIterator;

public class JRDFoxTupleIteratorAdapter implements ResultSet {

	TupleIterator iterator;

	public JRDFoxTupleIteratorAdapter(TupleIterator iterator) {
		this.iterator = iterator;
	}

	@Override
	public void dispose() {
		iterator.dispose();
	}

	@Override
	public boolean hasNext() {
		try {
			return iterator.isValid();
		} catch (JRDFStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void next() {
		try {
			iterator.getNext();
		} catch (JRDFStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void open() {
		try {
			iterator.open();
		} catch (JRDFStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String getItem(int index) {
		try {
			String result = iterator.getRawGroundTerm(index);
			if (result.startsWith("<") && result.endsWith(">"))
				return result.replace("<", "").replace(">", "");
			int first = result.indexOf('"');
			int last = result.lastIndexOf('"');
			if (first != last && first > -1) {
				return result.substring(first + 1, last);
			} else {
				return result;
			}
		} catch (JRDFStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean isIndividual(int index) {
		try {
			return iterator.getGroundTerm(index) instanceof Individual;
		} catch (JRDFStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

}
