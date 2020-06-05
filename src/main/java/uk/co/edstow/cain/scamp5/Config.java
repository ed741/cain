package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.Goal;

import java.util.List;

public class Config {
    PairGenFactory.PairGen strategy;

    boolean useMov2x;
    boolean useAdd3;

    boolean useAddx;
    boolean useAdd2x;
    boolean useSubx;
    boolean useSub2x;

    boolean subPowerOf2;

    final int availableRegisters;

    final int depth;
    final List<Goal> initialGoals;

    public Config(int availableRegisters, int depth, List<Goal> initialGoals) {
        this.availableRegisters = availableRegisters;
        this.depth = depth;
        this.initialGoals = initialGoals;
    }

    public Config useAll(){
        this.useMov2x = true;
        this.useAdd3 = true;
        this.useAddx = true;
        this.useAdd2x = true;
        this.useSubx = true;
        this.useSub2x = true;
        return this;
    }

    public Config useBasicOps(){
        this.useMov2x = false;
        this.useAdd3 = false;
        this.useAddx = false;
        this.useAdd2x = false;
        this.useSubx = false;
        this.useSub2x = false;
        return this;
    }

    public Config useSubPowerOf2(){
        this.subPowerOf2 = true;
        return this;
    }

    public Config setStrategy(PairGenFactory.PairGen strategy){
        this.strategy = strategy;
        return this;
    }

    public Config(PairGenFactory.PairGen strategy, boolean useMov2x, boolean useAdd3, boolean useAddx, boolean useAdd2x, boolean useSubx, boolean useSub2x, int availableRegisters, int depth, List<Goal> initialGoals) {
        this.strategy = strategy;

        this.useMov2x = useMov2x;
        this.useAdd3 = useAdd3;
        this.useAddx = useAddx;
        this.useAdd2x = useAdd2x;
        this.useSubx = useSubx;
        this.useSub2x = useSub2x;

        this.depth = depth;
        this.availableRegisters = availableRegisters;
        this.initialGoals = initialGoals;
    }

}
