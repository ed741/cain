package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;

public class PatternHeuristic2<G extends Kernel3DGoal<G>, T extends Transformation<R>, R extends Register> implements CostHeuristic<G, T, R> {

    private final int[] initialDivisions;
    private final int initialDivisionsMax;
    private final int initialDivisionsMin;

    public PatternHeuristic2(int[] initialDivisions) {
        this.initialDivisions = initialDivisions;
        this.initialDivisionsMax = Arrays.stream(this.initialDivisions).max().getAsInt();
        this.initialDivisionsMin = Arrays.stream(this.initialDivisions).min().getAsInt();
    }
    public PatternHeuristic2(List<G> initialGoals) {
        int[] initialDivisions = new int[initialGoals.size()];
        for (int i = 0; i < initialDivisions.length; i++) {
            initialDivisions[i] = 31 - Integer.numberOfLeadingZeros( initialGoals.get(i).totalI() );
        }
        this.initialDivisions = initialDivisions;
        this.initialDivisionsMax = Arrays.stream(this.initialDivisions).max().getAsInt();
        this.initialDivisionsMin = Arrays.stream(this.initialDivisions).min().getAsInt();
    }


    @Override
    public double getCost(GoalPair<G, T, R> pair, GoalBag<G> goals, Context<G, T,R> context) {
        GoalBag<G> proposedGoals = new GoalBag<>(goals);
        for (G upper : pair.getUppers()) {
            proposedGoals.remove(upper);
        }
        for (G goal : pair.getLowers()) {
            proposedGoals.remove(goal);
        }
        proposedGoals.addAll(pair.getLowers());
        // this quick 'n' dirty proposed goals generator disregards the transformation's properties but will always
        // underestimate the register usage so we dont accidentally exit early on potentially valid solutions.
        // We might want to do away with this check and make this check a problem for the pairGeneration code

        if(!context.registerAllocator.checkValid(proposedGoals, pair.getTransformation())){
            return -1; // exit early if too many registers are used.
        }
        double cost = 0;
//        cost += Math.pow(proposedGoals.size(), (5-Math.min(5, config.availableRegisters-proposedGoals.size())));
        for (G g : proposedGoals) {
            double atomDistanceCost = 0;
            for (Iterator<Tuple<Kernel3DGoal.Coord, Integer>> it = g.uniqueCountIterator(); it.hasNext(); ) {
                Tuple<Kernel3DGoal.Coord, Integer> t = it.next();
                Kernel3DGoal.Coord a = t.getA();
                int count = Math.abs(t.getB());
                atomDistanceCost += count * (Math.abs(a.x) + Math.abs(a.y) + Math.abs(a.z) + (t.getB()>0?0:1));
                cost += count;
            }
            cost += atomDistanceCost;
        }

        int min = Integer.MAX_VALUE;
        int max = 0;

        List<G> goalList = new ArrayList<>(proposedGoals.asList());
        for (int i = 0; i < goalList.size(); i++) {
            G goal = goalList.get(i);
            List<G> toRemove = patternRepeated(goalList, goal);
            goalList.removeAll(toRemove);
            if(i > goalList.size()){
                System.out.println("I " + i);
                System.out.println("AtomGoal " + goal);
                System.out.println("AtomGoal List " + goalList);
                System.out.println("toRemove " + toRemove);
                System.out.println("GoalSet " + proposedGoals);
                System.exit(-1);
            }
            goalList.add(i, goal);
            if(!goal.allSame()) {
                cost += Math.pow(goal.totalI(), 2);
            }

            if(min>1){
                min = Math.min(min, goal.minimumCount());
            }
            max = Math.max(max, goal.maximumCount());
        }

        cost += (1<<initialDivisionsMax) / (min>0?min:1);
        cost += max / (1<<initialDivisionsMin);

        if(pair.getLowers().size()==1&&pair.getUppers().get(0).same(pair.getLowers().get(0))){
            cost *=2;
        }
        return cost;
    }

    private static <G extends Kernel3DGoal<G>> List<G> patternRepeated(Collection<G> goals, G pattern){
        List<G> matches = new ArrayList<>();
        for (G goal : goals) {
            if (pattern.equivalent(goal)) {
                matches.add(goal);
            } else if (goal.totalI() == pattern.totalI()){
                if(pattern.totalI() == 0){
                    matches.add(goal);
                } else {
                    if(goal.isTranslation(pattern)){
                        matches.add(goal);
                    }
                }
            }

        }
        return matches;
    }


}
