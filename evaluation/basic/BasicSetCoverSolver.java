package basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class BasicSetCoverSolver
{
    public interface Filter<T>
    {
        boolean matches(T t);
    }
 
    public static <T> Set<T> solveSetCover(Filter<Set<T>> filter,
            List<T> listOfSets)
    {
        final int size = listOfSets.size();
        if (size > 50)
            throw new IllegalArgumentException("Too many combinations");
        
        // All the possible subset of a set: 2^size of the set. In this case
        // we have 2^size number of sets
        int combinations = 1 << size;
        
        System.out.println("combinations: " + combinations);
        List<Set<T>> possibleSolutions = new ArrayList<Set<T>>();
        for (int l = 0; l < combinations; l++)
        {
            Set<T> combination = new LinkedHashSet<T>();
            for (int j = 0; j < size; j++)
            {
                if (((l >> j) & 1) != 0)
                    combination.add(listOfSets.get(j));
            }
            possibleSolutions.add(combination);
        }
        // the possible solutions in order of size.
        Collections.sort(possibleSolutions, new Comparator<Set<T>>()
        {
            public int compare(Set<T> o1, Set<T> o2)
            {
                return o1.size() - o2.size();
            }
        });
        for (Set<T> possibleSolution : possibleSolutions)
        {
            if (filter.matches(possibleSolution))
                return possibleSolution;
        }
        return null;
    }
}