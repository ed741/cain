package uk.co.edstow.cain.structures;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public interface Bounds {
    int getXMax();
    int getXMin();
    int getYMax();
    int getYMin();
    int getZMax();
    int getZMin();

    Bounds includeCentre();

    default boolean includes(int x, int y, int z){
        return getXMin() <= x && x <= getXMax() &&
                getYMin() <= y && y <= getYMax() &&
                getZMin() <= z && z <= getZMax();
    }

    default boolean excludes(int x, int y, int z){
        return getXMin() > x || x > getXMax() ||
                getYMin() > y || y > getYMax() ||
                getZMin() > z || z > getZMax();
    }

    default int largestMagnitude(){
        int max = 0;
        if(getXMax()>max) max = getXMax();
        if(getYMax()>max) max = getYMax();
        if(getZMax()>max) max = getZMax();

        if(Math.abs(getXMin())>max) max = Math.abs(getXMin());
        if(Math.abs(getYMin())>max) max = Math.abs(getYMin());
        if(Math.abs(getZMin())>max) max = Math.abs(getZMin());
        return max;
    }

    class SimpleBounds implements Bounds {
        public final int xMax;
        public final int xMin;
        public final int yMax;
        public final int yMin;
        public final int zMax;
        public final int zMin;

        @Override
        public int getXMax() {
            return xMax;
        }
        @Override
        public int getXMin() {
            return xMin;
        }
        @Override
        public int getYMax() {
            return yMax;
        }
        @Override
        public int getYMin() {
            return yMin;
        }
        @Override
        public int getZMax() {
            return zMax;
        }
        @Override
        public int getZMin() {
            return zMin;
        }

        @Override
        public Bounds includeCentre() {
            return new SimpleBounds(Arrays.asList(this, new SimpleBounds(0,0,0,0,0,0)));
        }

        public SimpleBounds(int xMax, int xMin, int yMax, int yMin, int zMax, int zMin) {
            this.xMax = xMax;
            this.xMin = xMin;
            this.yMax = yMax;
            this.yMin = yMin;
            this.zMax = zMax;
            this.zMin = zMin;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimpleBounds that = (SimpleBounds) o;
            return getXMax() == that.getXMax() && getXMin() == that.getXMin() &&
                    getYMax() == that.getYMax() && getYMin() == that.getYMin() &&
                    getZMax() == that.getZMax() && getZMin() == that.getZMin();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getXMax(), getXMin(), getYMax(), getYMin(), getZMax(), getZMin());
        }

        public SimpleBounds(List<Bounds> boundsList) {
            if(boundsList.isEmpty()) throw new IllegalArgumentException();
            int xMax = Integer.MIN_VALUE;
            int xMin = Integer.MAX_VALUE;
            int yMax = Integer.MIN_VALUE;
            int yMin = Integer.MAX_VALUE;
            int zMax = Integer.MIN_VALUE;
            int zMin = Integer.MAX_VALUE;

            boolean found = false;
            for (Bounds b: boundsList){
                xMax = Math.max(xMax, b.getXMax());
                xMin = Math.min(xMin, b.getXMin());
                yMax = Math.max(yMax, b.getYMax());
                yMin = Math.min(yMin, b.getYMin());
                zMax = Math.max(zMax, b.getZMax());
                zMin = Math.min(zMin, b.getZMin());
            }
            this.xMax = xMax;
            this.xMin = xMin;
            this.yMax = yMax;
            this.yMin = yMin;
            this.zMax = zMax;
            this.zMin = zMin;
        }

        public SimpleBounds(Bounds... boundsList) {
            if(boundsList.length == 0) throw new IllegalArgumentException();
            int xMax = Integer.MIN_VALUE;
            int xMin = Integer.MAX_VALUE;
            int yMax = Integer.MIN_VALUE;
            int yMin = Integer.MAX_VALUE;
            int zMax = Integer.MIN_VALUE;
            int zMin = Integer.MAX_VALUE;

            for (Bounds b: boundsList){
                xMax = Math.max(xMax, b.getXMax());
                xMin = Math.min(xMin, b.getXMin());
                yMax = Math.max(yMax, b.getYMax());
                yMin = Math.min(yMin, b.getYMin());
                zMax = Math.max(zMax, b.getZMax());
                zMin = Math.min(zMin, b.getZMin());
            }
            this.xMax = xMax;
            this.xMin = xMin;
            this.yMax = yMax;
            this.yMin = yMin;
            this.zMax = zMax;
            this.zMin = zMin;
        }

        @Override
        public String toString() {
            return "SimpleBounds{" +
                    "xMax=" + xMax +
                    ", xMin=" + xMin +
                    ", yMax=" + yMax +
                    ", yMin=" + yMin +
                    ", zMax=" + zMax +
                    ", zMin=" + zMin +
                    '}';
        }
    }
}
