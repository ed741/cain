package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.structures.Goal;

public class Scamp5Config<G extends Goal<G>> {

        public final boolean useMov2x;
        public final boolean useAdd3;

        public final boolean useAddx;
        public final boolean useAdd2x;
        public final boolean useSubx;
        public final boolean useSub2x;

        public final boolean subPowerOf2;

        public final boolean onlyMov;

    public Scamp5Config(boolean useMov2x, boolean useAdd3, boolean useAddx, boolean useAdd2x, boolean useSubx, boolean useSub2x, boolean subPowerOf2, boolean onlyMov) {
        this.useMov2x = useMov2x;
        this.useAdd3 = useAdd3;
        this.useAddx = useAddx;
        this.useAdd2x = useAdd2x;
        this.useSubx = useSubx;
        this.useSub2x = useSub2x;
        this.subPowerOf2 = subPowerOf2;
        this.onlyMov = onlyMov;
    }

    public static class Builder<G extends Goal<G>> {
        private boolean useMov2x;
        private boolean useAdd3;

        private boolean useAddx;
        private boolean useAdd2x;
        private boolean useSubx;
        private boolean useSub2x;

        private boolean subPowerOf2;

        private boolean onlyMov;

        public Builder(boolean useMov2x, boolean useAdd3, boolean useAddx, boolean useAdd2x, boolean useSubx, boolean useSub2x, boolean subPowerOf2, boolean onlyMov) {
            this.useMov2x = useMov2x;
            this.useAdd3 = useAdd3;
            this.useAddx = useAddx;
            this.useAdd2x = useAdd2x;
            this.useSubx = useSubx;
            this.useSub2x = useSub2x;
            this.subPowerOf2 = subPowerOf2;
            this.onlyMov = onlyMov;
        }

        public Builder() {
        }
        public Builder(Scamp5Config<G> config) {
            this.useMov2x = config.useMov2x;
            this.useAdd3 = config.useAdd3;
            this.useAddx = config.useAddx;
            this.useAdd2x = config.useAdd2x;
            this.useSubx = config.useSubx;
            this.useSub2x = config.useSub2x;
            this.subPowerOf2 = config.subPowerOf2;
            this.onlyMov = config.onlyMov;
        }

        public Scamp5Config<G> build(){
            return new Scamp5Config<>(useMov2x,  useAdd3,  useAddx,  useAdd2x,  useSubx,  useSub2x,  subPowerOf2,  onlyMov);
        }

        public Builder<G> useAll(){
            return this.setUseMov2x(true).setUseAdd3(true).setUseAddx(true).setUseAdd2x(true).setUseSubx(true).setUseSub2x(true);
        }

        public Builder<G> useBasic(){
            return this.setUseMov2x(false).setUseAdd3(false).setUseAddx(false).setUseAdd2x(false).setUseSubx(false).setUseSub2x(false);
        }

        public boolean isUseMov2x() {
            return useMov2x;
        }

        public Builder<G> setUseMov2x(boolean useMov2x) {
            this.useMov2x = useMov2x;
            return this;
        }

        public boolean isUseAdd3() {
            return useAdd3;
        }

        public Builder<G> setUseAdd3(boolean useAdd3) {
            this.useAdd3 = useAdd3;
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

        public boolean isSubPowerOf2() {
            return subPowerOf2;
        }

        public Builder<G> setSubPowerOf2(boolean subPowerOf2) {
            this.subPowerOf2 = subPowerOf2;
            return this;
        }

        public boolean isOnlyMov() {
            return onlyMov;
        }

        public Builder<G> setOnlyMov(boolean onlyMov) {
            this.onlyMov = onlyMov;
            return this;
        }
    }

}
