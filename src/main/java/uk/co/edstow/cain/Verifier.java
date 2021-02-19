package uk.co.edstow.cain;

import uk.co.edstow.cain.regAlloc.RegisterAllocator;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.Plan;

import java.util.List;

public interface Verifier<G extends Goal<G>> {
    static <E extends Goal<E>> Verifier<E> SkipVerify(){
        return (code, initialGoals, finalGoals, plan, registerAllocator) -> "Verification Skipped";
    }

    default void verbose(int v){}
    String verify(String code, List<G> initialGoals, List<G> finalGoals, Plan<G,?> plan, RegisterAllocator<G,?> registerAllocator);
}
