package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.pairgen.Config;
import uk.co.edstow.cain.pairgen.CostHuristic;
import uk.co.edstow.cain.atom.pairGen.Distance;
import uk.co.edstow.cain.atom.Atom;
import uk.co.edstow.cain.atom.AtomGoal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;

import java.util.*;

public class PatternHuristic implements CostHuristic<AtomGoal> {

    private final int[] initialDivisions;
    private final int initialDivisionsMax;
    private final int initialDivisionsMin;

    public PatternHuristic(int[] initialDivisions) {
        this.initialDivisions = initialDivisions;
        this.initialDivisionsMax = Arrays.stream(this.initialDivisions).max().getAsInt();
        this.initialDivisionsMin = Arrays.stream(this.initialDivisions).min().getAsInt();
    }
    public PatternHuristic(List<AtomGoal> initialGoals) {
        int[] initialDivisions = new int[initialGoals.size()];
        for (int i = 0; i < initialDivisions.length; i++) {
            initialDivisions[i] = 31 - Integer.numberOfLeadingZeros( initialGoals.get(i).atomCount() );
        }
        this.initialDivisions = initialDivisions;
        this.initialDivisionsMax = Arrays.stream(this.initialDivisions).max().getAsInt();
        this.initialDivisionsMin = Arrays.stream(this.initialDivisions).min().getAsInt();
    }


    @Override
    public double getCost(GoalPair<AtomGoal> pair, GoalBag<AtomGoal> goals, Config<AtomGoal> config) {
        GoalBag<AtomGoal> proposedGoals = new GoalBag<>(goals);
        for (AtomGoal upper : pair.getUppers()) {
            proposedGoals.remove(upper);
        }


        List<AtomGoal> toAdd = new ArrayList<>();
        for (AtomGoal goal : pair.getLowers()) {
            proposedGoals.remove(goal);
            toAdd.add(goal);
        }
        proposedGoals.addAll(toAdd);
        if(proposedGoals.size() +(pair.getTransformation().ExtraRegisterCount()) > config.totalAvailableRegisters){
            return -1; // exit early if too many registers are used.
        }
        double cost = 0;
//        cost += Math.pow(proposedGoals.size(), (5-Math.min(5, config.availableRegisters-proposedGoals.size())));
        for (AtomGoal g : proposedGoals) {
            int subset = 0;
            for(AtomGoal g2 : proposedGoals){
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

        List<AtomGoal> goalList = new ArrayList<>(proposedGoals.asList());
        for (int i = 0; i < goalList.size(); i++) {
            AtomGoal goal = goalList.get(i);
            List<AtomGoal> toRemove = patternRepeated(goalList, goal);
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

    private static List<AtomGoal> patternRepeated(Collection<AtomGoal> goals, AtomGoal pattern){
        List<AtomGoal> matches = new ArrayList<>();
        for (AtomGoal goal : goals) {
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
