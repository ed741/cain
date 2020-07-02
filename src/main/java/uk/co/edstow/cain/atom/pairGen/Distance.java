package uk.co.edstow.cain.atom.pairGen;

import uk.co.edstow.cain.atom.Atom;
import uk.co.edstow.cain.atom.AtomGoal;

@SuppressWarnings("WeakerAccess")
public class Distance {
    public final int x, y, z;

    public Distance(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Distance(Atom a, Atom b){
        this.x = b.x-a.x;
        this.y = b.y-a.y;
        this.z = b.z-a.z;
    }

    public Distance(AtomGoal.AveragePosition position){
        this.x = (int) Math.round(position.x);
        this.y = (int) Math.round(position.y);
        this.z = (int) Math.round(position.z);
    }

    public Distance(SimpleTransformation.Direction dir, int length){
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

    public AtomGoal translate(AtomGoal goal){
        AtomGoal.Factory factory = new AtomGoal.Factory();
        for (Atom a: goal) {
            factory.add(a.moved(x, y, z));
        }
        return factory.get();
    }

    public SimpleTransformation.Direction majorDirection() {
        int absX = Math.abs(x);
        int absY = Math.abs(y);
        int absZ = Math.abs(z);

        if (isZero()){
            return null;
        }
        if(absX >= absY && absX >= absZ){
            return x>0? SimpleTransformation.Direction.E : SimpleTransformation.Direction.W;
        }
        if(absY >= absX && absY >= absZ){
            return y>0? SimpleTransformation.Direction.N : SimpleTransformation.Direction.S;
        }
        if(absZ >= absX && absZ >= absY){
            return z>0? SimpleTransformation.Direction.U : SimpleTransformation.Direction.D;
        }
        return null;
    }

    public SimpleTransformation.Direction majorXYDirection() {
        if (isZero()) return null;

        int absX = Math.abs(x);
        int absY = Math.abs(y);

        if(absX >= absY){
            return x>0? SimpleTransformation.Direction.E : SimpleTransformation.Direction.W;
        } else {
            return y>0? SimpleTransformation.Direction.N : SimpleTransformation.Direction.S;
        }
    }

    public Distance then(SimpleTransformation.Direction direction) {
        return new Distance(x-direction.x, y-direction.y, z);
    }

    public boolean same(Atom a, Atom b) {
        return this.x == b.x - a.x && this.y == b.y - a.y && this.z == b.z - a.z;
    }
}
