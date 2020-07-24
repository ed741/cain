package uk.co.edstow.cain;

import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.Plan;

public interface Verifier<G extends Goal<G>> {
    static <E extends Goal<E>> Verifier<E> SkipVerify(){
        return (code, reverseSearch, plan, registerAllocator) -> "Verification Skiped";
    }

    default void verbose(int v){}
    String verify(String code, ReverseSearch<G, ?> reverseSearch, Plan<G> plan, RegisterAllocator<G> registerAllocator);
}
