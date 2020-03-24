package cpacgen;

import cpacgen.util.Bounds;

public class Atom {
    public final int x;
    public final int y;
    public final int z;

    public Atom(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean at(int x, int y, int z) {
        return this.x == x && this.y == y && this.z == z;
    }

    public Atom moved(int x, int y, int z) {
        return new Atom(this.x+x, this.y+y, this.z+z);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(x) ^ Integer.hashCode(y) ^ Integer.hashCode(z);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Atom && ((Atom) o).x == x && ((Atom) o).y == y && ((Atom) o).z == z;
    }

    @Override
    public String toString() {
        return "Atom:(" + x + ", " + y  + ", " + z + ")";
    }

    public String toStringN() {
        return "(" + x + ", " + y  + ", " + z + ")";
    }
}
