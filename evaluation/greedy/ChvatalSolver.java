package greedy;

public class ChvatalSolver<T> extends GreedySolver<T> {
	
	public ChvatalSolver() {
		this._name = "Chvatal's algorithm";
		this.reset();
	}
	
	@Override
	public ElementSet<T> nextBestSet() {
		
		
		ElementSet<T> nextBestSet = null;
		
		double currentCostCovRatio = 0;
		double bestCostCovRatio = Double.POSITIVE_INFINITY;
		int elementsCovered = 0;
		
		for (ElementSet<T> e: this._model.getElementSetIterable()){
		
			elementsCovered = e.countElementsCovered(_elementsNotCovered);
			
			if (elementsCovered < 1)
				continue;
			
			currentCostCovRatio = e.getCost() / elementsCovered;
			
			
			if (currentCostCovRatio < bestCostCovRatio){
				nextBestSet = e;
				bestCostCovRatio = currentCostCovRatio;
			}

		}
		
		return nextBestSet; 
	}
	
}
