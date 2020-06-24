package uk.co.edstow.cain.structures;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.IntStream;

public class Atom implements Comparable<Atom> {
    public final int x;
    public final int y;
    public final int z;
    public final boolean positive;

    public Atom(int x, int y, int z, boolean positive) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.positive = positive;
    }

    public Atom moved(int x, int y, int z) {
        return new Atom(this.x+x, this.y+y, this.z+z, positive);
    }
    public Atom negate(){
        return new Atom(x,y,z,!positive);
    }
    public boolean samePos(Atom b){
        return x==b.x && y==b.y && z==b.z;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(x) ^ Integer.hashCode(y) ^ Integer.hashCode(z);
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof Atom && ((Atom) o).x == x && ((Atom) o).y == y && ((Atom) o).z == z && ((Atom) o).positive == positive;
    }

    @Override
    public String toString() {
        return "Atom:"+(positive?"+":"-")+"(" + x + ", " + y  + ", " + z + ")";
    }

    public String toStringN() {
        return (positive?"+":"-")+"(" + x + ", " + y  + ", " + z + ")";
    }

    @Override
    public int compareTo(Atom b) {
        if(x < b.x){
            return -1;
        }
        if(x > b.x){
            return 1;
        }
        if(y < b.y){
            return -1;
        }
        if(y > b.y){
            return 1;
        }
        if(z < b.z){
            return -1;
        }
        if(z > b.z){
            return 1;
        }
        if(positive && !b.positive){
            return 1;
        }
        if(!positive && b.positive){
            return -1;
        }
        return 0;
    }

    public static class AtomComparator implements Comparator<Atom> {
        @Override
        public int compare(Atom a, Atom b) {
           return a.compareTo(b);

        }
    }
    public static AtomComparator comparator = new AtomComparator();

    @SuppressWarnings("WeakerAccess")
    public static class Bounds {
        public final int xMax;
        public final int xMin;
        public final int yMax;
        public final int yMin;
        public final int zMax;
        public final int zMin;

        public Bounds(int xMax, int xMin, int yMax, int yMin, int zMax, int zMin) {
            this.xMax = xMax;
            this.xMin = xMin;
            this.yMax = yMax;
            this.yMin = yMin;
            this.zMax = zMax;
            this.zMin = zMin;
        }

        public Bounds(Collection<? extends Collection<Atom>> cs) {
            int xMax = Integer.MIN_VALUE;
            int xMin = Integer.MAX_VALUE;
            int yMax = Integer.MIN_VALUE;
            int yMin = Integer.MAX_VALUE;
            int zMax = Integer.MIN_VALUE;
            int zMin = Integer.MAX_VALUE;

            boolean found = false;
            for (Collection<? extends Atom> c: cs){
                for (Atom a: c){
                    found = true;
                    xMax = max(xMax, a.x);
                    xMin = min(xMin, a.x);
                    yMax = max(yMax, a.y);
                    yMin = min(yMin, a.y);
                    zMax = max(zMax, a.z);
                    zMin = min(zMin, a.z);
                }
            }
            if(found) {
                this.xMax = xMax;
                this.xMin = xMin;
                this.yMax = yMax;
                this.yMin = yMin;
                this.zMax = zMax;
                this.zMin = zMin;
            } else {
                this.xMax = 0;
                this.xMin = 0;
                this.yMax = 0;
                this.yMin = 0;
                this.zMax = 0;
                this.zMin = 0;
            }
        }

        public static Bounds BoundsFromGoal(Collection<Atom> c) {
            int xMax = Integer.MIN_VALUE;
            int xMin = Integer.MAX_VALUE;
            int yMax = Integer.MIN_VALUE;
            int yMin = Integer.MAX_VALUE;
            int zMax = Integer.MIN_VALUE;
            int zMin = Integer.MAX_VALUE;

            for (Atom a: c){
                xMax = max(xMax, a.x);
                xMin = min(xMin, a.x);
                yMax = max(yMax, a.y);
                yMin = min(yMin, a.y);
                zMax = max(zMax, a.z);
                zMin = min(zMin, a.z);
            }

            return new Bounds(xMax, xMin, yMax, yMin, zMax, zMin);
        }


        public Bounds(Bounds b, Atom a){
                xMax = max(b.xMax, a.x);
                xMin = min(b.xMin, a.x);
                yMax = max(b.yMax, a.y);
                yMin = min(b.yMin, a.y);
                zMax = max(b.zMax, a.z);
                zMin = min(b.zMin, a.z);
        }

        public boolean includes(Atom a){
            return xMin <= a.x && a.x <= xMax &&
                    yMin <= a.y && a.y <= yMax &&
                    zMin <= a.z && a.z <= zMax;
        }

        public boolean excludes(Atom a){
            return xMin > a.x || a.x > xMax ||
                    yMin > a.y || a.y > yMax ||
                    zMin > a.z || a.z > zMax;
        }

        public int largestMagnitude(){
            return IntStream.of(xMin, xMax, yMin, yMax, zMin, zMax).map(Math::abs).max().getAsInt();
        }

        private static int max(int a, int b){
            return a>b ? a : b;
        }

        private static int min(int a, int b){
            return a<b ? a : b;
        }


        @Override
        public String toString() {
            return "Bounds{" +
                    "xMax=" + xMax +
                    ", xMin=" + xMin +
                    ", yMax=" + yMax +
                    ", yMin=" + yMin +
                    ", zMax=" + zMax +
                    ", zMin=" + zMin +
                    '}';
        }

        public static Bounds combine(Collection<Bounds> bounds) {
            int xMax = Integer.MIN_VALUE;
            int xMin = Integer.MAX_VALUE;
            int yMax = Integer.MIN_VALUE;
            int yMin = Integer.MAX_VALUE;
            int zMax = Integer.MIN_VALUE;
            int zMin = Integer.MAX_VALUE;

            boolean found = false;
            for (Bounds b: bounds){
                found = true;
                xMax = max(xMax, b.xMax);
                xMin = min(xMin, b.xMin);
                yMax = max(yMax, b.yMax);
                yMin = min(yMin, b.yMin);
                zMax = max(zMax, b.zMax);
                zMin = min(zMin, b.zMin);
            }
            if(found) {
                return new Bounds(xMax, xMin, yMax, yMin, zMax, zMin);
            } else {
                return new Bounds(0,0,0,0,0,0);
            }
        }
    }
}
