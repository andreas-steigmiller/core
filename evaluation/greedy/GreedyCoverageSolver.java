package greedy;


public class GreedyCoverageSolver<T> extends GreedySolver<T> {
	
	public GreedyCoverageSolver() {
		this._name = "Greedy coverage heuristic";
		this.reset();
	}
	
	@Override
	public ElementSet<T> nextBestSet() {

		int currentSetCoverage = 0;
		int bestSetCoverage = 0;
		
		ElementSet<T> nextBestSet = null;
		
		for (ElementSet<T> e: this._model.getElementSetIterable()){
			
			currentSetCoverage = e.countElementsCovered(this._elementsNotCovered);
			
			if (currentSetCoverage > bestSetCoverage){
				bestSetCoverage = currentSetCoverage;
				nextBestSet = e;
			}

		}
		
		return nextBestSet;
	}
	
}
