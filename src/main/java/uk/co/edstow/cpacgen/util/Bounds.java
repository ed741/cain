package uk.co.edstow.cpacgen.util;


import uk.co.edstow.cpacgen.Atom;

import java.util.Collection;

public class Bounds {
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

        for (Collection<? extends Atom> c: cs){
            for (Atom a: c){
                xMax = max(xMax, a.x);
                xMin = min(xMin, a.x);
                yMax = max(yMax, a.y);
                yMin = min(yMin, a.y);
                zMax = max(zMax, a.z);
                zMin = min(zMin, a.z);
            }
        }

        this.xMax = xMax;
        this.xMin = xMin;
        this.yMax = yMax;
        this.yMin = yMin;
        this.zMax = zMax;
        this.zMin = zMin;
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
}
