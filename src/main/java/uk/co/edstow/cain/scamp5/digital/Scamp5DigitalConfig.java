package uk.co.edstow.cain.scamp5.digital;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.scamp5.Scamp5Config;
import uk.co.edstow.cain.scamp5.output.Scamp5OutputFormatter;
import uk.co.edstow.cain.structures.Goal;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Scamp5DigitalConfig extends Scamp5Config<Scamp5DigitalConfig> {

    public Scamp5DigitalConfig(boolean useMov, boolean useMovx, boolean useAdd, boolean useAddSelf, boolean useDiv, boolean useRes, boolean useRes2, Map<? extends Register, List<String>> registerMapping, List<String> scratchRegisters, int bits, Scamp5OutputFormatter outputFormatter) {
        super(outputFormatter);
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
    final Map<? extends Register, List<String>> registerMapping;
    final List<String> scratchRegisters;
    final int bits;

    public Scamp5DigitalConfig(Scamp5DigitalConfig proto, Map<? extends Register, List<String>> registerMapping, List<String> scratchRegisters, int bits) {
        super(proto.outputFormatter);
        this.useMov = proto.useMov;
        this.useMovx = proto.useMovx;
        this.useAdd = proto.useAdd;
        this.useAddSelf = proto.useAdd;
        this.useDiv = proto.useDiv;
        this.useRes = proto.useRes;
        this.useRes2 = proto.useRes2;
        this.onlyMov = !(this.useAdd || this.useDiv || this.useRes || this.useRes2);
        this.registerMapping = registerMapping;
        this.scratchRegisters = scratchRegisters;
        this.bits = bits;
    }

    public Scamp5DigitalConfig(Scamp5DigitalConfig proto, boolean onlyMov) {
        super(proto.outputFormatter);
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



    public boolean onlyMov() {
        return onlyMov;
    }

    @Override
    public Builder builder() {
        return new Builder(this);
    }


    public static class Builder extends Scamp5ConfigBuilder<Scamp5DigitalConfig> {
        private boolean useMov;
        private boolean useMovx;
        private boolean useAdd;
        private boolean useAddSelf;
        private boolean useDiv;
        private boolean useRes;
        private boolean useRes2;
        private boolean onlyMov;
        private Map<? extends Register, List<String>> registerMapping;
        private List<String> scratchRegisters = Collections.emptyList();
        private int bits;

        public Builder() {
        }

        public Builder(Scamp5DigitalConfig src) {
            this.useMov = src.useMov;
            this.useMovx = src.useMovx;
            this.useAdd = src.useAdd;
            this.useAddSelf = src.useAddSelf;
            this.useDiv = src.useDiv;
            this.useRes = src.useRes;
            this.useRes2 = src.useRes2;
            this.onlyMov = src.onlyMov;
            this.registerMapping = src.registerMapping;
            this.scratchRegisters = src.scratchRegisters;
            this.bits = src.bits;
            this.outputFormatter = src.outputFormatter;
        }

        public Scamp5DigitalConfig build() {
            return new Scamp5DigitalConfig(useMov, useMovx, useAdd, useAddSelf, useDiv, useRes, useRes2, registerMapping, scratchRegisters, bits, outputFormatter);
        }

        public Builder useMov(boolean useMov) {
            this.useMov = useMov;
            return this;
        }

        public Builder useMovx(boolean useMovx) {
            this.useMovx = useMovx;
            return this;
        }

        public Builder useAdd(boolean useAdd) {
            this.useAdd = useAdd;
            return this;
        }

        public Builder useAddSelf(boolean useAddSelf) {
            this.useAddSelf = useAddSelf;
            return this;
        }

        public Builder useDiv(boolean useDiv) {
            this.useDiv = useDiv;
            return this;
        }

        public Builder useRes(boolean useRes) {
            this.useRes = useRes;
            return this;
        }

        public Builder useRes2(boolean useRes2) {
            this.useRes2 = useRes2;
            return this;
        }

        public Builder onlyMov(boolean onlyMov) {
            this.onlyMov = onlyMov;
            return this;
        }

        public Builder registerMapping(Map<? extends Register, List<String>> registerMapping) {
            this.registerMapping = registerMapping;
            return this;
        }

        public Builder scratchRegisters(List<String> scratchRegisters) {
            this.scratchRegisters = scratchRegisters;
            return this;
        }

        public Builder bits(int bits) {
            this.bits = bits;
            return this;
        }
    }
}
