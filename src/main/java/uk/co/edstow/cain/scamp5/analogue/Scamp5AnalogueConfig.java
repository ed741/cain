package uk.co.edstow.cain.scamp5.analogue;

import uk.co.edstow.cain.scamp5.Scamp5Config;
import uk.co.edstow.cain.scamp5.output.Scamp5OutputFormatter;
import uk.co.edstow.cain.structures.Goal;

public class Scamp5AnalogueConfig extends Scamp5Config<Scamp5AnalogueConfig> {

    public final boolean useMov;
    public final boolean useMovx;
    public final boolean useMov2x;

    public final boolean useAdd;
    public final boolean useAddx;
    public final boolean useAdd2x;

    public final boolean useAdd3;

    public final boolean useSub;
    public final boolean useSubx;
    public final boolean useSub2x;

    public final boolean useDiv3;
    public final boolean useDiva;
    public final boolean useDiv4;
    public final boolean useDivq;

    public final boolean useRes;
    public final boolean useRes2;

    public final boolean useNeg;

    public final boolean subPowerOf2;

    public final boolean onlyMov;

    public Scamp5AnalogueConfig(boolean useMov, boolean useMovx, boolean useMov2x, boolean useAdd, boolean useAdd3, boolean useAddx, boolean useAdd2x, boolean useSub, boolean useSubx, boolean useSub2x, boolean useDiv3, boolean useDiva, boolean useDiv4, boolean useDivq, boolean useRes, boolean useRes2, boolean useNeg, boolean subPowerOf2, Scamp5OutputFormatter outputFormatter) {
        super(outputFormatter);
        this.useMov = useMov;
        this.useMovx = useMovx;
        this.useMov2x = useMov2x;
        this.useAdd = useAdd;
        this.useAdd3 = useAdd3;
        this.useAddx = useAddx;
        this.useAdd2x = useAdd2x;
        this.useSub = useSub;
        this.useSubx = useSubx;
        this.useSub2x = useSub2x;
        this.useDiv3 = useDiv3;
        this.useDiva = useDiva;
        this.useDiv4 = useDiv4;
        this.useDivq = useDivq;
        this.useRes = useRes;
        this.useRes2 = useRes2;
        this.useNeg = useNeg;
        this.subPowerOf2 = subPowerOf2;
        this.onlyMov = !(useAdd || useAdd3 || useAddx || useAdd2x || useSub || useSubx || useSub2x || useDiv3 || useDiv4 || useDiva || useDivq || useRes || useRes2 || useNeg);
    }

    public Scamp5AnalogueConfig(Scamp5AnalogueConfig proto) {
        super(proto.outputFormatter);
        this.useMov = proto.useMov;
        this.useMovx = proto.useMovx;
        this.useMov2x = proto.useMov2x;
        this.useAdd = proto.useAdd;
        this.useAddx = proto.useAddx;
        this.useAdd2x = proto.useAdd2x;
        this.useAdd3 = proto.useAdd3;
        this.useSub = proto.useSub;
        this.useSubx = proto.useSubx;
        this.useSub2x = proto.useSub2x;
        this.useDiv3 = proto.useDiv3;
        this.useDiva = proto.useDiva;
        this.useDiv4 = proto.useDiv4;
        this.useDivq = proto.useDivq;
        this.useRes = proto.useRes;
        this.useRes2 = proto.useRes2;
        this.useNeg = proto.useNeg;
        this.subPowerOf2 = proto.subPowerOf2;
        this.onlyMov = proto.onlyMov;
    }

    @Override
    public Builder builder() {
        return new Builder(this);
    }

    public static class Builder extends Scamp5ConfigBuilder<Scamp5AnalogueConfig> {

        private boolean useMov;
        private boolean useMovx;
        private boolean useMov2x;

        private boolean useAdd;
        private boolean useAddx;
        private boolean useAdd2x;

        private boolean useAdd3;

        private boolean useSub;
        private boolean useSubx;
        private boolean useSub2x;

        private boolean useDiv3;
        private boolean useDiva;
        private boolean useDiv4;
        private boolean useDivq;

        private boolean useRes;
        private boolean useRes2;

        private boolean useNeg;

        private boolean subPowerOf2;

        public Builder(boolean useMov, boolean useMovx, boolean useMov2x, boolean useAdd, boolean useAdd3, boolean useAddx, boolean useAdd2x, boolean useSub, boolean useSubx, boolean useSub2x, boolean useDiv3, boolean useDiva, boolean useDiv4, boolean useDivq, boolean useRes, boolean useRes2, boolean useNeg, boolean subPowerOf2, Scamp5OutputFormatter outputFormatter) {
            this.setOutputFormatter(outputFormatter);
            this.useMov = useMov;
            this.useMovx = useMovx;
            this.useMov2x = useMov2x;
            this.useAdd = useAdd;
            this.useAdd3 = useAdd3;
            this.useAddx = useAddx;
            this.useAdd2x = useAdd2x;
            this.useSub = useSub;
            this.useSubx = useSubx;
            this.useSub2x = useSub2x;
            this.useDiv3 = useDiv3;
            this.useDiva = useDiva;
            this.useDiv4 = useDiv4;
            this.useDivq = useDivq;
            this.useRes = useRes;
            this.useRes2 = useRes2;
            this.useNeg = useNeg;
            this.subPowerOf2 = subPowerOf2;
        }

