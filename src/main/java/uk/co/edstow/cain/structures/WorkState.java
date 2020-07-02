package uk.co.edstow.cain.structures;

import uk.co.edstow.cain.pairgen.PairGenFactory;

public class WorkState<G extends Goal<G>> {
    public final int depth;
    public final GoalBag<G> goals;
    public final Plan<G> currentPlan;
    public final PairGenFactory.PairGen<G> pairGen;

    public WorkState(int depth, GoalBag<G> goals, Plan<G> currentPlan, PairGenFactory.PairGen<G> pairGen) {
        this.depth = depth;
        this.goals = goals;
        this.currentPlan = currentPlan;
        this.pairGen = pairGen;
    }

    public WorkState(int depth, GoalBag<G> goals, Plan<G> currentPlan) {
        this.depth = depth;
        this.goals = goals;
        this.currentPlan = currentPlan;
        this.pairGen = null;
    }

}
