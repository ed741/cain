package uk.co.edstow.cain.goals.arrayGoal;

import uk.co.edstow.cain.goals.BankedKernel3DGoal;
import uk.co.edstow.cain.goals.atomGoal.Atom;
import uk.co.edstow.cain.goals.atomGoal.AtomGoal;
import uk.co.edstow.cain.structures.Bounds;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;

public final class BankedArrayGoal implements BankedKernel3DGoal<BankedArrayGoal> {

    private final int bank;
    private final int[] arr;
    private final int total;
    private final int centerX;
    private final int centerY;
    private final int centerZ;
    private final int xSize;
    private final int ySize;
    private final int zSize;

    protected BankedArrayGoal(int bank, int[] array, int centerX, int centerY, int centerZ, int xSize, int ySize, int zSize, int total) {
        this.bank = bank;
        this.arr = array;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
        this.total = total;
    }

    protected BankedArrayGoal(int bank, int[] array, int centerX, int centerY, int centerZ, int xSize, int ySize, int zSize) {
        this.bank = bank;
        this.arr = array;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
        int t = 0;
        for (int value: this.arr) {
            t += Math.abs(value);
        }
        this.total = t;
    }


    // constructor that resizes array appropriately
    public BankedArrayGoal(int bank, int centerX, int centerY, int centerZ, int[] array, int xSize, int ySize, int zSize) {
        this.bank = bank;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        boolean empty = true;
        for (int x = -centerX; x < xSize-centerX; x++) {
            for (int y = -centerY; y < ySize-centerY; y++) {
                for (int z = -centerZ; z < zSize-centerZ; z++) {
                    int xPos = x + centerX;
                    int yPos = y + centerY;
                    int zPos = z + centerZ;
                    if (array[(xPos*(ySize) + yPos)*(zSize) + zPos] != 0 ) {
                        empty = false;
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                        minZ = Math.min(minZ, z);
                        maxZ = Math.max(maxZ, z);
                    }
                }
            }
        }
        if (empty){
            this.centerX = 0;
            this.centerY = 0;
            this.centerZ = 0;
            this.xSize = 1;
            this.ySize = 1;
            this.zSize = 1;
            this.arr = new int[1];
            this.total = 0;
        } else {
            int realSizeX = maxX - minX + 1;
            int realSizeY = maxY - minY + 1;
            int realSizeZ = maxZ - minZ + 1;

            this.centerX = -minX;
            this.centerY = -minY;
            this.centerZ = -minZ;
            this.xSize = realSizeX;
            this.ySize = realSizeY;
            this.zSize = realSizeZ;
            if (realSizeX == xSize && realSizeY == ySize && realSizeZ == zSize) {
                this.arr = array;
            } else {
                int[] narray = new int[this.xSize * this.ySize * this.zSize];
                for (int x = -this.centerX; x < this.xSize - this.centerX; x++) {
                    for (int y = -this.centerY; y < this.ySize - this.centerY; y++) {
                        for (int z = -this.centerZ; z < this.zSize - this.centerZ; z++) {
                            int theirC = array[((x + centerX) * (ySize) + (y + centerY)) * (zSize) + (z + centerZ)];
                            narray[((x + this.centerX) * (this.ySize) + (y + this.centerY)) * (this.zSize) + (z + this.centerZ)] = theirC;
                        }
                    }
                }
                this.arr = narray;
            }
            int t = 0;
            for (int value : this.arr) {
                t += Math.abs(value);
            }
            this.total = t;
        }
    }

    public BankedArrayGoal(int bank, AtomGoal atoms) {
        this.bank = bank;
        Bounds b = atoms.bounds();
        this.centerX = -b.getXMin();
        this.centerY = -b.getYMin();
        this.centerZ = -b.getZMin();
        this.xSize = b.getXMax() - b.getXMin()+1;
        this.ySize = b.getYMax() - b.getYMin()+1;
        this.zSize = b.getZMax() - b.getZMin()+1;
        this.arr = new int[this.xSize*this.ySize*this.zSize];
        Iterator<Tuple<Atom, Integer>> tupleIterator = atoms.uniqueAtomCountIterator();
        int t = 0;
        while (tupleIterator.hasNext()){
            Tuple<Atom, Integer> next = tupleIterator.next();
            t += Math.abs(next.getB());
            this.arr[((next.getA().x+this.centerX)*(this.ySize) + (next.getA().y+this.centerY)) * (this.zSize) + (next.getA().z+this.centerZ)] = next.getA().positive? next.getB() : -next.getB();
        }
        this.total = t;
    }

