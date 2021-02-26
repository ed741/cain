package uk.co.edstow.cain.regAlloc;

import uk.co.edstow.cain.Transformation;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.Plan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public interface BankedRegisterAllocator<G extends Goal<G>, T extends Transformation> extends RegisterAllocator<G, T>{

    int getsBanks();

    @Override
    List<? extends BRegister> getAvailableRegistersArray();

    @Override
    List<? extends BRegister> getInitRegisters();

    @Override
    Mapping<G> solve(Plan<G, T> plan);

    class BRegister extends Register {
        public final int bank;
        public BRegister(int bank, String name) {
            super(name);
            this.bank = bank;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            BRegister bRegister = (BRegister) o;
            return this.bank == bRegister.bank;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), bank);
        }

        @Override
        public String toString() {
            return this.bank+"."+this.name;
        }
    }

    interface Mapping<G extends Goal<G>> extends RegisterAllocator.Mapping<G> {
        List<? extends BRegister> initRegisters();
        List<? extends BRegister> getTrash(int i);
        BRegister get(G goal);
    }

}
