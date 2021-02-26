package uk.co.edstow.cain.scamp5.superPixel;

import uk.co.edstow.cain.scamp5.Scamp5ConfigGetter;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.util.Tuple;

import java.util.Arrays;
import java.util.List;

public class Scamp5SuperPixelConfig<G extends Goal<G>> implements Scamp5ConfigGetter.Scamp5Config<G, Scamp5SuperPixelConfig<G>> {


    public Scamp5SuperPixelConfig(boolean useMovbx, boolean useAdd, boolean useAddSelf, boolean useDiv, boolean useRes, List<String> scratchRegisters, String selectReg, String maskReg, String maskedReg, String northReg, String eastReg, String southReg, String westReg, int width, int height, int banks, int[][][] bitOrder) {
        this.useMovbx = useMovbx;
        this.useAdd = useAdd;
        this.useAddSelf = useAddSelf;
        this.useDiv = useDiv;
        this.useRes = useRes;

        this.onlyMov = !(this.useAdd || this.useDiv || this.useRes);

        this.width = width;
        this.height = height;
        this.banks = banks;

        this.scratchRegisters = scratchRegisters;
        this.selectReg = selectReg;
        this.maskReg = maskReg;
        this.maskedReg = maskedReg;
        this.northReg = northReg;
        this.eastReg = eastReg;
        this.southReg = southReg;
        this.westReg = westReg;


        this.bitOrder = bitOrder;
        this.bits = new int[banks];
        this.xBankStart = new int[banks];
        this.yBankStart = new int[banks];
        for (int i = 0; i < this.banks; i++) {
            this.bits[i] = Arrays.stream(this.bitOrder[i]).mapToInt(is -> Arrays.stream(is).max().orElse(0)).max().orElse(0);

            loop: for (int x = 0; x < this.width; x++) {
                for (int y = 0; y < this.height; y++) {
                    if(this.bitOrder[i][x][y] == 1){
                        this.xBankStart[i] = x;
                        this.yBankStart[i] = y;
                        break loop;
                    }
                }
            }
        }
    }

    public final boolean useMovbx;
    public final boolean useAdd;
    public final boolean useAddSelf;
    public final boolean useDiv;
    public final boolean useRes;
    private final boolean onlyMov;

    final List<String> scratchRegisters;
    final String selectReg;
    public final String maskReg;
    public final String maskedReg;
    public final String northReg;
    public final String eastReg;
    public final String southReg;
    public final String westReg;

    public final int width;
    public final int height;
    public final int banks;

    public final int[][][] bitOrder;
    public final int[] bits;
    public final int[] xBankStart;
    public final int[] yBankStart;

    public Scamp5SuperPixelConfig(Scamp5SuperPixelConfig<G> proto, List<String> scratchRegisters, String selectReg, String maskReg, String maskedReg, String northReg, String eastReg, String southReg, String westReg, int width, int height, int banks, int[][][] bitOrder) {
        this.useMovbx = proto.useMovbx;
        this.useAdd = proto.useAdd;
        this.useAddSelf = proto.useAddSelf;
        this.useDiv = proto.useDiv;
        this.useRes = proto.useRes;


        this.onlyMov = !(this.useAdd || this.useAddSelf || this.useDiv || this.useRes);


        this.width = width;
        this.height = height;
        this.banks = banks;

        this.scratchRegisters = scratchRegisters;
        this.selectReg = selectReg;
        this.maskReg = maskReg;
        this.maskedReg = maskedReg;
        this.northReg = northReg;
        this.eastReg = eastReg;
        this.southReg = southReg;
        this.westReg = westReg;

        this.bitOrder = bitOrder;
        this.bits = new int[banks];
        this.xBankStart = new int[banks];
        this.yBankStart = new int[banks];
        for (int i = 0; i < this.banks; i++) {
            this.bits[i] = Arrays.stream(this.bitOrder[i]).mapToInt(is -> Arrays.stream(is).max().orElse(0)).max().orElse(0);

            loop: for (int x = 0; x < this.width; x++) {
                for (int y = 0; y < this.height; y++) {
                    if(this.bitOrder[i][x][y] == 1){
                        this.xBankStart[i] = x;
                        this.yBankStart[i] = y;
                        break loop;
                    }
                }
            }
        }

    }