    public BankedArrayGoal(int bank){
        this.bank = bank;
        this.centerX = 0;
        this.centerY = 0;
        this.centerZ = 0;
        this.xSize = 1;
        this.ySize = 1;
        this.zSize = 1;
        this.arr = new int[1];
        this.total = 0;
    }

    public BankedArrayGoal(int bank, BankedArrayGoal goal) {
        this.bank = bank;
        this.arr = goal.arr;
        this.centerX = goal.centerX;
        this.centerY = goal.centerY;
        this.centerZ = goal.centerZ;
        this.xSize = goal.xSize;
        this.ySize = goal.ySize;
        this.zSize = goal.zSize;
        this.total = goal.total;
    }

    public BankedArrayGoal(BankedArrayGoal goal) {
        this.bank = goal.bank;
        this.arr = goal.arr;
        this.centerX = goal.centerX;
        this.centerY = goal.centerY;
        this.centerZ = goal.centerZ;
        this.xSize = goal.xSize;
        this.ySize = goal.ySize;
        this.zSize = goal.zSize;
        this.total = goal.total;
    }

    @Override
    public String toString() {
        return "ArrayGoal{" +
                "bank=" + bank +
                ", arr=" + Arrays.toString(arr) +
                ", total=" + total +
                ", centerX=" + centerX +
                ", centerY=" + centerY +
                ", centerZ=" + centerZ +
                ", xSize=" + xSize +
                ", ySize=" + ySize +
                ", zSize=" + zSize +
                '}';
    }

    @Override
    public boolean allZero() {
        return this.total==0;
    }

    @Override
    public int get(int x, int y, int z){
        int xPos = x + centerX;
        if(xPos < 0 || xPos >= xSize) return 0;
        int yPos = y + centerY;
        if(yPos < 0 || yPos >= ySize) return 0;
        int zPos = z + centerZ;
        if(zPos < 0 || zPos >= zSize) return 0;
        return this.arr[(xPos*(ySize) + yPos)*(zSize) + zPos];

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankedArrayGoal arrayGoal = (BankedArrayGoal) o;
        return this.same(arrayGoal);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(total, centerX, centerY, centerZ, xSize, ySize, zSize, bank);
        result = 31 * result + Arrays.hashCode(arr);
        return result;
    }

    @Override
    public boolean same(BankedArrayGoal goal) {
        if (this.equivalent(goal)) {
            return true;
        }

        if (this.bank == goal.bank &&
                this.centerX == goal.centerX &&
                this.centerY == goal.centerY &&
                this.centerZ == goal.centerZ &&
                this.xSize == goal.xSize &&
                this.ySize == goal.ySize &&
                this.zSize == goal.zSize &&
                this.total == goal.total){
            return Arrays.equals(arr, goal.arr);
        } else {
            return false;
        }


    }

    @Override
    public boolean equivalent(BankedArrayGoal goal) {
        return goal==this;
    }

    @Override
    public double total() {
        return this.total;
    }

    public int totalI() {
        return this.total;
    }

