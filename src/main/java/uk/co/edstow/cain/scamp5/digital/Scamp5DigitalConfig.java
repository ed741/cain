package uk.co.edstow.cain.scamp5.digital;

import uk.co.edstow.cain.RegisterAllocator;
import uk.co.edstow.cain.scamp5.Scamp5ConfigGetter;
import uk.co.edstow.cain.scamp5.analogue.Scamp5AnalogueConfig;
import uk.co.edstow.cain.structures.Goal;

import java.util.List;
import java.util.Map;

public class Scamp5DigitalConfig<G extends Goal<G>> implements Scamp5ConfigGetter.Scamp5Config<G, Scamp5DigitalConfig<G>> {
    public Scamp5DigitalConfig(boolean useMov, boolean useMovx, boolean useAdd, boolean useAddSelf, boolean useDiv, boolean useRes, boolean useRes2, Map<RegisterAllocator.Register, List<String>> registerMapping, List<String> scratchRegisters, int bits) {
        this.useMov = useMov;
        this.useMovx = useMovx;
        this.useAdd = useAdd;
        this.useAddSelf = useAddSelf;
        this.useDiv = useDiv;
        this.useRes = useRes;
        this.useRes2 = useRes2;
        this.onlyMov = !(this.useAdd || this.useDiv || this.useRes || this.useRes2);
        this.registerMapping = registerMapping;
        this.scratchRegisters = scratchRegisters;
        this.bits = bits;
    }


    public final boolean useMov;
    public final boolean useMovx;
    public final boolean useAdd;
    public final boolean useAddSelf;
    public final boolean useDiv;
    public final boolean useRes;
    public final boolean useRes2;
    private final boolean onlyMov;
    final Map<RegisterAllocator.Register, List<String>> registerMapping;
    final List<String> scratchRegisters;
    final int bits;

    public Scamp5DigitalConfig(Scamp5AnalogueConfig<G> proto, Map<RegisterAllocator.Register, List<String>> registerMapping, List<String> scratchRegisters, int bits) {
        this.useMov = proto.useMov;
        this.useMovx = proto.useMovx;
        this.useAdd = proto.useAdd;
        this.useAddSelf = proto.useAdd;
        this.useDiv = proto.useDivq || proto.useDiv3 || proto.useDiv4 || proto.useDiva;
        this.useRes = proto.useRes;
        this.useRes2 = proto.useRes2;
        this.onlyMov = !(this.useAdd || this.useDiv || this.useRes || this.useRes2);
        this.registerMapping = registerMapping;
        this.scratchRegisters = scratchRegisters;
        this.bits = bits;
    }

    public Scamp5DigitalConfig(Scamp5DigitalConfig<G> proto, boolean onlyMov) {
        this.useMov = proto.useMov;
        this.useMovx = proto.useMovx;
        this.useAdd = proto.useAdd && (!onlyMov);
        this.useAddSelf = proto.useAddSelf && (!onlyMov);
        this.useDiv = proto.useDiv && (!onlyMov);
        this.useRes = proto.useRes && (!onlyMov);
        this.useRes2 = proto.useRes2 && (!onlyMov);
        this.onlyMov = !(this.useAdd || this.useDiv || this.useRes || this.useRes2);
        this.registerMapping = proto.registerMapping;
        this.scratchRegisters = proto.scratchRegisters;
        this.bits = proto.bits;
    }

    @Override
    public boolean onlyMov() {
        return onlyMov;
    }

    @Override
    public Scamp5DigitalConfig<G> getMovOnlyVersion() {
        return new Scamp5DigitalConfig<>(this, true);
    }
}
