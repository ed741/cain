package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.pairgen.Config;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.structures.Goal;

import java.util.List;

public class Scamp5Config<G extends Goal<G>> implements Config.ConfigWithRegs {
    private final int availableRegisters;
    public final int depth;
    public final List<G> initialGoals;

    public PairGenFactory.PairGen<G> strategy;

    public boolean useMov2x;
    public boolean useAdd3;

    public boolean useAddx;
    public boolean useAdd2x;
    public boolean useSubx;
    public boolean useSub2x;

    public boolean subPowerOf2;

    private boolean onlyCostantValues;


    public Scamp5Config(int availableRegisters, int depth, List<G> initialGoals) {
        this.availableRegisters = availableRegisters;
        this.depth = depth;
        this.initialGoals = initialGoals;
    }

    public Scamp5Config(PairGenFactory.PairGen<G> strategy, boolean useMov2x, boolean useAdd3, boolean useAddx, boolean useAdd2x, boolean useSubx, boolean useSub2x, int availableRegisters, int depth, List<G> initialGoals) {
        this.availableRegisters = availableRegisters;
        this.depth = depth;
        this.initialGoals = initialGoals;
        this.strategy = strategy;

        this.useMov2x = useMov2x;
        this.useAdd3 = useAdd3;
        this.useAddx = useAddx;
        this.useAdd2x = useAdd2x;
        this.useSubx = useSubx;
        this.useSub2x = useSub2x;
    }

    public Scamp5Config<G> useAll(){
        this.useMov2x = true;
        this.useAdd3 = true;
        this.useAddx = true;
        this.useAdd2x = true;
        this.useSubx = true;
        this.useSub2x = true;
        return this;
    }

    public Scamp5Config<G> useBasicOps(){
        this.useMov2x = false;
        this.useAdd3 = false;
        this.useAddx = false;
        this.useAdd2x = false;
        this.useSubx = false;
        this.useSub2x = false;
        return this;
    }

    public Scamp5Config<G> useSubPowerOf2(){
        this.subPowerOf2 = true;
        return this;
    }

    public Scamp5Config<G> setStrategy(PairGenFactory.PairGen<G> strategy){
        this.strategy = strategy;
        return this;
    }


    public boolean onlyConstantValues() {
        return onlyCostantValues;
    }

    public void setOnlyConstantValues() {
        onlyCostantValues = true;
    }

    @Override
    public int totalAvailableRegisters() {
        return availableRegisters;
    }
}
