package greedy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import basic.BasicSetCoverSolver;
import basic.BasicSetCoverSolver.Filter;

import java.io.*;

public class Pro5_ammachef {
	
	
	public static void main(String[] args) throws IOException {
		SCPModel<String> model = null;
		GreedyCoverageSolver CoverageMethod = new GreedyCoverageSolver();
		GreedyCostSolver CostMethod = new GreedyCostSolver();
		ChvatalSolver ChvatalMethod = new ChvatalSolver();
		double alpha = 1;
		
		
	
		//Integer[] solution = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };
		//Integer[][] arrayOfSets = { { 1 },{ 2 }, {1,2,3}, {1,5,7,8,9,11,12, 18}, { 3, 8 }, { 9, 10 }, { 17 }, { 10 },
        //        { 2, 3, 19 }, { 4, 5 }, {20}, { 5, 7, 20 }, { 5, 6, 16, 20 }, { 4, 7, 15, 20 }, { 6, 7 },
        //        { 8, 9, 13, 14, 15 }, {1,5,10,15,20}, {3,4,5,6,7,8} };
        
        List<Set<String>> listOfSets = new ArrayList<Set<String>>();
        
        String[] solution2 = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l" };
		String[][] arrayOfSets2 = { { "a" },{ "b" }, {"a", "d", "e"}, {"a","e","g","j", "k","l"}, { "c", "k" }, 
				{  "c", "b", }, { "b" }, { "j" }, { "j", "k", "l" } };
        
        for (String[] array : arrayOfSets2)
            listOfSets.add(new LinkedHashSet<String>(Arrays.asList(array)));
        final Set<String> solutionSet = new LinkedHashSet<String>(Arrays.asList(solution2));
        
        
        Filter<Set<Set<String>>> filter = new Filter<Set<Set<String>>>()
        {
            public boolean matches(Set<Set<String>> elements)
            {
                Set<String> union = new LinkedHashSet<String>();
                for (Set<String> ints : elements)
                    union.addAll(ints);
                return union.equals(solutionSet);
            }
        };
        Set<Set<String>> firstSolution = BasicSetCoverSolver.solveSetCover(filter,listOfSets);
        System.out.println("The shortest combination was " + firstSolution);
		
        model = getModelData(listOfSets);
        
        CoverageMethod.reset();
		CostMethod.reset();
		ChvatalMethod.reset();
		
		setMinCoverage(alpha, CoverageMethod, CostMethod, ChvatalMethod);
		CoverageMethod.setModel(model);
		CoverageMethod.solve();
		CoverageMethod.print();

		CostMethod.setModel(model);
		CostMethod.solve();
		CostMethod.print();

		ChvatalMethod.setModel(model);
		ChvatalMethod.solve();
		ChvatalMethod.print();
		
		printComparison(alpha, CoverageMethod, CostMethod, ChvatalMethod);

	}
	
	// building sets, each of cost 1 hardcoded
	public static SCPModel<String> getModelData(List<Set<String>> listOfSets) {
		SCPModel<String> scpmodel = new SCPModel<String>();
		
		for (int i=0; i< listOfSets.size(); i++){
			Set<String> setElements = listOfSets.get(i);
			scpmodel.addElementSet(i, 1, setElements);
		}
		
		System.out.println("\n" + scpmodel);
		
		return scpmodel;
	}

	// reset the solution methods
	public static void resetMethods(GreedyCoverageSolver CoverageMethod, GreedyCostSolver CostMethod, ChvatalSolver ChvatalMethod) {
		CoverageMethod.reset();
		CostMethod.reset();
		ChvatalMethod.reset();
	}
	
	
	// set minimum coverage level for solution methods
	public static void printComparison(double alpha, GreedyCoverageSolver CoverageMethod, GreedyCostSolver CostMethod, ChvatalSolver ChvatalMethod) {
		System.out.format("\nAlpha: %.2f%%\n\n", 100*alpha);
		System.out.println("Algorithm                   Time (ms)     Obj Fn Val     Coverage (%)");
		System.out.println("---------------------------------------------------------------------");
		CoverageMethod.printRowMetrics();
		CostMethod.printRowMetrics();
		ChvatalMethod.printRowMetrics();
		System.out.println("---------------------------------------------------------------------");
		
		double tol = 1e-10;	// numerical tolerance
		
		// get time winner
		long minTime = CoverageMethod.getCompTime();
		String timeWinner = "Coverage";
		if (minTime - CostMethod.getCompTime() > tol) {
			minTime = CostMethod.getCompTime();
			timeWinner = "Cost";
		}
		if (minTime - ChvatalMethod.getCompTime() > tol) {
			minTime = ChvatalMethod.getCompTime();
			timeWinner = "Chvatal";
		}
		
		// get obj fn val winner
		double minObj = CoverageMethod.getObjFn();
		String objWinner = "Coverage";
		if (minObj - CostMethod.getObjFn() > tol) {
			minObj = CostMethod.getObjFn();
			objWinner = "Cost";
		}
		if (minObj - ChvatalMethod.getObjFn() > tol) {
			minObj = ChvatalMethod.getObjFn();
			objWinner = "Chvatal";
		}
		
		// get coverage winner
		double maxCov = CoverageMethod.getCoverage();
		String covWinner = "Coverage";
		if (CostMethod.getCoverage() - maxCov > tol) {
			maxCov = CostMethod.getCoverage();
			covWinner = "Cost";
		}
		if (ChvatalMethod.getCoverage() - maxCov > tol) {
			minObj = ChvatalMethod.getCoverage();
			covWinner = "Chvatal";
		}
		
		System.out.format("%-25s%12s%15s%17s\n", "Category winner", timeWinner, objWinner, covWinner);
		System.out.println("---------------------------------------------------------------------\n");
		
		String overall = "Unclear";
		if (timeWinner.equals(objWinner) && objWinner.equals(covWinner))
			overall = timeWinner;
		
		System.out.println("Overall winner: " + overall + "\n");
	}
	
	
	// set minimum coverage level for solution methods
	public static void setMinCoverage(double alpha, GreedyCoverageSolver CoverageMethod, GreedyCostSolver CostMethod, ChvatalSolver ChvatalMethod) {
		CoverageMethod.setMinCoverage(alpha);
		CostMethod.setMinCoverage(alpha);
		ChvatalMethod.setMinCoverage(alpha);
	}	
}
