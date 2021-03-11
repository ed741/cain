package uk.co.edstow.cain.fileRun;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.Plan;
import uk.co.edstow.cain.transformations.Transformation;

import java.util.List;

public class Result<G extends Goal<G>, T extends Transformation<R>, R extends Register> {
    private final FileRun<G,T,R> fileRun;
    public final Plan<G, T, R> plan;
    public final long nodesExpanded;
    public final int cost;
    public final int depth;
    public final int[] circuitDepths;
    public final long time;
    public final String code;
    public final List<G> initialGoals;
    public final List<G> finalGoals;
    public final Verifier.VerificationResult verificationOutput;

    public Result(FileRun<G,T,R> fileRun, Plan<G, T, R> plan, long nodesExpanded, long time, String code, Verifier.VerificationResult verf) {
        this.fileRun = fileRun;
        this.plan = plan;
        this.nodesExpanded = nodesExpanded;
        this.time = time;
        this.cost = fileRun.reverseSearch.costFunction.apply(plan);
        this.depth = plan.depth();
        this.circuitDepths = plan.circuitDepths();
        this.code = code;
        this.initialGoals = fileRun.reverseSearch.getInitialGoals();
        this.finalGoals = fileRun.reverseSearch.getFinalGoals();
        this.verificationOutput = verf;
    }
}
