package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;

public class ThresholdPairGen<G extends Goal<G>, T extends Transformation<R>, R extends Register> implements PairGenFactory<G,T,R> {
    private final int threshold;
    private final PairGenFactory<G,T,R> above;
    private final PairGenFactory<G,T,R> below;


    public ThresholdPairGen(int threshold, PairGenFactory<G,T,R> above, PairGenFactory<G,T,R> below) {
        this.threshold = threshold ;
        this.above = above;
        this.below = below;
    }

    @Override
    public PairGen<G, T, R> generatePairs(GoalBag<G> goals, Context<G, T, R> context) {
        int max = Integer.MIN_VALUE;
        for (G goal : goals) {
            max = (int) Math.max(max, goal.total());
        }
        PairGen<G,T,R> stratergy;
        if(max>threshold){
            stratergy = above.generatePairs(goals, context);
        } else {
            stratergy = below.generatePairs(goals, context);
        }
        return stratergy;
    }
}