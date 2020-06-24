package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.ReverseSearch;
import uk.co.edstow.cain.pairgen.Config;
import uk.co.edstow.cain.pairgen.CostHuristic;
import uk.co.edstow.cain.pairgen.Distance;
import uk.co.edstow.cain.structures.Atom;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;

import java.util.*;

public class PatternHuristic<C extends Config> implements CostHuristic<C> {

    private int[] initialDivisions;
    private int initialDivisionsMax;
    private int initialDivisionsMin;

    public PatternHuristic(ReverseSearch rs) {
        this.initialDivisions = rs.getInitialDivisions();
        this.initialDivisionsMax = Arrays.stream(this.initialDivisions).max().getAsInt();
        this.initialDivisionsMin = Arrays.stream(this.initialDivisions).min().getAsInt();
    }

    @Override
    public double getCost(GoalPair pair, GoalBag goals, Config config) {
        GoalBag proposedGoals = new GoalBag(goals);
        for (Goal upper : pair.getUppers()) {
            proposedGoals.remove(upper);
        }


        List<Goal> toAdd = new ArrayList<>();
        for (Goal goal : pair.getLowers()) {
            proposedGoals.remove(goal);
            toAdd.add(goal);
        }
        proposedGoals.addAll(toAdd);
        if(proposedGoals.size() +(pair.getTransformation().ExtraRegisterCount()) > config.availableRegisters){
            return -1; // exit early if too many registers are used.
        }
        double cost = 0;
//        cost += Math.pow(proposedGoals.size(), (5-Math.min(5, config.availableRegisters-proposedGoals.size())));
        for (Goal g : proposedGoals) {
            int subset = 0;
            for(Goal g2 : proposedGoals){
                if(g2.hasSubGoal(g)){
                    subset++;
                }
                if(subset > 1){
                    break;
                }
            }
            double atomDistanceCost = 0;
            for (Atom a : g) {
                atomDistanceCost += Math.abs(a.x) + Math.abs(a.y) + Math.abs(a.z) + (a.positive?0:1);
                cost +=1;
            }
            cost += atomDistanceCost/subset;
        }

        int min = Integer.MAX_VALUE;
        int max = 0;

        List<Goal> goalList = new ArrayList<>(proposedGoals);
        for (int i = 0; i < goalList.size(); i++) {
            Goal goal = goalList.get(i);
            List<Goal> toRemove = patternRepeated(goalList, goal);
            goalList.removeAll(toRemove);
            if(i > goalList.size()){
                System.out.println("I " + i);
                System.out.println("Goal " + goal);
                System.out.println("Goal List " + goalList);
                System.out.println("toRemove " + toRemove);
                System.out.println("GoalSet " + proposedGoals);
                System.exit(-1);
            }
            goalList.add(i, goal);
            if(!goal.allSame()) {
                cost += Math.pow(goal.atomCount(), 2);
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

    private static List<Goal> patternRepeated(Collection<Goal> goals, Goal pattern){
        List<Goal> matches = new ArrayList<>();
        for (Goal goal : goals) {
            if (pattern.equivalent(goal)) {
                matches.add(goal);
            } else if (goal.atomCount() == pattern.atomCount()){
                if(pattern.atomCount() == 0){
                    matches.add(goal);
                } else {
                    if (pattern.get(0).positive == goal.get(0).positive) {
                        Distance d = new Distance(pattern.get(0), goal.get(0));
                        int i = 0;
                        for (; i < pattern.size(); i++) {
                            if (pattern.get(i).positive != goal.get(i).positive || !d.same(pattern.get(i), goal.get(i))) {
                                break;
                            }
                        }
                        if (i == pattern.size()) {
                            matches.add(goal);
                        }
                    }
                }
            }

        }
        return matches;
    }
}
