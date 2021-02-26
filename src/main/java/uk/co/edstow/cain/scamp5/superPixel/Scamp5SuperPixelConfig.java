package uk.co.edstow.cain.scamp5.superPixel;

import uk.co.edstow.cain.regAlloc.BankedLinearScanRegisterAllocator;
import uk.co.edstow.cain.scamp5.Scamp5ConfigGetter;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.util.Tuple;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
}
