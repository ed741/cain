package uk.co.edstow.cain.structures;

import uk.co.edstow.cain.pairgen.PairGen;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.transformations.Transformation;

public class WorkState<G extends Goal<G>, T extends Transformation<R>, R extends Register> {
    public final int depth;
    public final GoalBag<G> goals;
    public final Plan<G,T,R> currentPlan;
    public final PairGen<G,T,R> pairGen;

    public WorkState(int depth, GoalBag<G> goals, Plan<G,T,R> currentPlan, PairGen<G,T,R> pairGen) {
        this.depth = depth;
        this.goals = goals;
        this.currentPlan = currentPlan;
        this.pairGen = pairGen;
    }

    public WorkState(int depth, GoalBag<G> goals, Plan<G,T,R> currentPlan) {
        this.depth = depth;
        this.goals = goals;
        this.currentPlan = currentPlan;
        this.pairGen = null;
    }

}
