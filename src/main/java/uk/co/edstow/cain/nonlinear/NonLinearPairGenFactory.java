package uk.co.edstow.cain.nonlinear;

import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static uk.co.edstow.cain.nonlinear.NonLinearGoal.idOfGoal;
import static uk.co.edstow.cain.nonlinear.NonLinearGoal.idOfGoals;

public class NonLinearPairGenFactory<G extends Goal<G>> implements PairGenFactory<NonLinearGoal<G>>{

    final LinearPairGenFactory<G> linearPairGenFactory;

    public NonLinearPairGenFactory(LinearPairGenFactory<G> linearPairGenFactory) {
        this.linearPairGenFactory = linearPairGenFactory;
    }

    @Override
    public Collection<Tuple<List<GoalPair<NonLinearGoal<G>>>, NonLinearGoal<G>>> applyAllUnaryOpForwards(List<NonLinearGoal<G>> initialGoals, Context<NonLinearGoal<G>> context, NonLinearGoal<G> goal) {
        G id = goal.identity();
        if(id != null){
            List<G> initGoals = new ArrayList<>();
            for (NonLinearGoal<G> initialGoal : initialGoals) {
                G ig = initialGoal.identity();
                if (ig != null){
                    initGoals.add(ig);
                }
            }
            return linearPairGenFactory.applyAllUnaryOpForwards(initGoals, new Context<G>(context.searchDepth, context.totalAvailableRegisters, initGoals), id).stream()
                    .map(tuple -> new Tuple<>(
                            tuple.getA().stream().map(pair -> new GoalPair<>(idOfGoals(pair.getUppers()), idOfGoals(pair.getLowers()), pair.getTransformation())).collect(Collectors.toList()),
                            idOfGoal(tuple.getB())))
                    .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public PairGen<NonLinearGoal<G>> generatePairs(GoalBag<NonLinearGoal<G>> goals, Context<NonLinearGoal<G>> context) {
        List<G> initGoals = new ArrayList<>();
        for (NonLinearGoal<G> initialGoal : context.initialGoals) {
            G ig = initialGoal.identity();
            if (ig != null){
                initGoals.add(ig);
            }
        }
        return new NonLinearPairGen<>(goals, linearPairGenFactory, new Context<G>(context.searchDepth, context.totalAvailableRegisters, initGoals));
    }


    private static class NonLinearPairGen<G extends Goal<G>> implements PairGen<NonLinearGoal<G>>{
        private final GoalBag<NonLinearGoal<G>> goals;
        private final PairGenFactory<G> linearPairGenFactory;
        private final Context<G> context;
        private int count = 0;
        private List<GoalPair<NonLinearGoal<G>>> currentList;

        public NonLinearPairGen(GoalBag<NonLinearGoal<G>> goals, PairGenFactory<G> linearPairGenFactory, Context<G> context) {
            this.goals = goals;
            this.linearPairGenFactory = linearPairGenFactory;
            this.context = context;
        }

        @Override
        public GoalPair<NonLinearGoal<G>> next() {
            count++;
            if(currentList!=null){
                if(currentList.isEmpty()){
                    return null;
                } else {
                    return currentList.remove(0);
                }
            }
            currentList = new ArrayList<>();


            GoalBag<G> idGoals = new GoalBag<>();
            GoalBag<NonLinearGoal<G>> nonLinearGoals = new GoalBag<>();
            for (NonLinearGoal<G> goal : goals) {
                if(goal.identity()!=null){
                    idGoals.add(goal.identity());
                } else {
                    nonLinearGoals.add(goal);
                    currentList.addAll(goal.getReductions());
                }
            }
            PairGen<G> linearPairGen = linearPairGenFactory.generatePairs(idGoals, context);
            GoalPair<G> c = linearPairGen.next();
            while (c != null){
                currentList.add(new GoalPair<>(idOfGoals(c.getUppers()), idOfGoals(c.getLowers()), c.getTransformation()));
                c = linearPairGen.next();
            }





            return null;
        }

        @Override
        public int getNumber() {
            return count;
        }
    }



}