    @Override
    public boolean hasSubGoal(BankedArrayGoal a) {
        if(bank != a.bank) return false;
        for (int x = -a.centerX; x < a.xSize-a.centerX; x++) {
            for (int y = -a.centerY; y < a.ySize-a.centerY; y++) {
                for (int z = -a.centerZ; z < a.zSize-a.centerZ; z++) {
                    int xPos = x + a.centerX;
                    int yPos = y + a.centerY;
                    int zPos = z + a.centerZ;
                    int idx = (xPos*(a.ySize) + yPos)*(a.zSize) + zPos;
                    int theirC = a.arr[idx];
                    int thisC = get(x, y, z);
                    if (theirC > 0 && theirC > thisC) return false;
                    if (theirC < 0 && theirC < thisC) return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean allSame() {
        return xSize==1 && ySize==1 && zSize==1;
    }

    @Override
    public int minimumCount() {
        boolean seen = false;
        int min = Integer.MAX_VALUE;
        for (int i : arr) {
            if (i != 0) {
                int abs = Math.abs(i);
                if (abs < min) {
                    seen = true;
                    min = abs;
                }
            }
        }
        return seen ? min : 0;
    }

    @Override
    public int maximumCount() {
        boolean seen = false;
        int max = Integer.MIN_VALUE;
        for (int i : arr) {
            if (i != 0) {
                int abs = Math.abs(i);
                if (abs > max) {
                    seen = true;
                    max = abs;
                }
            }
        }
        return seen ? max : 0;
    }

    @Override
    public boolean isTranslation(BankedArrayGoal pattern) {
        if(this.xSize != pattern.xSize || this.ySize != pattern.ySize || this.zSize != pattern.zSize){
            return false;
        }
        return Arrays.equals(this.arr, pattern.arr);

    }

    @Override
    public BankedArrayGoal copy() {
        return new BankedArrayGoal(this);
    }

    @Override
    public String[][] getCharTable(Bounds b, boolean topBorder, boolean bottomBorder, boolean centreDot, boolean colourNeg) {

        int width = 1+b.getXMax() - b.getXMin();
        int height = 1+b.getYMax() - b.getYMin();
        int depth = 1+b.getZMax() - b.getZMin();
        int[][][] intArray = new int[width][height][depth];
        int ixZero = -b.getXMin();
        int iyZero = -b.getYMin();
        int izZero = -b.getZMin();
        {
            for (int x = b.getXMin(); x <= b.getXMax(); x++) {
                for (int y = b.getYMin(); y <= b.getYMax(); y++) {
                    for (int z = b.getZMin(); z <= b.getZMax(); z++) {
                        int c = this.get(x, y, z);

                        int ax = ixZero + x;
                        int ay = iyZero + y;
                        int az = izZero + z;

                        intArray[ax][ay][az] = c;

                    }
                }
            }
        }
        String[][] tableArray = new String[height + 2][width + 2];
        {
            int xZero = 1 - b.getXMin();
            int yZero = 1 - b.getYMin();
            for (int x = b.getXMin() - 1; x <= b.getXMax() + 1; x++) {
                for (int y = b.getYMin() - 1; y <= b.getYMax() + 1; y++) {
                    if (b.getXMin() <= x && x <= b.getXMax() && b.getYMin() <= y && y <= b.getYMax()) {

                        StringBuilder sb = new StringBuilder();
                        boolean zeros = true;
                        if(depth>1){sb.append('[');}
                        for (int z = 0; z < intArray[ixZero + x][iyZero + y].length; z++) {
                            int count = intArray[ixZero + x][iyZero + y][z];
                            zeros &= count == 0;
                            boolean neg = intArray[ixZero + x][iyZero + y][z] < 0;
                            if (neg) {
                                sb.append(colourNeg ? "\u001B[34m" : "-");//blue
                            }
                            sb.append(Math.abs(count));
                            if (neg && colourNeg) {
                                sb.append("\u001B[0m");//reset

                            }
                            if(z<intArray[ixZero + x][iyZero + y].length-1){
                                sb.append(",");
                            }
                        }
                        if(depth>1){sb.append(']');}
                        if(centreDot && zeros && x==0 && y==0){
                            if(depth == 1) {
                                tableArray[yZero + y][xZero + x] = ".";
                            } else {
                                StringBuilder dot = new StringBuilder("[");
                                for(int k = 0; k < depth-1;k++){
                                    dot.append(" ");
                                }
                                dot.append(".");
                                for(int k = 0; k < depth-1;k++){
                                    dot.append(" ");
                                }
                                dot.append("]");
                                tableArray[yZero + y][xZero + x] = dot.toString();
                            }
                        } else {
                            tableArray[yZero + y][xZero + x] = sb.toString();
                        }
                    } else if (x == 0) {
                        tableArray[yZero + y][xZero + x] = "|";
                    } else if (y == 0) {
                        tableArray[yZero + y][xZero + x] = "-";
                    } else if (yZero + y == 0 && bottomBorder) {
                        tableArray[yZero + y][xZero + x] = "v";
                    } else if (yZero + y == height + 1 && topBorder) {
                        tableArray[yZero + y][xZero + x] = "v";
                    } else {
                        tableArray[yZero + y][xZero + x] = "+";
                    }
                }
            }
        }
        tableArray[0][0] = Integer.toString(bank);
        return tableArray;
    }

    @Override
    public Bounds bounds() {
        return new Bounds.SimpleBounds(this.xSize-this.centerX-1, -this.centerX,
                this.ySize-this.centerY-1, -this.centerY,
                this.zSize-this.centerZ-1, -this.centerZ);
    }

    @Override
    public int compareTo(BankedArrayGoal arrayGoal) {
        if (arrayGoal == this) return 0;
        int c = Integer.compare(arrayGoal.bank, this.bank);
        if (c != 0) return c;
        c = Integer.compare(arrayGoal.total, this.total);
        if (c != 0) return c;
        c = Integer.compare(arrayGoal.centerX, this.centerX);
        if (c != 0) return c;
        c = Integer.compare(arrayGoal.centerY, this.centerY);
        if (c != 0) return c;
        c = Integer.compare(arrayGoal.centerZ, this.centerZ);
        if (c != 0) return c;
        c = Integer.compare(arrayGoal.xSize, this.xSize);
        if (c != 0) return c;
        c = Integer.compare(arrayGoal.ySize, this.ySize);
        if (c != 0) return c;
        c = Integer.compare(arrayGoal.zSize, this.zSize);
        if (c != 0) return c;
        for (int i = 0; i < arr.length; i++) {
            c = Integer.compare(arrayGoal.arr[i], this.arr[i]);
            if (c != 0) return c;
        }
        return 0;
    }

    @Override
    public BankedArrayGoal without(BankedArrayGoal goal) {
        if(goal.total == 0){
            return new BankedArrayGoal(this);
        }
        int[] narr = Arrays.copyOf(this.arr, this.arr.length);

        for (int x = -this.centerX; x < this.xSize-this.centerX; x++) {
            for (int y = -this.centerY; y < this.ySize-this.centerY; y++) {
                for (int z = -this.centerZ; z < this.zSize-this.centerZ; z++) {
                    int xPos = x + centerX;
                    int yPos = y + centerY;
                    int zPos = z + centerZ;
                    int idx = (xPos*(ySize) + yPos)*(zSize) + zPos;
                    int theirC = goal.get(x, y, z);
                    int thisC = narr[idx];
                    if(theirC < 0 && thisC < 0) {
                        if(theirC < thisC){
                            narr[idx]= 0;
                        } else {
                            narr[idx] = thisC - theirC;
                        }
                    } else if (theirC > 0 && thisC > 0) {
                        if(theirC > thisC){
                            narr[idx]= 0;
                        } else {
                            narr[idx] = thisC - theirC;
                        }
                    }
                }
            }
        }
        return new BankedArrayGoal(bank, centerX, centerY, centerZ, narr, xSize, ySize, zSize);
    }

    @Override
    public BankedArrayGoal negated() {
        int[] narr = Arrays.copyOf(this.arr, this.arr.length);
        for (int i = 0; i < narr.length; i++) {
            narr[i] = -narr[i];
        }
        return new BankedArrayGoal(bank, narr, this.centerX, this.centerY, this.centerZ, this.xSize, this.ySize, this.zSize, this.total);
    }

    @Override
    public BankedArrayGoal translated(int x, int y, int z){
        return new BankedArrayGoal(bank, this.arr, this.centerX-x, this.centerY-y, this.centerZ-z, this.xSize, this.ySize, this.zSize, this.total);
    }

    @Override
    public BankedArrayGoal added(BankedArrayGoal goal) {
        if(goal.total == 0){
            return new BankedArrayGoal(this);
        }
        Bounds b = new Bounds.SimpleBounds(Arrays.asList(this.bounds(), goal.bounds()));
        int centerX = -b.getXMin();
        int centerY = -b.getYMin();
        int centerZ = -b.getZMin();
        int xSize = b.getXMax()-b.getXMin()+1;
        int ySize = b.getYMax()-b.getYMin()+1;
        int zSize = b.getZMax()-b.getZMin()+1;
        int[] narr = new int[xSize * ySize * zSize];
        for (int x = b.getXMin(); x <= b.getXMax() ; x++) {
            for (int y = b.getYMin(); y <= b.getYMax() ; y++) {
                for (int z = b.getZMin(); z <= b.getZMax() ; z++) {
                    narr[((x+centerX)*(ySize) + (y+centerY)) * (zSize) + (z+centerZ)] =
                            this.get(x, y, z) + goal.get(x, y, z);
                }
            }
        }
        return new BankedArrayGoal(bank, centerX, centerY, centerZ, narr, xSize, ySize, zSize);
    }

    @Override
    public BankedArrayGoal subtracted(BankedArrayGoal goal) {
        if(goal.total == 0){
            return new BankedArrayGoal(this);
        }
        Bounds b = new Bounds.SimpleBounds(Arrays.asList(this.bounds(), goal.bounds()));
        int centerX = -b.getXMin();
        int centerY = -b.getYMin();
        int centerZ = -b.getZMin();
        int xSize = b.getXMax()-b.getXMin()+1;
        int ySize = b.getYMax()-b.getYMin()+1;
        int zSize = b.getZMax()-b.getZMin()+1;
        int[] narr = new int[xSize * ySize * zSize];
        for (int x = b.getXMin(); x <= b.getXMax() ; x++) {
            for (int y = b.getYMin(); y <= b.getYMax() ; y++) {
                for (int z = b.getZMin(); z <= b.getZMax() ; z++) {
                    narr[((x+centerX)*(ySize) + (y+centerY)) * (zSize) + (z+centerZ)] =
                            this.get(x, y, z) - goal.get(x, y, z);
                }
            }
        }
        return new BankedArrayGoal(bank, centerX, centerY, centerZ, narr, xSize, ySize, zSize);
    }


    @Override
    public List<BankedArrayGoal> allSplits(){
        List<int[]> lists = new ArrayList<>();
        try {
            lists.add(new int[this.arr.length]);
            for (int i = 0; i < this.arr.length; i++) {
                int c = arr[i];
                if(c != 0) {
                    int inc = Integer.signum(c);
                    int listsLength = lists.size();
                    for (int k = inc; k != c+inc; k += inc) {
                        for (int j = 0; j < listsLength; j++) {
                            int[] ints = lists.get(j);
                            int[] narr = Arrays.copyOf(ints, ints.length);
                            narr[i] = k;
                            lists.add(narr);
                        }
                    }
                }
            }
            ArrayList<BankedArrayGoal> out = new ArrayList<>();
            for (int i = 0; i < lists.size(); i++) {
                out.add(new BankedArrayGoal(bank, centerX, centerY, centerZ, lists.get(i), xSize, ySize, zSize));
            }
            return out;
        } catch (OutOfMemoryError memoryError){
            System.out.println("OUT OF MEM size: " + lists.size() + " * " + this.arr.length);
            System.exit(-1);
            return null;
        }
    }


    @Override
    public Iterator<Tuple<Coord, Integer>> uniqueCountIterator() {
        return new Iterator<Tuple<Coord, Integer>>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                while ( this.i < arr.length && arr[i] == 0) {
                    this.i += 1;
                }
                return this.i != arr.length;
            }

            @Override
            public Tuple<Coord, Integer> next() {
                if (!hasNext()){return null;}
                int z = (i % zSize) - centerZ;
                int y = ((i / zSize) % ySize) - centerY;
                int x = (i /(ySize*zSize)) - centerX;
                Tuple<Coord, Integer> tuple = new Tuple<>(new Coord(x, y, z), arr[i]);
                i += 1;
                return tuple;
            }
        };
    }

    public Iterator<Tuple<Atom, Integer>> uniqueAtomCountIterator() {
        return new Iterator<Tuple<Atom, Integer>>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                while ( this.i < arr.length && arr[i] == 0) {
                    this.i += 1;
                }
                return this.i != arr.length;
            }

            @Override
            public Tuple<Atom, Integer> next() {
                if (!hasNext()){return null;}
                int z = (i % zSize) - centerZ;
                int y = ((i / zSize) % ySize) - centerY;
                int x = (i /(ySize*zSize)) - centerX;
                Tuple<Atom, Integer> tuple = new Tuple<>(new Atom(x, y, z, arr[i] >= 0), Math.abs(arr[i]));
                i += 1;
                return tuple;
            }
        };
    }

