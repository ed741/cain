package uk.co.edstow.cain.scamp5.analogue;

import uk.co.edstow.cain.scamp5.Scamp5ConfigGetter;
import uk.co.edstow.cain.structures.Goal;

public class Scamp5AnalougeConfig<G extends Goal<G>> implements Scamp5ConfigGetter.Scamp5Config<G, Scamp5AnalougeConfig<G>> {

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

    private final boolean onlyMov;

    public Scamp5AnalougeConfig(boolean useMov, boolean useMovx, boolean useMov2x, boolean useAdd, boolean useAdd3, boolean useAddx, boolean useAdd2x, boolean useSub, boolean useSubx, boolean useSub2x, boolean useDiv3, boolean useDiva, boolean useDiv4, boolean useDivq, boolean useRes, boolean useRes2, boolean useNeg, boolean subPowerOf2) {
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

    public Scamp5AnalougeConfig(Scamp5AnalougeConfig<G> proto) {
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
    public boolean onlyMov() {
        return onlyMov;
    }

    @Override
    public Scamp5AnalougeConfig<G> getMovOnlyVersion() {
        return new Builder<>(this).setOnlyMov().build();
    }

    public static class Builder<G extends Goal<G>> {

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

        public Builder(boolean useMov, boolean useMovx, boolean useMov2x, boolean useAdd, boolean useAdd3, boolean useAddx, boolean useAdd2x, boolean useSub, boolean useSubx, boolean useSub2x, boolean useDiv3, boolean useDiva, boolean useDiv4, boolean useDivq, boolean useRes, boolean useRes2, boolean useNeg, boolean subPowerOf2) {
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
        public Builder(Scamp5AnalougeConfig<G> config) {
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

        public Scamp5AnalougeConfig<G> build(){
            return new Scamp5AnalougeConfig<>(useMov, useMovx, useMov2x, useAdd, useAdd3,  useAddx,  useAdd2x, useSub, useSubx,  useSub2x, useDiv3, useDiva, useDiv4, useDivq, useRes, useRes2, useNeg, subPowerOf2);
        }

        public Builder<G> useAll(){
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

        public Builder<G> useBasic(){
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

        public Builder<G> setOnlyMov() {
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

        public boolean isUseMov() {
            return useMov;
        }

        public Builder<G> setUseMov(boolean useMov) {
            this.useMov = useMov;
            return this;
        }

        public boolean isUseMovx() {
            return useMovx;
        }

        public Builder<G> setUseMovx(boolean useMovx) {
            this.useMovx = useMovx;
            return this;
        }

        public boolean isUseMov2x() {
            return useMov2x;
        }

        public Builder<G> setUseMov2x(boolean useMov2x) {
            this.useMov2x = useMov2x;
            return this;
        }

        public boolean isUseAdd() {
            return useAdd;
        }

        public Builder<G> setUseAdd(boolean useAdd) {
            this.useAdd = useAdd;
            return this;
        }

        public boolean isUseAddx() {
            return useAddx;
        }

        public Builder<G> setUseAddx(boolean useAddx) {
            this.useAddx = useAddx;
            return this;
        }

        public boolean isUseAdd2x() {
            return useAdd2x;
        }

        public Builder<G> setUseAdd2x(boolean useAdd2x) {
            this.useAdd2x = useAdd2x;
            return this;
        }

        public boolean isUseAdd3() {
            return useAdd3;
        }

        public Builder<G> setUseAdd3(boolean useAdd3) {
            this.useAdd3 = useAdd3;
            return this;
        }

        public boolean isUseSub() {
            return useSub;
        }

        public Builder<G> setUseSub(boolean useSub) {
            this.useSub = useSub;
            return this;
        }

        public boolean isUseSubx() {
            return useSubx;
        }

        public Builder<G> setUseSubx(boolean useSubx) {
            this.useSubx = useSubx;
            return this;
        }

        public boolean isUseSub2x() {
            return useSub2x;
        }

        public Builder<G> setUseSub2x(boolean useSub2x) {
            this.useSub2x = useSub2x;
            return this;
        }

        public boolean isUseDiv3() {
            return useDiv3;
        }

        public Builder<G> setUseDiv3(boolean useDiv3) {
            this.useDiv3 = useDiv3;
            return this;
        }

        public boolean isUseDiva() {
            return useDiva;
        }

        public Builder<G> setUseDiva(boolean useDiva) {
            this.useDiva = useDiva;
            return this;
        }

        public boolean isUseDiv4() {
            return useDiv4;
        }

        public Builder<G> setUseDiv4(boolean useDiv4) {
            this.useDiv4 = useDiv4;
            return this;
        }

        public boolean isUseDivq() {
            return useDivq;
        }

        public Builder<G> setUseDivq(boolean useDivq) {
            this.useDivq = useDivq;
            return this;
        }

        public boolean isUseRes() {
            return useRes;
        }

        public Builder<G> setUseRes(boolean useRes) {
            this.useRes = useRes;
            return this;
        }

        public boolean isUseRes2() {
            return useRes2;
        }

        public Builder<G> setUseRes2(boolean useRes2) {
            this.useRes2 = useRes2;
            return this;
        }

        public boolean isUseNeg() {
            return useNeg;
        }

        public Builder<G> setUseNeg(boolean useNeg) {
            this.useNeg = useNeg;
            return this;
        }

        public boolean isSubPowerOf2() {
            return subPowerOf2;
        }

        public Builder<G> setSubPowerOf2(boolean subPowerOf2) {
            this.subPowerOf2 = subPowerOf2;
            return this;
        }
    }

}
