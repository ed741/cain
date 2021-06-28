package uk.co.edstow.cain.pairgen;

import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.util.Tuple;

import java.util.Iterator;

@SuppressWarnings("WeakerAccess")
public class Distance {
    public final int x, y, z;

    public Distance(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Distance(Kernel3DGoal.Coord a, Kernel3DGoal.Coord b){
        this.x = b.x-a.x;
        this.y = b.y-a.y;
        this.z = b.z-a.z;
    }

    public Distance(Kernel3DGoal.AveragePosition position){
        this.x = (int) Math.round(position.x);
        this.y = (int) Math.round(position.y);
        this.z = (int) Math.round(position.z);
    }

    public Distance(Direction dir, int length){
        this.x = -dir.x * length;
        this.y = -dir.y * length;
        this.z = -dir.z * length;
    }

    public boolean isZero(){
        return x==0 && y==0 && z==0;
    }

    public int manhattan() {
        return Math.abs(x)+Math.abs(y)+Math.abs(z);
    }

    public int manhattanXY() {
        return Math.abs(x)+Math.abs(y);
    }

    @Override
    public String toString() {
        return "D:("+x+","+y+","+z+")";
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(x) ^ Integer.hashCode(y) ^ Integer.hashCode(z);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Distance && ((Distance) o).x == x && ((Distance) o).y == y && ((Distance) o).z == z;
    }

    public Distance inverse(){
        return new Distance(-x, -y, -z);
    }

    public <G extends Kernel3DGoal<G>> G translate(Kernel3DGoal<G> goal){
        Kernel3DGoal.Kernel3DGoalFactory<G> factory = goal.newFactory();
        for (Iterator<Tuple<Kernel3DGoal.Coord, Integer>> it = goal.uniqueCountIterator(); it.hasNext(); ) {
            Tuple<Kernel3DGoal.Coord, Integer> a = it.next();
            factory.add(a.getA().x+x, a.getA().y+y, a.getA().z+z, a.getB());
        }
        return factory.get();
    }

    public Direction majorDirection() {
        int absX = Math.abs(x);
        int absY = Math.abs(y);
        int absZ = Math.abs(z);

        if (isZero()){
            return null;
        }
        if(absX >= absY && absX >= absZ){
            return x>0? Direction.E : Direction.W;
        }
        if(absY >= absX && absY >= absZ){
            return y>0? Direction.N : Direction.S;
        }
        if(absZ >= absX && absZ >= absY){
            return z>0? Direction.U : Direction.D;
        }
        return null;
    }

    public Direction majorXYDirection() {
        if (isZero()) return null;

        int absX = Math.abs(x);
        int absY = Math.abs(y);

        if(absX >= absY){
            return x>0? Direction.E : Direction.W;
        } else {
            return y>0? Direction.N : Direction.S;
        }
    }

    public Distance then(Direction direction) {
        return new Distance(x-direction.x, y-direction.y, z);
    }

    public boolean same(Kernel3DGoal.Coord a, Kernel3DGoal.Coord b) {
        return this.x == b.x - a.x && this.y == b.y - a.y && this.z == b.z - a.z;
    }

    public enum Direction {
        N(0,-1,0), E(-1,0,0), S(0,1,0), W(1,0,0), U(1,0,-1), D(1,0,1);

        public final int x, y, z;
        Direction(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Direction opposite(){
            switch (this){
                case N: return S;
                case E: return W;
                case S: return N;
                case W: return E;
                case U: return D;
                case D: return U;
            }
            assert false;
            System.exit(-1);
            return null;
        }
    }
}
