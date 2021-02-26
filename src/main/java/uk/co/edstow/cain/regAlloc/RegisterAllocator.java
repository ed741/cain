package uk.co.edstow.cain.regAlloc;

import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.Plan;

import java.util.List;


public interface RegisterAllocator<G extends Goal<G>, T extends Transformation<R>, R extends Register> {
    int getAvailableRegisters();

    List<R> getAvailableRegistersArray();

    List<R> getInitRegisters();

    boolean checkValid(GoalBag<G> current, T lastTransformation);

    boolean checkPossible(Plan<G,T,R> p);

    Mapping<G,R> solve(Plan<G,T,R> plan);

    interface Mapping<G extends Goal<G>, R extends Register> {
        List<R> initRegisters();
        List<R> getTrash(int i);
        R get(G goal);
    }
}
