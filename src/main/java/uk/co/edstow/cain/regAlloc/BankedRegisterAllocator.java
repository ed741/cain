package uk.co.edstow.cain.regAlloc;

import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.Plan;

import java.util.List;

public interface BankedRegisterAllocator<G extends Goal<G>, T extends Transformation<BRegister>> extends RegisterAllocator<G, T, BRegister>{

    int getsBanks();

    @Override
    List<BRegister> getAvailableRegistersArray();

    @Override
    List<BRegister> getInitRegisters();

    @Override
    Mapping<G> solve(Plan<G, T, BRegister> plan);

    interface Mapping<G extends Goal<G>> extends RegisterAllocator.Mapping<G, BRegister> {
        List<BRegister> initRegisters();
        List<BRegister> getTrash(int i);
        BRegister get(G goal);
    }

}
