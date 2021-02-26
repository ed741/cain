package uk.co.edstow.cain.nonlinear;

import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static uk.co.edstow.cain.nonlinear.NonLinearGoal.idOfGoal;
import static uk.co.edstow.cain.nonlinear.NonLinearGoal.idOfGoals;

public class NonLinearPairGenFactory<G extends Goal<G>, T extends Transformation> implements PairGenFactory<NonLinearGoal<G, T>, T>{//TODO generify 'Transformation'

    final LinearPairGenFactory<G, T> linearPairGenFactory;

    public NonLinearPairGenFactory(LinearPairGenFactory<G, T> linearPairGenFactory) {
        this.linearPairGenFactory = linearPairGenFactory;
    }

    @Override
    public Collection<Tuple<List<GoalPair<NonLinearGoal<G, T>, T>>, NonLinearGoal<G, T>>> applyAllUnaryOpForwards(List<NonLinearGoal<G, T>> initialGoals, Context<NonLinearGoal<G, T>, T> context, NonLinearGoal<G, T> goal) {
        G id = goal.identity();
        if(id != null){
            List<G> initGoals = new ArrayList<>();
            for (NonLinearGoal<G, T> initialGoal : initialGoals) {
                G ig = initialGoal.identity();
                if (ig != null){
                    initGoals.add(ig);
                }
            }
            // TODO wrap registerallocator for to work with G rather than Nonlinear<G>
            Collection<Tuple<List<GoalPair<NonLinearGoal<G, T>, T>>, NonLinearGoal<G, T>>> list = new ArrayList<>();
            for (Tuple<List<GoalPair<G, T>>, G> tuple : linearPairGenFactory.applyAllUnaryOpForwards(initGoals, new Context<>(context.searchDepth, null, initGoals), id)) {
                List<GoalPair<NonLinearGoal<G, T>, T>> result = new ArrayList<>();
                for (GoalPair<G, T> pair : tuple.getA()) {
                    GoalPair<NonLinearGoal<G, T>, T> nonLinearGoalTGoalPair = new GoalPair<>(idOfGoals(pair.getUppers()), idOfGoals(pair.getLowers()), pair.getTransformation());
                    result.add(nonLinearGoalTGoalPair);
                }
                Tuple<List<GoalPair<NonLinearGoal<G, T>, T>>, NonLinearGoal<G, T>> objects = new Tuple<List<GoalPair<NonLinearGoal<G, T>, T>>, NonLinearGoal<G, T>>(
                        result,
                        idOfGoal(tuple.getB()));
                list.add(objects);
            }
            return list;
        }
        return null;
    }

    @Override
    public PairGen<NonLinearGoal<G, T>, T> generatePairs(GoalBag<NonLinearGoal<G,T>> goals, Context<NonLinearGoal<G, T>, T> context) {
        List<G> initGoals = new ArrayList<>();
        for (NonLinearGoal<G, T> initialGoal : context.initialGoals) {
            G ig = initialGoal.identity();
            if (ig != null){
                initGoals.add(ig);
            }
        }
        return new NonLinearPairGen<>(goals, linearPairGenFactory, new Context<G ,T>(context.searchDepth, null /*context.registerAllocator*/, initGoals));// TODO wrap registerallocator for to work with G rather than Nonlinear<G>
    }

    @Override
    public T getDummyTransformation(List<NonLinearGoal<G, T>> upperGoals, List<NonLinearGoal<G, T>> lowerGoals, Context<NonLinearGoal<G, T>, T> context) {
        return null;//linearPairGenFactory.getDummyTransformation(upperGoals, lowerGoals, context); //TODO make work
    }


    private static class NonLinearPairGen<G extends Goal<G>, T extends Transformation> implements PairGen<NonLinearGoal<G, T>, T>{
        private final GoalBag<NonLinearGoal<G, T>> goals;
        private final PairGenFactory<G, T> linearPairGenFactory;
        private final Context<G, T> context;
        private int count = 0;
        private List<GoalPair<NonLinearGoal<G, T>, T>> currentList;

        public NonLinearPairGen(GoalBag<NonLinearGoal<G, T>> goals, PairGenFactory<G, T> linearPairGenFactory, Context<G, T> context) {
            this.goals = goals;
            this.linearPairGenFactory = linearPairGenFactory;
            this.context = context;
        }

        @Override
        public GoalPair<NonLinearGoal<G, T>, T> next() {
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
            GoalBag<NonLinearGoal<G, T>> nonLinearGoals = new GoalBag<>();
            for (NonLinearGoal<G, T> goal : goals) {
                if(goal.identity()!=null){
                    idGoals.add(goal.identity());
                } else {
                    nonLinearGoals.add(goal);
                    currentList.addAll(goal.getReductions());
                }
            }
            PairGen<G, T> linearPairGen = linearPairGenFactory.generatePairs(idGoals, context);
            GoalPair<G, T> c = linearPairGen.next();
            while (c != null){
                currentList.add(new GoalPair<NonLinearGoal<G,T>, T>(idOfGoals(c.getUppers()), idOfGoals(c.getLowers()), c.getTransformation()));
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