    @Override
    public AveragePosition getAveragePos() {
        double ax=0, ay=0, az=0;
        for (int x = -this.centerX; x < this.xSize-this.centerX; x++) {
            for (int y = -this.centerY; y < this.ySize-this.centerY; y++) {
                for (int z = -this.centerZ; z < this.zSize-this.centerZ; z++) {
                    int xPos = x + centerX;
                    int yPos = y + centerY;
                    int zPos = z + centerZ;
                    int idx = (xPos*(ySize) + yPos)*(zSize) + zPos;
                    int thisC = Math.abs(arr[idx]);
                    ax += thisC*x;
                    ay += thisC*y;
                    az += thisC*z;
                }
            }
        }
        return new AveragePosition(ax/total, ay/total, az/total);
    }


    @Override
    public int getBank() {
        return bank;
    }

    @Override
    public BankedArrayGoal inBank(int bank) {
        return new BankedArrayGoal(bank, this);
    }

    @Override
    public boolean isInBank(int bank) {
        return this.bank==bank;
    }

    @Override
    public Factory newFactory() {
        return new Factory(bank);
    }

    public static class Factory implements BankedKernel3DGoalFactory<BankedArrayGoal> {
        private int bank;
        final HashMap<Coord, Integer> map = new HashMap<>();