        public Builder() {
        }
        public Builder(Scamp5AnalogueConfig config) {
            this.setOutputFormatter(config.outputFormatter);
            this.useMov = config.useMov;
            this.useMovx = config.useMovx;
            this.useMov2x = config.useMov2x;
            this.useAdd = config.useAdd;
            this.useAdd3 = config.useAdd3;
            this.useAddx = config.useAddx;
            this.useAdd2x = config.useAdd2x;
            this.useSub = config.useSub;
            this.useSubx = config.useSubx;
            this.useSub2x = config.useSub2x;
            this.useDiv3 = config.useDiv3;
            this.useDiva = config.useDiva;
            this.useDiv4 = config.useDiv4;
            this.useDivq = config.useDivq;
            this.useRes = config.useRes;
            this.useRes2 = config.useRes2;
            this.useNeg = config.useNeg;
            this.subPowerOf2 = config.subPowerOf2;
        }

        public Scamp5AnalogueConfig build(){
            return new Scamp5AnalogueConfig(useMov, useMovx, useMov2x, useAdd, useAdd3, useAddx, useAdd2x, useSub, useSubx, useSub2x, useDiv3, useDiva, useDiv4, useDivq, useRes, useRes2, useNeg, subPowerOf2, outputFormatter);
        }

        public Builder useAll(){
            this.useMov = true;
            this.useMovx = true;
            this.useMov2x = true;
            this.useAdd = true;
            this.useAdd3 = true;
            this.useAddx = true;
            this.useAdd2x = true;
            this.useSub = true;
            this.useSubx = true;
            this.useSub2x = true;
            this.useDiv3 = true;
            this.useDiva = true;
            this.useDiv4 = true;
            this.useDivq = false;
            this.useRes = true;
            this.useRes2 = true;
            this.useNeg = true;
            return this;
        }

        public Builder useBasic(){
            this.useMov = true;
            this.useMovx = true;
            this.useMov2x = false;
            this.useAdd = true;
            this.useAdd3 = false;
            this.useAddx = false;
            this.useAdd2x = false;
            this.useSub = true;
            this.useSubx = false;
            this.useSub2x = false;
            this.useDiv3 = false;
            this.useDiva = false;
            this.useDiv4 = false;
            this.useDivq = true;
            this.useRes = true;
            this.useRes2 = false;
            this.useNeg = true;
            return this;
        }

        public boolean isOnlyMov() {
            return !(useAdd || useAdd3 || useAddx || useAdd2x || useSub || useSubx || useSub2x || useDiv3 || useDiv4 || useDiva || useDivq || useRes || useRes2 || useNeg);
        }

        public Builder setOnlyMov() {
            this.useAdd = false;
            this.useAdd3 = false;
            this.useAddx = false;
            this.useAdd2x = false;
            this.useSub = false;
            this.useSubx = false;
            this.useSub2x = false;
            this.useDiv3 = false;
            this.useDiva = false;
            this.useDiv4 = false;
            this.useDivq = false;
            this.useRes = false;
            this.useRes2 = false;
            this.useNeg = false;
            return this;
        }

        public Builder setUseMov(boolean useMov) {
            this.useMov = useMov;
            return this;
        }

        public Builder setUseMovx(boolean useMovx) {
            this.useMovx = useMovx;
            return this;
        }

        public Builder setUseMov2x(boolean useMov2x) {
            this.useMov2x = useMov2x;
            return this;
        }

        public Builder setUseAdd(boolean useAdd) {
            this.useAdd = useAdd;
            return this;
        }

        public Builder setUseAddx(boolean useAddx) {
            this.useAddx = useAddx;
            return this;
        }

        public Builder setUseAdd2x(boolean useAdd2x) {
            this.useAdd2x = useAdd2x;
            return this;
        }

        public Builder setUseAdd3(boolean useAdd3) {
            this.useAdd3 = useAdd3;
            return this;
        }

        public Builder setUseSub(boolean useSub) {
            this.useSub = useSub;
            return this;
        }

        public Builder setUseSubx(boolean useSubx) {
            this.useSubx = useSubx;
            return this;
        }

        public Builder setUseSub2x(boolean useSub2x) {
            this.useSub2x = useSub2x;
            return this;
        }

        public Builder setUseDiv3(boolean useDiv3) {
            this.useDiv3 = useDiv3;
            return this;
        }

        public Builder setUseDiva(boolean useDiva) {
            this.useDiva = useDiva;
            return this;
        }

        public Builder setUseDiv4(boolean useDiv4) {
            this.useDiv4 = useDiv4;
            return this;
        }

        public Builder setUseDivq(boolean useDivq) {
            this.useDivq = useDivq;
            return this;
        }

        public Builder setUseRes(boolean useRes) {
            this.useRes = useRes;
            return this;
        }

        public Builder setUseRes2(boolean useRes2) {
            this.useRes2 = useRes2;
            return this;
        }

        public Builder setUseNeg(boolean useNeg) {
            this.useNeg = useNeg;
            return this;
        }

        public Builder setSubPowerOf2(boolean subPowerOf2) {
            this.subPowerOf2 = subPowerOf2;
            return this;
        }

    }

}
