package uk.co.edstow.cain.goals;

import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.util.Tuple;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public interface Kernel3DGoal<G extends Kernel3DGoal<G>> extends Goal<G> {

    boolean allZero();
    int get(int x, int y, int z);
    int totalI();
    boolean hasSubGoal(G a);
    boolean allSame();
    int minimumCount();
    int maximumCount();

    /**
     * @param pattern A goal to compare
     * @return true iff there exists a translation that would make pattern equal to this.
     */
    boolean isTranslation(G pattern);

    G copy();
    G without(G goal);
    G added(G goal);
    G subtracted(G goal);
    G negated();
    G translated(int x, int y, int z);

    List<G> allSplits();

    Iterator<Tuple<Coord, Integer>> uniqueCountIterator();

    default AveragePosition getAveragePos(){
        double x=0, y=0, z=0;
        Iterator<Tuple<Coord, Integer>> tupleIterator = uniqueCountIterator();
        while (tupleIterator.hasNext()){
         Tuple<Coord, Integer> t = tupleIterator.next();
            x += t.getA().x*t.getB();
            y += t.getA().y*t.getB();
            z += t.getA().z*t.getB();
        }
        return new AveragePosition(x/total(), y/total(), z/total());
    }




    class AveragePosition {
        public final double x, y, z;

        @SuppressWarnings("WeakerAccess")
        public AveragePosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }


    Kernel3DGoalFactory<G> newFactory();

    interface Kernel3DGoalFactory<G>{
        G get();
        Kernel3DGoalFactory<G> add(int x, int y, int z, int v);
        Kernel3DGoalFactory<G> sub(int x, int y, int z, int v);
        Kernel3DGoalFactory<G> add(G goal);
        Kernel3DGoalFactory<G> sub(G goal);
        Kernel3DGoalFactory<G> addAll(Collection<G> goals);

        default Kernel3DGoalFactory<G> add(Coord coord, int v){
            return this.add(coord.x, coord.y, coord.z, v);
        }
        default Kernel3DGoalFactory<G> sub(Coord coord, int v){
            return this.sub(coord.x, coord.y, coord.z, v);
        }

    }


    class Coord {
        public final int x;
        public final int y;
        public final int z;

        public Coord(int x, int y, int z) {
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

        @Override
        public String toString() {
            return "(" + x +
                    "," + y +
                    "," + z +
                    ')';
        }
    }
}