        public Factory(int bank) {
            this.bank = bank;
        }

        @Override
        public BankedArrayGoal get() {
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            int total = 0;
            for (Map.Entry<Coord, Integer> coordIntegerEntry : map.entrySet()) {
                int v = coordIntegerEntry.getValue();
                if (v != 0){
                    total += Math.abs(v);
                    minX = Math.min(minX, coordIntegerEntry.getKey().x);
                    maxX = Math.max(maxX, coordIntegerEntry.getKey().x);
                    minY = Math.min(minY, coordIntegerEntry.getKey().y);
                    maxY = Math.max(maxY, coordIntegerEntry.getKey().y);
                    minZ = Math.min(minZ, coordIntegerEntry.getKey().z);
                    maxZ = Math.max(maxZ, coordIntegerEntry.getKey().z);
                }
            }
            if(total==0){return new BankedArrayGoal(bank);}
            int centerX = -minX;
            int centerY = -minY;
            int centerZ = -minZ;
            int xSize = maxX - minX + 1;
            int ySize = maxY - minY + 1;
            int zSize = maxZ - minZ + 1;

            int[] narray = new int[xSize * ySize * zSize];
            for (Map.Entry<Coord, Integer> next : map.entrySet()) {
                int v = next.getValue();
                if (v != 0){
                    narray[((next.getKey().x+centerX)*(ySize) + (next.getKey().y+centerY)) * (zSize) + (next.getKey().z+centerZ)] = v;
                }
            }
            return new BankedArrayGoal(bank, narray, centerX, centerY, centerZ, xSize ,ySize, zSize, total);
        }

