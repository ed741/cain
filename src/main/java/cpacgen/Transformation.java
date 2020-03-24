package cpacgen;

import cpacgen.util.Tuple;
import sun.java2d.xr.MutableInteger;

import java.util.*;

public abstract class Transformation {

    public static class TransformationApplicationException extends Exception{

        public TransformationApplicationException(String s) {
            super(s);
        }
    };

    public abstract int inputCount();
    public abstract Goal applyForwards() throws TransformationApplicationException;
    //public abstract List<Goal> applyBackwards()throws TransformationApplicationException;
    public abstract double cost();
    public abstract String toStringN();

    @Override
    public String toString() {
        return "MiscTransformation";
    }

    public static class Null extends Transformation {

        @Override
        public int inputCount() {
            return 0;
        }

        @Override
        public Goal applyForwards() throws TransformationApplicationException {
            return null;
        }

        @Override
        public double cost() {
            return 0;
        }

        @Override
        public String toStringN() {
            return "Null_t";
        }

        @Override
        public String toString() {
            return "Null_t";
        }
    }

        public static Collection<Tuple<? extends Transformation, Goal>> applyAllUnaryOpForwards(Goal goal){
        ArrayList<Tuple<? extends Transformation, Goal>> list = new ArrayList<>();
        for (Transformation.Direction d: Transformation.Direction.values()){
            for (int i = 0; i < 4; i++){
                Transformation t = new Move(i, d, goal);
                try {
                    Goal go = t.applyForwards();
                    list.add(new Tuple<>(t, go));
                } catch (TransformationApplicationException ignored) {}
            }
        }
        for (int i = 0; i < 8; i++){
            Transformation t = new Div(i, goal);
            try {
                Goal go = t.applyForwards();
                list.add(new Tuple<>(t, go));
            } catch (TransformationApplicationException ignored) {}
        }
        return list;
    }

    public static Collection<Tuple<? extends Transformation, Goal>> applyAllUnaryOpBackwards(Goal goal){
        ArrayList<Tuple<? extends Transformation, Goal>> list = new ArrayList<>();
        for (Transformation.Direction d: Transformation.Direction.values()){
            for (int i = 1; i < 2; i++){
                Goal input = Move.getBackwardsApplication(i, d, goal);
                Transformation t = new Move(i, d, input);
                list.add(new Tuple<>(t, input));
            }
        }
        for (int i = 1; i < 2; i++){
            Goal input = Div.getBackwardsApplication(i, goal);
            Transformation t = new Div(i, input);
            list.add(new Tuple<>(t, input));
        }
        return list;
    }


    public static class Div extends Transformation{
        final int divisions;
        final Goal in;

        private static Goal getForwardsApplication(int divisions, Goal goal) throws TransformationApplicationException {
            Goal out = new Goal();
            Map<Atom, MutableInteger> count = new HashMap<>();
            for (Atom a: goal){
                count.putIfAbsent(a, new MutableInteger(0));
                MutableInteger i = count.get(a);
                i.setValue(i.getValue()+1);
            }
            for (Map.Entry<Atom, MutableInteger> entry : count.entrySet()) {
                int i = entry.getValue().getValue();
                boolean isValid = i > 0 && ((i >> divisions) << divisions) == i;
                if (!isValid){
                    throw new TransformationApplicationException(i + " Atoms cannot halved " + divisions + " time(s)");
                }
                for (int j = 0; j < i>>divisions; j++){
                    out.add(entry.getKey());
                }
            }
            return out;
        }

        private static Goal getBackwardsApplication(int divisions, Goal goal){
            Goal out = new Goal();
            for (Atom a: goal){
                for (int j = 0; j < 1<<divisions; j++){
                    out.add(a);
                }
            }
            return out;
        }

        public Div(int divisions, Goal in) {
            assert divisions > 0;
            this.divisions = divisions;
            this.in = in;
        }

        @Override
        public String toString() {
            return "Div:"+divisions;
        }


        @Override
        public String toStringN() {
            return toString();
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public Goal applyForwards() throws TransformationApplicationException {
            return getForwardsApplication(divisions, in);

        }

        @Override
        public double cost() {
            return divisions;
        }
    }

    public static class Move extends Transformation{
        final int steps;
        final Direction dir;
        final Goal in;

        private static Goal getForwardsApplication(int steps, Direction dir, Goal goal){
            Goal out = new Goal();
            int rx = steps * dir.x;
            int ry = steps * dir.y;
            for (Atom a: goal){
                out.add(a.moved(rx, ry, 0));
            }
            return out;
        }

        private static Goal getBackwardsApplication(int steps, Direction dir, Goal goal){
            Goal out = new Goal();
            int rx = steps * dir.x;
            int ry = steps * dir.y;
            for (Atom a: goal){
                out.add(a.moved(-rx, -ry, 0));
            }
            return out;
        }

        public Move(int steps, Direction dir, Goal in) {
            this.steps = steps;
            this.dir = dir;
            this.in = in;
        }

        @Override
        public String toString() {
            return "Move:"+this.in+"by"+this.dir+""+steps;
        }

        @Override
        public String toStringN() {
            return "Move:"+this.in.toStringN()+"by"+this.dir+""+steps;
        }


        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public Goal applyForwards() throws TransformationApplicationException {
            return getForwardsApplication(steps, dir, in);

        }

        @Override
        public double cost() {
            return steps;
        }
    }

    public enum Direction {
        N(0,1), E(1,0), S(0,-1), W(-1,0);

        int x, y;
        Direction(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }


    public static class Add extends Transformation{
        Goal a;
        Goal b;

        private static Goal getForwardsApplication(Goal a, Goal b){
            Goal out =  new Goal(a);
            out.addAll(b);
            return out;
        }

        public Add(Goal a, Goal b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return "Add:"+a+"+"+b;
        }

        @Override
        public String toStringN() {
            return "Add:"+a.toStringN()+"+"+b.toStringN();
        }

        @Override
        public int inputCount() {
            return 2;
        }

        @Override
        public Goal applyForwards() throws TransformationApplicationException {
            return getForwardsApplication(a, b);
        }

        @Override
        public double cost() {
            return 1;
        }
    }
}