    public Scamp5SuperPixelConfig(Scamp5SuperPixelConfig<G> proto, boolean onlyMov) {
        this.useMovbx = proto.useMovbx;
        this.useAdd = proto.useAdd && (!onlyMov);
        this.useAddSelf = proto.useAddSelf && (!onlyMov);
        this.useDiv = proto.useDiv && (!onlyMov);
        this.useRes = proto.useRes && (!onlyMov);

        this.onlyMov = !(this.useAdd || this.useDiv || this.useRes);

        this.width = proto.width;
        this.height = proto.height;
        this.banks = proto.banks;

        this.scratchRegisters = proto.scratchRegisters;
        this.selectReg = proto.selectReg;
        this.maskReg = proto.maskReg;
        this.maskedReg = proto.maskedReg;
        this.northReg = proto.northReg;
        this.eastReg = proto.eastReg;
        this.southReg = proto.southReg;
        this.westReg = proto.westReg;

        this.bitOrder = proto.bitOrder;
        this.bits = proto.bits;
        this.xBankStart = proto.xBankStart;
        this.yBankStart = proto.yBankStart;


    }

    @Override
    public boolean onlyMov() {
        return onlyMov;
    }

    @Override
    public Scamp5SuperPixelConfig<G> getMovOnlyVersion() {
        return new Scamp5SuperPixelConfig<>(this, true);
    }

    public int getBits(int bank) {
        return this.bits[bank];
    }
    public boolean isBankSameShape(int bank1, int bank2){
        return Arrays.deepEquals(this.bitOrder[bank1], this.bitOrder[bank2]);
    }

    public Tuple<Integer, Integer> getBankTranslation(int fromBank, int toBank) {
        return new Tuple<>(xBankStart[toBank] - xBankStart[fromBank], yBankStart[toBank] - yBankStart[fromBank]);
    }


    public static final class Builder<G extends Goal<G>> {
        public boolean useMovbx;
        public boolean useAdd;
        public boolean useAddSelf;
        public boolean useDiv;
        public boolean useRes;
        public String maskReg;
        public String maskedReg;
        public String northReg;
        public String eastReg;
        public String southReg;
        public String westReg;
        public int width;
        public int height;
        public int banks;
        public int[][][] bitOrder;
        List<String> scratchRegisters;
        String selectReg;

        public Builder() {
        }

        public Builder<G> setUseMovbx(boolean useMovbx) {
            this.useMovbx = useMovbx;
            return this;
        }

        public Builder<G> setUseAdd(boolean useAdd) {
            this.useAdd = useAdd;
            return this;
        }

        public Builder<G> setUseAddSelf(boolean useAddSelf) {
            this.useAddSelf = useAddSelf;
            return this;
        }

        public Builder<G> setUseDiv(boolean useDiv) {
            this.useDiv = useDiv;
            return this;
        }

        public Builder<G> setUseRes(boolean useRes) {
            this.useRes = useRes;
            return this;
        }

        public Builder<G> setScratchRegisters(List<String> scratchRegisters) {
            this.scratchRegisters = scratchRegisters;
            return this;
        }

        public Builder<G> setSelectReg(String selectReg) {
            this.selectReg = selectReg;
            return this;
        }

        public Builder<G> setMaskReg(String maskReg) {
            this.maskReg = maskReg;
            return this;
        }

        public Builder<G> setMaskedReg(String maskedReg) {
            this.maskedReg = maskedReg;
            return this;
        }

        public Builder<G> setNorthReg(String northReg) {
            this.northReg = northReg;
            return this;
        }

        public Builder<G> setEastReg(String eastReg) {
            this.eastReg = eastReg;
            return this;
        }

        public Builder<G> setSouthReg(String southReg) {
            this.southReg = southReg;
            return this;
        }

        public Builder<G> setWestReg(String westReg) {
            this.westReg = westReg;
            return this;
        }

        public Builder<G> setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder<G> setHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder<G> setBanks(int banks) {
            this.banks = banks;
            return this;
        }

        public Builder<G> setBitOrder(int[][][] bitOrder) {
            this.bitOrder = bitOrder;
            return this;
        }

        public Scamp5SuperPixelConfig<G> build() {
            return new Scamp5SuperPixelConfig<>(useMovbx, useAdd, useAddSelf, useDiv, useRes, scratchRegisters, selectReg, maskReg, maskedReg, northReg, eastReg, southReg, westReg, width, height, banks, bitOrder);
        }
    }
}
