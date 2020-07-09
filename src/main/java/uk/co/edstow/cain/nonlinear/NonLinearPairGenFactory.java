package uk.co.edstow.cain.nonlinear;

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

    final PairGenFactory<G> linearPairGenFactory;

    public NonLinearPairGenFactory(PairGenFactory<G> linearPairGenFactory) {
        this.linearPairGenFactory = linearPairGenFactory;
    }

    @Override
    public Collection<Tuple<List<GoalPair<NonLinearGoal<G>>>, NonLinearGoal<G>>> applyAllUnaryOpForwards(List<NonLinearGoal<G>> initialGoals, int depth, NonLinearGoal<G> goal) {
        G id = goal.identity();
        if(id != null){
            List<G> initGoals = new ArrayList<>();
            for (NonLinearGoal<G> initialGoal : initialGoals) {
                G ig = initialGoal.identity();
                if (ig != null){
                    initGoals.add(ig);
                }
            }
            return linearPairGenFactory.applyAllUnaryOpForwards(initGoals, depth, id).stream()
                    .map(tuple -> new Tuple<>(
                            tuple.getA().stream().map(pair -> new GoalPair<>(idOfGoals(pair.getUppers()), idOfGoals(pair.getLowers()), pair.getTransformation())).collect(Collectors.toList()),
                            idOfGoal(tuple.getB())))
                    .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public PairGen<NonLinearGoal<G>> generatePairs(GoalBag<NonLinearGoal<G>> goals, int depth) {

        return null;
    }
}
