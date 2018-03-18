package greedy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import basic.BasicSetCoverSolver;
import basic.BasicSetCoverSolver.Filter;
import semfacet.data.structures.FacetName;

import java.io.*;

public class Report {
	
	
	public static void printReportSetCover(){
		
	}
	
	
	/**
	 * This method calculate the diversification of the facets according 
	 * to the greedy approach of the SetCover algorithm.
	 * The three approaches are computed automatically and a report is shown.
	 */
	public static void getGreedyAlgorithms(Set<String> totalElements, List<Set<String>> listOfSets){
		SCPModel<String> model = null;
		GreedyCoverageSolver CoverageMethod = new GreedyCoverageSolver();
		GreedyCostSolver CostMethod = new GreedyCostSolver();
		ChvatalSolver ChvatalMethod = new ChvatalSolver();
		double alpha = 1;
		
		Filter<Set<Set<String>>> filter = new Filter<Set<Set<String>>>()
        {
            public boolean matches(Set<Set<String>> elements)
            {
                Set<String> union = new LinkedHashSet<String>();
                for (Set<String> ints : elements)
                    union.addAll(ints);
                return union.equals(totalElements);
            }
        };
        
		
        model = getModelData(listOfSets);
        System.out.println("\n" + model);
        
        double start = System.currentTimeMillis();        
        Set<Set<String>> firstSolution = BasicSetCoverSolver.solveSetCover(filter,listOfSets);
        System.out.println("The basic set cover algorithm returns: " + firstSolution);
        double end = System.currentTimeMillis();
        System.out.println("\n" + "Time basic set cover algorithm (ms): " + (end-start));
        
        
        CoverageMethod.reset();
		CostMethod.reset();
		ChvatalMethod.reset();
		
		setMinCoverage(alpha, CoverageMethod, CostMethod, ChvatalMethod);
		CoverageMethod.setModel(model);
		CoverageMethod.solve();
		//CoverageMethod.print();

		CostMethod.setModel(model);
		CostMethod.solve();
		//CostMethod.print();

		ChvatalMethod.setModel(model);
		ChvatalMethod.solve();
		//ChvatalMethod.print();
		
		printComparison(alpha, CoverageMethod, CostMethod, ChvatalMethod);
		
	}
		
	// building sets, each of cost 1 hardcoded
	public static SCPModel<String> getModelData(List<Set<String>> listOfSets) {
		SCPModel<String> scpmodel = new SCPModel<String>();
		
		for (int i=0; i< listOfSets.size(); i++){
			Set<String> setElements = listOfSets.get(i);
			scpmodel.addElementSet(i, 1, setElements);
		}	
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
