package uk.co.edstow.cain.structures;

import java.util.Comparator;

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
}
