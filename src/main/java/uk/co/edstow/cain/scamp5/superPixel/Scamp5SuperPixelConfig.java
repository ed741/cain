package uk.co.edstow.cain.scamp5.superPixel;

import uk.co.edstow.cain.scamp5.Scamp5Config;
import uk.co.edstow.cain.scamp5.output.Scamp5OutputFormatter;
import uk.co.edstow.cain.util.Bits;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Scamp5SuperPixelConfig extends Scamp5Config<Scamp5SuperPixelConfig> {

    public final boolean useMovbx;
    public final boolean useAdd;
    public final boolean useAddSelf;
    public final boolean useSub;
    public final boolean useDiv;
    public final boolean useRes;
    public final boolean useNeg;
    private final boolean onlyMov;

    public final List<String> scratchRegisters;
    public final String selectReg;
    public final String flagReg;
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
    public final boolean[][] sameShapeLookup;


    public Scamp5SuperPixelConfig(boolean useMovbx, boolean useAdd, boolean useAddSelf, boolean useSub, boolean useDiv, boolean useRes, boolean useNeg, List<String> scratchRegisters, String selectReg, String flagReg, String maskReg, String maskedReg, String northReg, String eastReg, String southReg, String westReg, int width, int height, int banks, int[][][] bitOrder, Scamp5OutputFormatter outputFormatter) {
        super(outputFormatter);
        this.useMovbx = useMovbx;
        this.useAdd = useAdd;
        this.useAddSelf = useAddSelf;
        this.useSub = useSub;
        this.useDiv = useDiv;
        this.useRes = useRes;
        this.useNeg = useNeg;

        this.onlyMov = !(this.useAdd || this.useDiv || this.useRes || this.useNeg);

        this.width = width;
        this.height = height;
        this.banks = banks;

        this.scratchRegisters = scratchRegisters;
        this.selectReg = selectReg;
        this.flagReg = flagReg;
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
        this.sameShapeLookup = new boolean[banks][banks];
        generateBankInfo();
    }

    public Scamp5SuperPixelConfig(Scamp5SuperPixelConfig proto, List<String> scratchRegisters, String selectReg, String flagReg, String maskReg, String maskedReg, String northReg, String eastReg, String southReg, String westReg, int width, int height, int banks, int[][][] bitOrder) {
        super(proto.outputFormatter);
        this.useMovbx = proto.useMovbx;
        this.useAdd = proto.useAdd;
        this.useAddSelf = proto.useAddSelf;
        this.useSub = proto.useSub;
        this.useDiv = proto.useDiv;
        this.useRes = proto.useRes;
        this.useNeg = proto.useNeg;

        this.onlyMov = !(this.useAdd || this.useAddSelf || this.useDiv || this.useRes || this.useNeg);


        this.width = width;
        this.height = height;
        this.banks = banks;

        this.scratchRegisters = scratchRegisters;
        this.selectReg = selectReg;
        this.flagReg = flagReg;
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
        this.sameShapeLookup = new boolean[banks][banks];
        generateBankInfo();

    }

    public Scamp5SuperPixelConfig(Scamp5SuperPixelConfig proto, boolean onlyMov) {
        super(proto.outputFormatter);
        this.useMovbx = proto.useMovbx;
        this.useAdd = proto.useAdd && (!onlyMov);
        this.useAddSelf = proto.useAddSelf && (!onlyMov);
        this.useSub = proto.useSub && (!onlyMov);
        this.useDiv = proto.useDiv && (!onlyMov);
        this.useRes = proto.useRes && (!onlyMov);
        this.useNeg = proto.useNeg && (!onlyMov);

        this.onlyMov = !(this.useAdd || this.useDiv || this.useRes);

        this.width = proto.width;
        this.height = proto.height;
        this.banks = proto.banks;

        this.scratchRegisters = proto.scratchRegisters;
        this.selectReg = proto.selectReg;
        this.flagReg = proto.flagReg;
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
        this.sameShapeLookup = proto.sameShapeLookup;
    }


    private void generateBankInfo() {
        for (int i = 0; i < this.banks; i++) {
            this.bits[i] = Arrays.stream(this.bitOrder[i]).mapToInt(is -> Arrays.stream(is).max().orElse(0)).max().orElse(0);

            loop:
            for (int x = 0; x < this.width; x++) {
                for (int y = 0; y < this.height; y++) {
                    if (this.bitOrder[i][x][y] == 1) {
                        this.xBankStart[i] = x;
                        this.yBankStart[i] = y;
                        break loop;
                    }
                }
            }
        }
        for (int i = 0; i < this.banks; i++) {
            for (int j = 0; j < this.banks; j++) {
                boolean same = true;
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        if(this.bitOrder[i][x][y]>0){
                            int jx = x-this.xBankStart[i]+this.xBankStart[j];
                            int jy = y-this.yBankStart[i]+this.yBankStart[j];
                            same &= 0 <= jx && jx < this.width &&
                                    0 <= jy && jy < this.height &&
                                    this.bitOrder[i][x][y] == this.bitOrder[j][jx][jy];
                        }
                    }
                }
                this.sameShapeLookup[i][j] = same;
            }
        }
    }

    public boolean onlyMov() {
        return onlyMov;
    }

    public int getBits(int bank) {
        return this.bits[bank];
    }
    public boolean isBankSameShape(int bank1, int bank2){
        return this.sameShapeLookup[bank1][bank2];
    }
    public boolean allSameShape() {
        for(boolean b: this.sameShapeLookup[0]) if(!b) return false;
        return true;
    }


    public Tuple<Integer, Integer> getBankTranslation(int fromBank, int toBank) {
        return new Tuple<>(xBankStart[toBank] - xBankStart[fromBank], yBankStart[toBank] - yBankStart[fromBank]);
    }

    public String getDirLessSignificant(int bank, int bit){
        return this.getDirSignificant(bank, bit, true);
    }
    public String getDirMoreSignificant(int bank, int bit){
        return this.getDirSignificant(bank, bit, false);
    }

    private String getDirSignificant(int bank, int bit, boolean lessSignificant){
        int x = -1;
        int y = -1;
        Dir direction = null;
        {
            bitSearch:
            for (int xPos = 0; xPos < this.width; xPos++) {
                for (int yPos = 0; yPos < this.height; yPos++) {
                    if (this.bitOrder[bank][xPos][yPos] == bit) {
                        x = xPos;
                        y = yPos;
                        break bitSearch;
                    }
                }
            }
        }
        assert x >= 0;
        for (int d = 0; d < Dir.values().length; d++) {
            Dir dir = Dir.values()[d];
            if (0 <= x + dir.x && x + dir.x < this.width &&
                    0 <= y + dir.y && y + dir.y < this.height &&
                    this.bitOrder[bank][x + dir.x][y + dir.y] == bit + (lessSignificant?-1:1) &&
                    bit + (lessSignificant?-1:1)>0
            ) {
                direction = dir;
                break;
            }
        }
        if(direction == null){
            throw new IllegalArgumentException("Cannot find next significant Direction");
        }
        switch (direction){

            case N:
                return "north";
            case E:
                return "east";
            case S:
                return "south";
            case W:
                return "west";
            default:
                throw new IllegalArgumentException("Unknown Direction!");
        }
    }

    @Override
    public Builder builder() {
        return new Builder(this);
    }

    private interface Marker {
        char u();
        char l();
        String getReg(Scamp5SuperPixelConfig config);
    }

    private static class SimpleMarker implements Marker {
        final char u;
        final char l;
        final String reg;

        private SimpleMarker(char u, char l, String reg) {
            this.u = u;
            this.l = l;
            this.reg = reg;
        }

        @Override
        public char u() {
            return u;
        }

        @Override
        public char l() {
            return l;
        }

        @Override
        public String getReg(Scamp5SuperPixelConfig config) {
            return reg;
        }
    }

    private enum Dir implements Marker {
        N(0,'N','n', 0,1, c->c.northReg),
        E(1,'E','e',1,0, c->c.eastReg),
        S(2,'S','s',0,-1, c->c.southReg),
        W(3,'W','w',-1,0, c->c.westReg);
        final char u;
        final char l;
        final int idx;
        final int x;
        final int y;
        private final Function<Scamp5SuperPixelConfig, String> getReg;
        Dir(int idx, char u, char l, int x, int y, Function<Scamp5SuperPixelConfig, String> getReg) {
            this.idx = idx;
            this.u = u;
            this.l = l;
            this.x = x;
            this.y = y;
            this.getReg = getReg;
        }

        @Override
        public String getReg(Scamp5SuperPixelConfig config){
            return this.getReg.apply(config);
        }
        int get(int[] arr){
            return arr[idx];
        }
        int inc(int[] arr){
            arr[idx] = arr[idx]+1;
            return arr[idx];
        }
        int dec(int[] arr){
            arr[idx] = arr[idx]-1;
            return arr[idx];
        }
        int add(int[] arr, int val){
            arr[idx] = arr[idx]+val;
            return arr[idx];
        }
        int set(int[] arr, int val){
            int t = arr[idx];
            arr[idx] = val;
            return t;
        }
        static int[] makeArray(int d){
            int[] ints = new int[Dir.values().length];
            Arrays.fill(ints, d);
            return ints;
        }
        boolean get(boolean[] arr){
            return arr[idx];
        }
        boolean or(boolean[] arr, boolean v){
            arr[idx] = arr[idx] || v;
            return arr[idx];
        }
        boolean and(boolean[] arr, boolean v){
            arr[idx] = arr[idx] && v;
            return arr[idx];
        }
        boolean set(boolean[] arr, boolean v){
            boolean t = arr[idx];
            arr[idx] =  v;
            return t;
        }
        static boolean[] makeArray(boolean d){
            boolean[] booleans = new boolean[Dir.values().length];
            Arrays.fill(booleans, d);
            return booleans;
        }
        static <V> Map<Dir, V> makeMap(Supplier<V> defaultVal){
            Map<Dir, V> map = new HashMap<>();
            for (Dir value : Dir.values()) {
                map.put(value, defaultVal.get());
            }
            return map;
        }
        static Dir getDir(char c){
            for (int i = 0; i < Dir.values().length; i++) {
                if(Dir.values()[i].u == c || Dir.values()[i].l == c) return Dir.values()[i];
            }
            throw new IllegalArgumentException("char isn't a Dir");
        }

        @Override
        public char u() {
            return u;
        }

        @Override
        public char l() {
            return l;
        }
    }

    private static class Pattern {
        public final byte xMask;
        public final byte yMask;
        public final byte xVal;
        public final byte yVal;
        public final int count;
        public final boolean[][] set;


        Pattern(int xMask, int yMask, int xVal, int yVal, int count, boolean[][] set) {
            this.xMask = (byte) xMask;
            this.yMask = (byte) yMask;
            this.xVal = (byte) xVal;
            this.yVal = (byte) yVal;
            this.count = count;
            this.set = set;
        }

        @Override
        public String toString() {
            return "Pattern{" +
                    "xMask=" + xMask +
                    ", yMask=" + yMask +
                    ", xVal=" + xVal +
                    ", yVal=" + yVal +
                    ", count=" + count +
                    ", set=" + Bits.booleanArrayToString(this.set) +
                    '}';
        }
    }
