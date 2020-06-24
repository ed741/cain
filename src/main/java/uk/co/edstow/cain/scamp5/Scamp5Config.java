package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.pairgen.Config;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.Goal;

import java.util.List;

public class Scamp5Config extends Config {
    PairGenFactory.PairGen strategy;

    boolean useMov2x;
    boolean useAdd3;

    boolean useAddx;
    boolean useAdd2x;
    boolean useSubx;
    boolean useSub2x;

    boolean subPowerOf2;

    public Scamp5Config(int availableRegisters, int depth, List<Goal> initialGoals) {
        super(availableRegisters, depth, initialGoals);
    }

    public Scamp5Config(PairGenFactory.PairGen strategy, boolean useMov2x, boolean useAdd3, boolean useAddx, boolean useAdd2x, boolean useSubx, boolean useSub2x, int availableRegisters, int depth, List<Goal> initialGoals) {
        super(availableRegisters, depth, initialGoals);

        this.strategy = strategy;

        this.useMov2x = useMov2x;
        this.useAdd3 = useAdd3;
        this.useAddx = useAddx;
        this.useAdd2x = useAdd2x;
        this.useSubx = useSubx;
        this.useSub2x = useSub2x;
    }

    public Scamp5Config useAll(){
        this.useMov2x = true;
        this.useAdd3 = true;
        this.useAddx = true;
        this.useAdd2x = true;
        this.useSubx = true;
        this.useSub2x = true;
        return this;
    }

    public Scamp5Config useBasicOps(){
        this.useMov2x = false;
        this.useAdd3 = false;
        this.useAddx = false;
        this.useAdd2x = false;
        this.useSubx = false;
        this.useSub2x = false;
        return this;
    }

    public Scamp5Config useSubPowerOf2(){
        this.subPowerOf2 = true;
        return this;
    }

    public Scamp5Config setStrategy(PairGenFactory.PairGen strategy){
        this.strategy = strategy;
        return this;
    }



}
