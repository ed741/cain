package uk.co.edstow.cain.structures;

import uk.co.edstow.cain.pairgen.PairGenFactory;

public class WorkState {
    public final int depth;
    public final GoalBag goals;
    public final Plan currentPlan;
    public final PairGenFactory.PairGen pairGen;

    public WorkState(int depth, GoalBag goals, Plan currentPlan, PairGenFactory.PairGen pairGen) {
        this.depth = depth;
        this.goals = goals;
        this.currentPlan = currentPlan;
        this.pairGen = pairGen;
    }

    public WorkState(int depth, GoalBag goals, Plan currentPlan) {
        this.depth = depth;
        this.goals = goals;
        this.currentPlan = currentPlan;
        this.pairGen = null;
    }

}
