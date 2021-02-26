package uk.co.edstow.cain.structures;

import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.pairgen.PairGenFactory;

public class WorkState<G extends Goal<G>, T extends Transformation> {
    public final int depth;
    public final GoalBag<G> goals;
    public final Plan<G, T> currentPlan;
    public final PairGenFactory.PairGen<G, T> pairGen;

    public WorkState(int depth, GoalBag<G> goals, Plan<G, T> currentPlan, PairGenFactory.PairGen<G, T> pairGen) {
        this.depth = depth;
        this.goals = goals;
        this.currentPlan = currentPlan;
        this.pairGen = pairGen;
    }

    public WorkState(int depth, GoalBag<G> goals, Plan<G, T> currentPlan) {
        this.depth = depth;
        this.goals = goals;
        this.currentPlan = currentPlan;
        this.pairGen = null;
    }

}