        @Override
        public Factory setBank(int bank) {
            this.bank = bank;
            return this;
        }

        @Override
        public Factory add(int x, int y, int z, int v) {
            map.compute(new Coord(x, y, z), (coord, c) -> (c==null?0:c)+v);
            return this;
        }

        @Override
        public Factory sub(int x, int y, int z, int v) {
            map.compute(new Coord(x, y, z), (coord, c) -> (c==null?0:c)-v);
            return this;
        }

        @Override
        public Factory add(BankedArrayGoal goal) {
            for (int i = 0; i < goal.arr.length; i++) {
                int v = goal.arr[i];
                if (v != 0){
                    int z = i % goal.zSize;
                    int y = (i / goal.zSize) % goal.ySize;
                    int x = i /(goal.ySize*goal.zSize);
                    map.compute(new Factory.Coord(x, y, z), (coord, c) -> (c==null?0:c)+v);
                }
            }
            return this;
        }

        @Override
        public Factory sub(BankedArrayGoal goal) {
            for (int i = 0; i < goal.arr.length; i++) {
                int v = goal.arr[i];
                if (v != 0){
                    int z = i % goal.zSize;
                    int y = (i / goal.zSize) % goal.ySize;
                    int x = i /(goal.ySize*goal.zSize);
                    map.compute(new Factory.Coord(x, y, z), (coord, c) -> (c==null?0:c)-v);
                }
            }
            return this;
        }

        @Override
        public Factory addAll(Collection<BankedArrayGoal> goals) {
            for (BankedArrayGoal goal : goals) {
                add(goal);
            }
            return this;
        }


        private static class Coord{
            final int x; final int y; final int z;

            private Coord(int x, int y, int z) {
                this.x = x;
                this.y = y;
                this.z = z;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Coord coord = (Coord) o;
                return x == coord.x && y == coord.y && z == coord.z;
            }

            @Override
            public int hashCode() {
                return Objects.hash(x, y, z);
            }
        }
    }
}