//    void printBitCharMask(char[][] dirs, int w, int h){
//        for (int y = h-1; y >= 0 ; y--) {
//            for (int x = 0; x < w; x++) {
//                System.out.print(dirs[x][y]=='\0'?'_':dirs[x][y]);
//                if(x<w-1) {
//                    System.out.print(',');
//                }
//            }
//            System.out.println();
//        }
//    }
//    void printBitSet(boolean[][] set, int w, int h){
//        for (int y = h-1; y >= 0 ; y--) {
//            for (int x = 0; x < w; x++) {
//                System.out.print(set[x][y]?'1':'0');
//                if(x<w-1) {
//                    System.out.print(',');
//                }
//            }
//            System.out.println();
//        }
//    }

    void selectBit(StringBuilder sb, int bank, int bit, String reg, boolean inKernelMode) {
        int x = -1;
        int y = -1;
        {
            bitSearch:
            for (int xPos = 0; xPos < this.width; xPos++) {
                for (int yPos = 0; yPos < this.height; yPos++) {
                    if (this.bitOrder[bank][xPos][yPos] == bit) {
                        x = xPos;
                        y = yPos;
                        break bitSearch;
                    }
                }
            }
            if(x<0) throw new IllegalArgumentException("Bit " + bit + " Not found in Bank "+ bank);
        }
        byte xMask = (byte) (256-this.width); // mask set to like this since only 1 PE in the SuperPixel can be set at
        byte yMask = (byte) (256-this.height);// a time using this method.
        byte xVal = (byte) (this.width - 1 - x);
        byte yVal = (byte) (this.height - 1 - y);
        if(inKernelMode) {
            sb.append(outputFormatter.kernel_end());
        }
        if(reg.equals(this.selectReg)) {
            sb.append(outputFormatter.select_pattern(yVal, xVal, yMask, xMask));
        } else {
            sb.append(outputFormatter.load_pattern(reg, yVal, xVal, yMask, xMask));
        }
        if(inKernelMode) {
            sb.append(outputFormatter.kernel_begin());
        }

    }

    void selectBank(StringBuilder sb, int bank, String reg, List<String> scratchRegisters) {
        char[][] mask = new char[this.width][this.height];
        Marker marker = new SimpleMarker('O', 'o', reg);
        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.height; y++) {
                mask[x][y] = this.bitOrder[bank][x][y] > 0? marker.u():'#';
            }
        }
