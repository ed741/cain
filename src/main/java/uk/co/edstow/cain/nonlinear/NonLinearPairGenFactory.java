package uk.co.edstow.cain.nonlinear;

import uk.co.edstow.cain.regAlloc.Register;
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

public class NonLinearPairGenFactory<G extends Goal<G>, T extends Transformation<R>, R extends Register> implements PairGenFactory<NonLinearGoal<G,T,R>, T, R>{//TODO generify 'Transformation'

    final LinearPairGenFactory<G, T, R> linearPairGenFactory;

    public NonLinearPairGenFactory(LinearPairGenFactory<G, T, R> linearPairGenFactory) {
        this.linearPairGenFactory = linearPairGenFactory;
    }

    @Override
    public Collection<Tuple<List<GoalPair<NonLinearGoal<G,T,R>, T, R>>, NonLinearGoal<G,T,R>>> applyAllUnaryOpForwards(List<NonLinearGoal<G,T,R>> initialGoals, Context<NonLinearGoal<G,T,R>, T, R> context, NonLinearGoal<G,T,R> goal) {
        G id = goal.identity();
        if(id != null){
            List<G> initGoals = new ArrayList<>();
            for (NonLinearGoal<G,T,R> initialGoal : initialGoals) {
                G ig = initialGoal.identity();
                if (ig != null){
                    initGoals.add(ig);
                }
            }
            // TODO wrap registerallocator for to work with G rather than Nonlinear<G>
            Collection<Tuple<List<GoalPair<NonLinearGoal<G,T,R>, T, R>>, NonLinearGoal<G,T,R>>> list = new ArrayList<>();
            for (Tuple<List<GoalPair<G, T, R>>, G> tuple : linearPairGenFactory.applyAllUnaryOpForwards(initGoals, new Context<>(context.searchDepth, null, initGoals), id)) {
                List<GoalPair<NonLinearGoal<G,T,R>, T, R>> result = new ArrayList<>();
                for (GoalPair<G, T, R> pair : tuple.getA()) {
                    GoalPair<NonLinearGoal<G,T,R>, T, R> nonLinearGoalTGoalPair = new GoalPair<>(idOfGoals(pair.getUppers()), idOfGoals(pair.getLowers()), pair.getTransformation());
                    result.add(nonLinearGoalTGoalPair);
                }
                Tuple<List<GoalPair<NonLinearGoal<G,T,R>, T, R>>, NonLinearGoal<G,T,R>> objects = new Tuple<>(
                        result,
                        idOfGoal(tuple.getB()));
                list.add(objects);
            }
            return list;
        }
        return null;
    }

    @Override
    public PairGen<NonLinearGoal<G,T,R>, T, R> generatePairs(GoalBag<NonLinearGoal<G,T,R>> goals, Context<NonLinearGoal<G,T,R>, T, R> context) {
        List<G> initGoals = new ArrayList<>();
        for (NonLinearGoal<G,T,R> initialGoal : context.initialGoals) {
            G ig = initialGoal.identity();
            if (ig != null){
                initGoals.add(ig);
            }
        }
        return new NonLinearPairGen<>(goals, linearPairGenFactory, new Context<>(context.searchDepth, null /*context.registerAllocator*/, initGoals));// TODO wrap registerallocator for to work with G rather than Nonlinear<G>
    }

    @Override
    public T getDummyTransformation(List<NonLinearGoal<G,T,R>> upperGoals, List<NonLinearGoal<G,T,R>> lowerGoals, Context<NonLinearGoal<G,T,R>,T,R> context) {
        return null;//linearPairGenFactory.getDummyTransformation(upperGoals, lowerGoals, context); //TODO make work
    }


    private static class NonLinearPairGen<G extends Goal<G>, T extends Transformation<R>, R extends Register> implements PairGen<NonLinearGoal<G,T,R>,T,R>{
        private final GoalBag<NonLinearGoal<G,T,R>> goals;
        private final PairGenFactory<G,T,R> linearPairGenFactory;
        private final Context<G,T,R> context;
        private int count = 0;
        private List<GoalPair<NonLinearGoal<G,T,R>, T, R>> currentList;

        public NonLinearPairGen(GoalBag<NonLinearGoal<G,T,R>> goals, PairGenFactory<G,T,R> linearPairGenFactory, Context<G,T,R> context) {
            this.goals = goals;
            this.linearPairGenFactory = linearPairGenFactory;
            this.context = context;
        }

        @Override
        public GoalPair<NonLinearGoal<G,T,R>, T, R> next() {
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
            GoalBag<NonLinearGoal<G,T,R>> nonLinearGoals = new GoalBag<>();
            for (NonLinearGoal<G,T,R> goal : goals) {
                if(goal.identity()!=null){
                    idGoals.add(goal.identity());
                } else {
                    nonLinearGoals.add(goal);
                    currentList.addAll(goal.getReductions());
                }
            }
            PairGen<G,T,R> linearPairGen = linearPairGenFactory.generatePairs(idGoals, context);
            GoalPair<G, T, R> c = linearPairGen.next();
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
