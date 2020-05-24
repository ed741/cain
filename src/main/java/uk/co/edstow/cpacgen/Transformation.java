package uk.co.edstow.cpacgen;

import uk.co.edstow.cpacgen.util.Tuple;

import java.util.*;

public abstract class Transformation {

    public abstract String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers);

    public static class TransformationApplicationException extends Exception{

        public TransformationApplicationException(String s) {
            super(s);
        }

    }
    @SuppressWarnings("WeakerAccess")
    public abstract int inputCount();

    public boolean[] inputRegisterOutputInterference(){
        boolean[] out =  new boolean[inputCount()];
        for (int i = 0; i < out.length; i++) {
            out[i] = true;
        }
        return out;
    }
    public boolean inputRegisterOutputInterferes(){
        for (boolean b : inputRegisterOutputInterference()) {
            if(b){return true;}
        }
        return false;
    }
    public int[] inputRegisterIntraInterference(){

        int[] out = new int[inputCount()];
        for (int i = 0; i < out.length; i++) {
            out[i]=i;
        }
        return out;
    }
    public abstract Goal applyForwards() throws TransformationApplicationException;
    //public abstract List<Goal> applyBackwards()throws TransformationApplicationException;
    public abstract double cost();
    public abstract String toStringN();

    @Override
    public String toString() {
        return "MiscTransformation";
    }

    public static class Null extends Transformation {
        private final int inputCount;

        public Null(int inputCount) {
            this.inputCount = inputCount;
        }

        @Override
        public boolean[] inputRegisterOutputInterference(){
            return new boolean[inputCount()];
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            return String.format("//Null Instruction: %s <- %s", upper, lowers);
        }

        @Override
        public int inputCount() {
            return inputCount;
        }

        @Override
        public Goal applyForwards(){
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
        private final int divisions;
        private final Goal in;

        private static Goal getForwardsApplication(int divisions, Goal goal) throws TransformationApplicationException {
            Goal.Factory factory = new Goal.Factory();
            Map<Atom, Integer> count = new HashMap<>();
            for (Atom a: goal){
                Integer i = count.getOrDefault(a, 0);
                count.put(a, i);
            }
            for (Map.Entry<Atom, Integer> entry : count.entrySet()) {
                int i = entry.getValue();
                boolean isValid = i > 0 && ((i >> divisions) << divisions) == i;
                if (!isValid){
                    throw new TransformationApplicationException(i + " Atoms cannot halved " + divisions + " time(s)");
                }
                for (int j = 0; j < i>>divisions; j++){
                    factory.add(entry.getKey());
                }
            }
            return factory.get();
        }

        private static Goal getBackwardsApplication(int divisions, Goal goal){
            Goal.Factory factory = new Goal.Factory();
            for (Atom a: goal){
                for (int j = 0; j < 1<<divisions; j++){
                    factory.add(a);
                }
            }
            return factory.get();
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
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("div %d (%s, %s)", divisions, upper, lowers.get(0));
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
        private final int steps;
        private final Direction dir;
        private final Goal in;

        private static Goal getForwardsApplication(int steps, Direction dir, Goal goal){
            Goal.Factory factory = new Goal.Factory();
            int rx = steps * dir.x;
            int ry = steps * dir.y;
            for (Atom a: goal){
                factory.add(a.moved(rx, ry, 0));
            }
            return factory.get();
        }

        private static Goal getBackwardsApplication(int steps, Direction dir, Goal goal){
            Goal.Factory factory = new Goal.Factory();
            int rx = steps * dir.x;
            int ry = steps * dir.y;
            for (Atom a: goal){
                factory.add(a.moved(-rx, -ry, 0));
            }
            return factory.get();
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
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("move(%s, %s, %d, %s)", upper, lowers.get(0), steps, dir);
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public Goal applyForwards() {
            return getForwardsApplication(steps, dir, in);

        }

        @Override
        public double cost() {
            return steps;
        }
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
                default: return null;
            }
        }
    }


    public static class Add extends Transformation{
        private final Goal a;
        private final Goal b;

        private static Goal getForwardsApplication(Goal a, Goal b){
            Goal.Factory factory = new Goal.Factory(a);
            factory.addAll(b);
            return factory.get();
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
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            StringBuilder sb = new StringBuilder();
            sb.append("add(");
            sb.append(upper);
            for (RegisterAllocator.Register lower : lowers) {
                sb.append(", ");
                sb.append(lower);
            }
            sb.append(")");
            return sb.toString();
        }

        @Override
        public int inputCount() {
            return 2;
        }

        @Override
        public Goal applyForwards() {
            return getForwardsApplication(a, b);
        }

        @Override
        public double cost() {
            return 1;
        }
    }
}