//        printBitCharMask(mask, this.width, this.height);
        Map<Marker, List<Pattern>> patternList = generateBitMarkerPatterns(mask, Collections.singletonMap(marker, this.getBits(bank)));
        sb.append(outputFormatter.comment(String.format("SetSelectBank %s -> %s", bank, reg)));
        generateCodeForPatterns(sb, scratchRegisters, patternList);
        sb.append(outputFormatter.comment("done - SetSelectBank"));
    }

    void setValue(StringBuilder sb, int bank, boolean[] value, String reg, List<String> scratchRegisters) {
        Marker marker = new SimpleMarker('O', 'o', reg);
        assert value.length == getBits(bank);
        int count = 0;
        char[][] mask = new char[this.width][this.height];
        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.height; y++) {
                if (this.bitOrder[bank][x][y]>0 && value[this.bitOrder[bank][x][y] - 1]) {
                    count++;
                    mask[x][y] = marker.u();
                } else {
                    mask[x][y] = '#';
                }
            }
        }
//        printBitCharMask(mask, this.width, this.height);
        Map<Marker, List<Pattern>> patternList = generateBitMarkerPatterns(mask, Collections.singletonMap(marker, count));
        sb.append(outputFormatter.comment(String.format("SetValue %s:%s -> %s", bank, reg, Arrays.toString(value))));
        generateCodeForPatterns(sb, scratchRegisters, patternList);
        sb.append(outputFormatter.comment("done - SetValue"));
    }
    int selectFlood(StringBuilder sb, int bank, int bit, String reg, List<String> scratchRegisters) {
        return selectFlood(sb, bank, bit, reg, scratchRegisters, false, true);
    }
    int selectFlood(StringBuilder sb, int bank, int bit, String reg, List<String> scratchRegisters, boolean allBanks, boolean inKernelMode) {
        assert bit <= getBits(bank);
        Marker origin = new SimpleMarker('O', 'o', reg);
        Map<Marker, Integer> counts = new HashMap<>();
        if (reg != null) counts.put(origin, 1);
        Arrays.stream(Dir.values()).forEach(d-> counts.put(d, 0));

        int count = 0;
        final int maxBounds = this.width * this.height;
        char[][] mask = new char[this.width][this.height];
        int[][] distanceMap = new int[this.width][this.height];

        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.height; y++) {
                if(this.bitOrder[bank][x][y] == bit){
                    mask[x][y] = origin.u();
                    distanceMap[x][y] = maxBounds;

                } else if(!allBanks && this.bitOrder[bank][x][y] == 0){
                    mask[x][y] = '#';
                } else {
                    mask[x][y] = '~';
                    count++;
                }
            }
        }

        final int totalCount = count+1;
        int bounds = maxBounds;

        while (count>0 && bounds>0) {
            for (int x = 0; x < this.width; x++) {
                for (int y = 0; y < this.height; y++) {
                    if (mask[x][y] == '~') {
                        List<Dir> options = new ArrayList<>(Dir.values().length);
                        dirLoop: for (Dir d: Dir.values()) {
                            if (x + d.x >= 0 && x + d.x < this.width && y + d.y >= 0 && y + d.y < this.height) {
                                switch (mask[x + d.x][y + d.y]) {
                                    case '#':
                                    case '~':
                                        continue dirLoop;
                                    default:
                                        if(distanceMap[x + d.x][y + d.y] == bounds) {
                                            options.add(d);
                                        }
                                }
                            }
                        }
                        options.sort(Comparator.comparingInt((Dir d) -> d.x).thenComparingInt(d -> d.y));
                        if (!options.isEmpty()) {
                            mask[x][y] = options.get(0).u;
                            counts.computeIfPresent(options.get(0), (d, i) -> i+1);
                            distanceMap[x][y] = bounds-1;
                            count--;
                        }
                    }
                }
            }
            bounds--;
        }
        int maxDistance = maxBounds-bounds;
        assert count == 0;
        Map<Marker, List<Pattern>> patterns = generateBitMarkerPatterns(mask, counts);
        generateCodeForPatterns(sb, scratchRegisters, patterns, inKernelMode);
        return maxDistance;
    }

    void setDirLessSignificant(StringBuilder sb, int bank) {
        char[][] bitDir = new char[this.width][this.height];
        Map<Dir, Integer> count = generateBitDirectionArray(bank, bitDir, true, Arrays.asList(Dir.values()));
        Map<Dir, List<Pattern>> patterns = generateBitMarkerPatterns(bitDir, count);
        sb.append(outputFormatter.comment("SetDirLessSignificant"));
        generateCodeForPatterns(sb, Collections.singletonList(this.maskReg), patterns);
        sb.append(outputFormatter.comment("done - SetDirLessSignificant"));

    }

    void setDirMoreSignificant(StringBuilder sb, int bank) {
        char[][] bitDir = new char[this.width][this.height];
        Map<Dir, Integer> count = generateBitDirectionArray(bank, bitDir, false, Arrays.asList(Dir.values()));
        Map<Dir, List<Pattern>> patterns = generateBitMarkerPatterns(bitDir, count);
        sb.append(outputFormatter.comment("SetDirMoreSignificant"));
        generateCodeForPatterns(sb, Collections.singletonList(this.maskReg), patterns);
        sb.append("/*done - SetDirMoreSignificant*/");
    }

    public void setDirLessSignificantAndSelectBit(StringBuilder sb, int bank, int place, String reg, List<String> availableRegs) {
        sb.append(outputFormatter.comment("SetDirLessSignificantAndSelect"));
        setDirSignificantAndSelectBit(sb, bank, place, reg, true, availableRegs);
        sb.append(outputFormatter.comment("done - SetDirLessSignificantAndSelect"));
    }

    public void setDirMoreSignificantAndSelectBit(StringBuilder sb, int bank, int place, String reg, List<String> availableRegs) {
        sb.append(outputFormatter.comment("SetDirMoreSignificantAndSelect"));
        setDirSignificantAndSelectBit(sb, bank, place, reg, false, availableRegs);
        sb.append(outputFormatter.comment("done - SetDirMoreSignificantAndSelect"));
    }

    private void setDirSignificantAndSelectBit(StringBuilder sb, int bank, int place, String reg, boolean lessSignificant, List<String> availableRegs) {
        int x = -1;
        int y = -1;
        Dir direction = null;
        {
            bitSearch:
            for (int xPos = 0; xPos < this.width; xPos++) {
                for (int yPos = 0; yPos < this.height; yPos++) {
                    if (this.bitOrder[bank][xPos][yPos] == place) {
                        x = xPos;
                        y = yPos;
                        break bitSearch;
                    }
                }
            }
            if(x<0) throw new IllegalArgumentException("Bit " + place + " Not found in Bank "+ bank);
            for (int d = 0; d < Dir.values().length; d++) {
                Dir dir = Dir.values()[d];
                if (0 <= x + dir.x && x + dir.x < this.width &&
                        0 <= y + dir.y && y + dir.y < this.height &&
                        this.bitOrder[bank][x + dir.x][y + dir.y] == place + (lessSignificant?-1:1) &&
                        place + (lessSignificant?-1:1)>0) {
                    direction = dir;
                    break;
                }
            }
        }
        byte xMask = (byte) (256-this.width); // mask set to like this since only 1 PE in the SuperPixel can be set at
        byte yMask = (byte) (256-this.height);// a time using this method.
        byte xVal = (byte) (this.width - 1 - x);
        byte yVal = (byte) (this.height - 1 - y);

        sb.append(outputFormatter.kernel_end());
        if(reg.equals(this.selectReg)) {
            sb.append(outputFormatter.select_pattern(yVal, xVal, yMask, xMask));
        } else {
            sb.append(outputFormatter.load_pattern(reg, yVal, xVal, yMask, xMask));
        }
        sb.append(outputFormatter.kernel_begin());
        sb.append(outputFormatter.CLR(this.northReg, this.southReg, this.eastReg, this.westReg));
        if(direction != null){
                sb.append(outputFormatter.MOV(direction.getReg.apply(this), reg));
        }

    }

    private <M extends Marker> void generateCodeForPatterns(StringBuilder sb, List<String> scratchRegs, Map<M, List<Pattern>> patterns) {
        generateCodeForPatterns(sb, scratchRegs, patterns, true);
    }
    private <M extends Marker> void generateCodeForPatterns(StringBuilder sb, List<String> scratchRegs, Map<M, List<Pattern>> patterns, boolean inKernelMode) {
        List<Map.Entry<M, List<Pattern>>> patternList = patterns.entrySet().stream().sorted(Comparator.comparingInt(e -> -e.getValue().size())).collect(Collectors.toList());
        List<String> availableRegs = new ArrayList<>(scratchRegs);
        patterns.keySet().forEach(m -> {if (!availableRegs.contains(m.getReg(this))){availableRegs.add(m.getReg(this));}});
        List<String> toClear = new ArrayList<>();
        boolean leaveInKernelMode = inKernelMode;
        for (Map.Entry<M, List<Pattern>> entry : patternList) {
            M dir = entry.getKey();
            List<Pattern> list = entry.getValue();
            String reg = dir.getReg(this);
            sb.append(outputFormatter.comment(String.format("loading Dir: %s into %s", dir.u(), reg)));
            inKernelMode = generateCodeForPatternsReg(sb, availableRegs, toClear, reg, list, inKernelMode);
        }

        if(!toClear.isEmpty()){
            if(!inKernelMode) {sb.append(outputFormatter.kernel_begin()); inKernelMode = true;}
            sb.append(outputFormatter.CLR(toClear.toArray(new String[0])));
        }
        if(leaveInKernelMode && !inKernelMode) {
            sb.append(outputFormatter.kernel_begin());
        } else if (!leaveInKernelMode && inKernelMode) {
            sb.append(outputFormatter.kernel_end());
        }

    }

    private boolean generateCodeForPatternsReg(StringBuilder sb, List<String> availableRegs, List<String> toClear, String reg, List<Pattern> list, boolean inKernelMode) {
        availableRegs.remove(reg);
        if (list.isEmpty()){
            toClear.add(reg);
        } else if (list.size()==1){
            if(inKernelMode) {sb.append(outputFormatter.kernel_end()); inKernelMode = false;}
            Pattern pattern = list.get(0);
            if(reg.equals(this.selectReg)) {
                sb.append(outputFormatter.select_pattern(pattern.yVal, pattern.xVal, pattern.yMask, pattern.xMask));
            } else {
                sb.append(outputFormatter.load_pattern(reg, pattern.yVal, pattern.xVal, pattern.yMask, pattern.xMask));
            }
        } else if (list.size() <= 4 && list.size() <= availableRegs.size()+1) {
            if(reg.equals(this.selectReg)) {
                throw new IllegalArgumentException("Scamp5 cannot have multiple patterns together in select register: "+this.selectReg + " Patterns: " + list.toString());
            }
            if(inKernelMode) {sb.append(outputFormatter.kernel_end()); inKernelMode = false;}
            Pattern pattern;
            for (int i = 0; i < list.size() - 1; i++) {
                pattern = list.get(i);
                sb.append(outputFormatter.load_pattern(availableRegs.get(i), pattern.yVal, pattern.xVal, pattern.yMask, pattern.xMask));
            }
            pattern = list.get(list.size()-1);
            sb.append(outputFormatter.select_pattern(pattern.yVal, pattern.xVal, pattern.yMask, pattern.xMask));

            List<String> regs = new ArrayList<>();
            for (int i = 0; i < list.size() - 1; i++) {
                regs.add(availableRegs.get(i));
            }
            regs.add(this.selectReg);
            {sb.append(outputFormatter.kernel_begin()); inKernelMode = true;}
            sb.append(outputFormatter.OR(reg, regs.toArray(new String[0])));

        } else if (availableRegs.size() >= 1) {
            if(reg.equals(this.selectReg)) {
                throw new IllegalArgumentException("Scamp5 cannot have multiple patterns together in select register: "+this.selectReg + " Patterns: " + list.toString());
            }
            String regA = reg;
            String regB = availableRegs.get(0);
            String currentReg = regA;
            String altReg = regB;
            if(inKernelMode) {sb.append(outputFormatter.kernel_end()); inKernelMode = false;}
            Pattern pattern = list.get(0);
            sb.append(outputFormatter.load_pattern(currentReg, pattern.yVal, pattern.xVal, pattern.yMask, pattern.xMask));
            for (int i = 1; i < list.size(); i++) {
                pattern = list.get(i);
                if(inKernelMode) {sb.append(outputFormatter.kernel_end()); inKernelMode = false;}
                sb.append(outputFormatter.select_pattern(pattern.yVal, pattern.xVal, pattern.yMask, pattern.xMask));
                sb.append(outputFormatter.kernel_begin()); inKernelMode = true;
                sb.append(outputFormatter.OR(altReg, currentReg, this.selectReg));
                altReg = altReg.equals(regA) ?regB:regA;
                currentReg = currentReg.equals(regA) ?regB:regA;
            }
            if(!currentReg.equals(reg)) {
                if(!inKernelMode) {sb.append(outputFormatter.kernel_begin()); inKernelMode = true;}
                sb.append(outputFormatter.MOV(reg, currentReg));
            }
        } else {
            throw new IllegalArgumentException("More complex Direction-Pattern loading not supported. Ask a developer for support");
        }
        return inKernelMode;
    }


    private  Map<Dir, Integer> generateBitDirectionArray(int bank, char[][] bitDir, boolean lessSignificant, List<Dir> dirs) {
        int bits = this.getBits(bank);
        int xPos = -1;
        int yPos = -1;
        if(lessSignificant) {
            xPos = this.xBankStart[bank];
            yPos = this.yBankStart[bank];
        } else {
            boolean found = false;
            // start at the highest number (most significant bit)
            for (int x = 0; x < this.width; x++) {
                for (int y = 0; y < this.height; y++) {
                    if(this.bitOrder[bank][x][y] == bits){
                        xPos = x;
                        yPos = y;
                        found = true;
                        break;
                    }
                }
            }
            if (!found) throw new IllegalArgumentException("getBits("+bank+") returned returned "+bits+" but this value isn't in BitOrder");
        }
        int place;

        Map<Dir, Integer> count = new HashMap<>();
        dirs.forEach(d -> count.put(d, 0));
        bitDir[xPos][yPos] = 'O';// origin (least or most significant bit depending on 'lessSignificant')
        do {
            place = this.bitOrder[bank][xPos][yPos];
            for (Dir dir : dirs) {
                if (0 <= xPos - dir.x && xPos - dir.x < this.width &&
                        0 <= yPos - dir.y && yPos - dir.y < this.height &&
                        this.bitOrder[bank][xPos - dir.x][yPos - dir.y] == place + (lessSignificant ? 1 : -1) &&
                        place + (lessSignificant ? 1 : -1) > 0) {
                    bitDir[xPos - dir.x][yPos - dir.y] = dir.u;
                    count.computeIfPresent(dir, (d, c) -> c + 1);
                    xPos = xPos - dir.x;
                    yPos = yPos - dir.y;
                    break;
                }
            }
        } while (place != this.bitOrder[bank][xPos][yPos]);
//        printBitCharMask(bitDir, this.width, this.height);
        if (place != (lessSignificant?bits:1)) throw new IllegalArgumentException("Cannot find bits!");
        return count;
    }


    private <M extends Marker> Map<M, List<Pattern>> generateBitMarkerPatterns(char[][] bitDir, Map<M, Integer> countIn) {
        Map<M, Integer> count = new HashMap<>();
        countIn.forEach(count::put);

        Map<M, List<Pattern>> patterns = new HashMap<>();
        count.keySet().forEach(d -> patterns.put(d, new ArrayList<>()));


        List<Tuple<Integer, Integer>> masks = getMaskList();

        while(count.values().stream().mapToInt(i -> i).sum()>0){

            Map<M, List<Pattern>> nextPatterns = new HashMap<>();
            count.keySet().forEach(d -> nextPatterns.put(d, new ArrayList<>()));


            for (Tuple<Integer, Integer> mask : masks) {
                int xMask = mask.getA();
                int yMask = mask.getB();
                for (int xVal = 0; xVal < this.width; xVal++) {
                    for (int yVal = 0; yVal < this.height; yVal++) {
                        if ((xMask & xVal) > 0 || (yMask & yVal) > 0) continue;

                        boolean[][] set = getPatternSetArray(xMask, yMask, xVal, yVal);
//                        System.out.println("xMask: " + Bits.Str(xMask, 2) + " xVal: " + Bits.Str(xVal, 2) + " yMask: " + Bits.Str(yMask, 2) + " yVal: " + Bits.Str(yVal, 2));
//                        printBitSet(set, this.width, this.height);

                        Map<M, Boolean> match = new HashMap<>();
                        count.keySet().forEach(d -> match.put(d, true));

                        Map<M, Integer> newCount = new HashMap<>();
                        count.keySet().forEach(d -> newCount.put(d, 0));

                        checkMatch: for (int x = 0; x < this.width; x++) {
                            for (int y = 0; y < this.height; y++) {
                                if (set[x][y]) {
                                    for (M dir : count.keySet()) {
                                        boolean m = match.get(dir);
                                        match.put(dir, m && (bitDir[x][y] == dir.u() || bitDir[x][y] == dir.l() || bitDir[x][y] == 0));
                                        int c = newCount.get(dir);
                                        newCount.put(dir, c+(bitDir[x][y] == dir.u() ? 1 : 0));
                                    }
                                    if(match.values().stream().noneMatch(b->b)) {
                                       break checkMatch;
                                    }
                                }
                            }
                        }
//                        for (Dir dir : Dir.values()) {
//                            System.out.print(dir.u+":" + (dir.get(match) ? "T " : "F ") + dir.get(newCount) + ",  ");
//                        } System.out.println();


                        for (M dir : count.keySet()) {
                            if(match.get(dir) && newCount.get(dir)>0){
                                nextPatterns.get(dir).add(new Pattern(xMask, yMask, xVal, yVal, newCount.get(dir), set));
                                break;
                            }
                        }

                    }
                }
            }
            nextPatterns.forEach((dir, ps) -> ps.sort(Comparator.comparingInt(pattern -> -pattern.count)));
            for (M dir : count.keySet()) {
                List<Pattern> patternList = nextPatterns.get(dir);
                if(!patternList.isEmpty()){
                    Pattern pattern = patternList.get(0);
                    patterns.get(dir).add(pattern);
                    count.computeIfPresent(dir, (d, i) -> i-pattern.count);

                    for (int x = 0; x < this.width; x++) {
                        for (int y = 0; y < this.height; y++) {
                            if (pattern.set[x][y] && bitDir[x][y] != 0) {
                                assert bitDir[x][y] == dir.u() || bitDir[x][y] == dir.l();
                                bitDir[x][y] = dir.l();
                            }
                        }
                    }
                }
            }
//            printBitCharMask(bitDir, this.width, this.height);

        }
        if (count.values().stream().mapToInt(i -> i).sum()!=0) throw new IllegalArgumentException("Cannot find patterns to match bank");
        return patterns;
    }

    private List<Tuple<Integer, Integer>> getMaskList() {
        List<Tuple<Integer, Integer>> masks = new ArrayList<>();
        {
            for (int i = 0; i < this.width; i++) {
                for (int j = 0; j < this.height; j++) {
                    masks.add(new Tuple<>(256-this.width+i, 256-this.height+j));
                }
            }
            masks.sort(Comparator.comparingInt(a -> -Bits.countOnes(a.getA()) - Bits.countOnes(a.getB())));
        }
        return masks;
    }

    private boolean[][] getPatternSetArray(int xMask, int yMask, int xVal, int yVal) {
        boolean[][] set = new boolean[this.width][this.height];
        for (int x = 0; x < this.width; x++) {
            boolean xSet = true;
            for (int i = 0; i < Bits.log2nlz(this.width); i++) {
                if (!Bits.isOne(xMask, i) && Bits.isOne(xVal, i) != Bits.isOne(x, i)) {
                    xSet = false;
                    break;
                }
            }
            if (xSet) {
                for (int y = 0; y < this.height; y++) {
                    boolean ySet = true;
                    for (int i = 0; i < Bits.log2nlz(this.height); i++) {
                        if (!Bits.isOne(yMask, i) && Bits.isOne(yVal, i) != Bits.isOne(y, i)) {
                            ySet = false;
                            break;
                        }
                    }
                    set[this.width-1-x][this.height-1-y] = ySet;
                }
            }
        }
        return set;
    }


    public static final class Builder extends Scamp5ConfigBuilder<Scamp5SuperPixelConfig> {
        public boolean useMovbx;
        public boolean useAdd;
        public boolean useAddSelf;
        public boolean useSub;
        public boolean useDiv;
        public boolean useRes;
        public boolean useNeg;
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
        public List<String> scratchRegisters;
        public String selectReg;
        public String flagReg;

        public Builder() {
        }

        public Builder(Scamp5SuperPixelConfig src) {
            this.useMovbx = src.useMovbx;
            this.useAdd = src.useAdd;
            this.useAddSelf = src.useAddSelf;
            this.useSub = src.useSub;
            this.useDiv = src.useDiv;
            this.useRes = src.useRes;
            this.useNeg = src.useNeg;
            this.selectReg = src.selectReg;
            this.flagReg = src.flagReg;
            this.maskReg = src.maskReg;
            this.maskedReg = src.maskedReg;
            this.northReg = src.northReg;
            this.eastReg = src.eastReg;
            this.southReg = src.southReg;
            this.westReg = src.westReg;
            this.width = src.width;
            this.height = src.height;
            this.banks = src.banks;
            this.bitOrder = src.bitOrder;
            this.scratchRegisters = src.scratchRegisters;
            this.outputFormatter = src.outputFormatter;
        }

        public Builder useMovbx(boolean useMovbx) {
            this.useMovbx = useMovbx;
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

        public Builder useSub(boolean useSub) {
            this.useSub = useSub;
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

        public Builder useNeg(boolean useNeg) {
            this.useNeg = useNeg;
            return this;
        }

        public Builder scratchRegisters(List<String> scratchRegisters) {
            this.scratchRegisters = scratchRegisters;
            return this;
        }

        public Builder selectReg(String selectReg) {
            this.selectReg = selectReg;
            return this;
        }

        public Builder flagReg(String flagReg) {
            this.flagReg = flagReg;
            return this;
        }

        public Builder maskReg(String maskReg) {
            this.maskReg = maskReg;
            return this;
        }

        public Builder maskedReg(String maskedReg) {
            this.maskedReg = maskedReg;
            return this;
        }

        public Builder northReg(String northReg) {
            this.northReg = northReg;
            return this;
        }

        public Builder eastReg(String eastReg) {
            this.eastReg = eastReg;
            return this;
        }

        public Builder southReg(String southReg) {
            this.southReg = southReg;
            return this;
        }

        public Builder westReg(String westReg) {
            this.westReg = westReg;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder banks(int banks) {
            this.banks = banks;
            return this;
        }

        public Builder bitOrder(int[][][] bitOrder) {
            this.bitOrder = bitOrder;
            return this;
        }

        public Scamp5SuperPixelConfig build() {
            return new Scamp5SuperPixelConfig(useMovbx, useAdd, useAddSelf, useSub, useDiv, useRes, useNeg, scratchRegisters, selectReg, flagReg, maskReg, maskedReg, northReg, eastReg, southReg, westReg, width, height, banks, bitOrder, outputFormatter);
        }
    }
}
