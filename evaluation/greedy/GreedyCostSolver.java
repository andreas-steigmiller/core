package greedy;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

public class GreedyCostSolver<T> extends GreedySolver<T> {
	
	public GreedyCostSolver() {
		this._name = "Greedy cost heuristic";
		this.reset();
	}
	
	@Override
	public ElementSet<T> nextBestSet() {
		
		double currentSetCost = 0;
		double bestSetCost = Double.MAX_VALUE;
		int elementsCovered = 0;
		ElementSet<T> nextBestSet = null;
		
		for (ElementSet<T> e: this._model.getElementSetIterable()){
			
			elementsCovered = e.countElementsCovered(_elementsNotCovered);
			
			if(elementsCovered < 1)
				continue;
			
			currentSetCost = e.getCost();
			
			if (currentSetCost < bestSetCost){
				nextBestSet = e;	
				bestSetCost = currentSetCost;
			}
		}
		
		
		return nextBestSet; 
	}
	
}
