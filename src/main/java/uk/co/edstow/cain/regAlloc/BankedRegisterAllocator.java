package uk.co.edstow.cain.regAlloc;

import uk.co.edstow.cain.goals.BankedGoal;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.Plan;

import java.util.List;

public interface BankedRegisterAllocator<G extends BankedGoal<G> & Goal<G>, T extends Transformation<R>, R extends BRegister> extends RegisterAllocator<G, T, R>{

    int getsBanks();

    @Override
    List<R> getAvailableRegistersArray();

    @Override
    List<R> getInitRegisters();

    @Override
    Mapping<G,R> solve(Plan<G, T, R> plan);

    interface Mapping<G extends Goal<G>, R extends BRegister> extends RegisterAllocator.Mapping<G, R> {
        List<R> initRegisters();
        List<R> getTrash(int i);
        R get(G goal);
    }

}
