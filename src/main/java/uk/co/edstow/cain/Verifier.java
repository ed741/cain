package uk.co.edstow.cain;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.regAlloc.RegisterAllocator;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.Plan;
import uk.co.edstow.cain.transformations.Transformation;

import java.util.List;

public interface Verifier<G extends Goal<G>, T extends Transformation<R>, R extends Register> {
    static <G extends Goal<G>, T extends Transformation<R>, R extends Register> Verifier<G,T,R> SkipVerify(){
        return (code, initialGoals, finalGoals, plan, registerAllocator) -> "Verification Skipped";
    }

    default void verbose(int v){}
    String verify(String code, List<G> initialGoals, List<G> finalGoals, Plan<G,T,R> plan, RegisterAllocator<G,T,R> registerAllocator);
}
