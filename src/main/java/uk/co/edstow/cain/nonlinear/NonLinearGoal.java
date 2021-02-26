package uk.co.edstow.cain.nonlinear;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.structures.Bounds;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalPair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class NonLinearGoal<G extends Goal<G>, T extends Transformation<R>, R extends Register> implements Goal<NonLinearGoal<G,T,R>> {


    public abstract G identity();
    public abstract List<G> base();
    protected abstract int rank();

    public abstract Collection<GoalPair<NonLinearGoal<G,T,R>,T, R>> getReductions();

    public static class WrapperNonLinearGoal<G extends Goal<G>, T extends Transformation<R>, R extends Register> extends NonLinearGoal<G,T,R> {
        final G goal;

        public WrapperNonLinearGoal(G goal) {
            this.goal = goal;
        }

        @Override
        public G identity() {
            return goal;
        }

        @Override
        public List<G> base() {
            return Collections.singletonList(goal);
        }

        @Override
        public boolean same(NonLinearGoal<G,T,R> goal) {
            return goal.identity() != null && this.goal.same(goal.identity());
        }

        @Override
        public boolean equivalent(NonLinearGoal<G,T,R> goal) {
            return goal instanceof WrapperNonLinearGoal && ((WrapperNonLinearGoal<G,T,R>) goal).goal == this.goal;
        }

        @Override
        public double total() {
            return this.goal.total();
        }

        @Override
        public String[][] getCharTable(Bounds b, boolean top, boolean bottom, boolean centreDot, boolean colourNeg) {
            return this.goal.getCharTable(b, top, bottom, centreDot, colourNeg);
        }

        @Override
        public Bounds bounds() {
            return this.goal.bounds();
        }

        @Override
        protected int rank() {
            return 0;
        }

        @Override
        public Collection<GoalPair<NonLinearGoal<G,T,R>, T, R>> getReductions() {
            // Identity function cannot be reduced or rewritten usefully.
            return Collections.emptyList();
        }

        @Override
        public int compareTo(NonLinearGoal<G,T,R> goal) {
            int c = Integer.compare(rank(), goal.rank());
            if ( c!= 0){
                return c;
            }
            assert goal instanceof WrapperNonLinearGoal;
            return ((WrapperNonLinearGoal<G,T,R>) goal).goal.compareTo(this.goal);
        }
    }

    public static class UnaryNonLinearGoal<G extends Goal<G>, T extends Transformation<R>, R extends Register> extends NonLinearGoal<G,T,R> {

        final NonLinearGoal<G,T,R> goal;
        final UnaryNonLinearFunction func;

        public UnaryNonLinearGoal(NonLinearGoal<G,T,R> goal, UnaryNonLinearFunction func) {
            this.goal = goal;
            this.func = func;
        }

        @Override
        public boolean same(NonLinearGoal<G,T,R> goal) {
            if(this == goal) return true;
            if(goal instanceof UnaryNonLinearGoal){
                UnaryNonLinearGoal<G,T,R> other = (UnaryNonLinearGoal<G,T,R>) goal;
                return other.func == func && this.goal.same(other.goal);
            }
            return false;
        }

        @Override
        public boolean equivalent(NonLinearGoal<G,T,R> goal) {
            if(this == goal) return true;
            if(goal instanceof UnaryNonLinearGoal){
                UnaryNonLinearGoal<G,T,R> other = (UnaryNonLinearGoal<G,T,R>) goal;
                return other.func == func && this.goal.equivalent(other.goal);
            }
            return false;
        }

        @Override
        public double total() {
            return goal.total();
        }

        @Override
        public String[][] getCharTable(Bounds b, boolean top, boolean bottom, boolean centreDot, boolean colourNeg) {
            return goal.getCharTable(b, top, bottom, centreDot, colourNeg);
        }

        @Override
        public Bounds bounds() {
            return goal.bounds();
        }

        @Override
        public int compareTo(NonLinearGoal<G,T,R> goal) {

            int c = Integer.compare(rank(), goal.rank());
            if ( c!= 0){
                return c;
            }
            assert goal instanceof UnaryNonLinearGoal;
            UnaryNonLinearGoal<G,T,R> other = (UnaryNonLinearGoal<G,T,R>) goal;
            int f = this.func.compareTo(other.func);
            if (f!=0) return f;
            return this.goal.compareTo(other.goal);
        }

        @Override
        public G identity() {
            return null;
        }

        @Override
        public List<G> base() {
            return goal.base();
        }

        @Override
        protected int rank() {
            return 1;
        }

        @Override
        public Collection<GoalPair<NonLinearGoal<G,T,R>, T, R>> getReductions() {
            return Collections.singletonList(new GoalPair<>(this, this.goal, null
//                    new Transformation() {//TODO fix
//                @Override
//                public String code(List<LinearScanRegisterAllocator.Register> uppers, List<LinearScanRegisterAllocator.Register> lowers, List<LinearScanRegisterAllocator.Register> trash) {
//                    return String.format("// %s <- %s :: %s", uppers.get(0), lowers.get(0), func.toString());
//                }
//
//                @Override
//                public int inputCount() {
//                    return 1;
//                }
//
//                @Override
//                public int outputCount() {
//                    return 2;
//                }
//
//                @Override
//                public boolean[] inputRegisterOutputInterference(int u) {
//                    return new boolean[0];
//                }
//
//                @Override
//                public int[] inputRegisterIntraInterference() {
//                    return new int[0];
//                }
//
//                @Override
//                public boolean clobbersInput(int i) {
//                    return false;
//                }
//
//                @Override
//                public double cost() {
//                    return 2;
//                }
//
//                @Override
//                public String toStringN() {
//                    return func.toString() + " " + goal.toString();
//                }
//            }
            ));
        }
    }
    public enum UnaryNonLinearFunction implements Comparable<UnaryNonLinearFunction> {
        Absolute, Relu, Sin, Cos

    }

    public static <G extends Goal<G>, T extends Transformation<R>, R extends Register> List<NonLinearGoal<G,T,R>> idOfGoals(List<G> input){
        List<NonLinearGoal<G,T,R>> list = new ArrayList<>();
        for (G g : input) {
            NonLinearGoal<G,T,R> idOfGoal = idOfGoal(g);
            list.add(idOfGoal);
        }
        return list;
    }

    public static <G extends Goal<G>, T extends Transformation<R>, R extends Register> NonLinearGoal<G,T,R> idOfGoal(G goal){
        return new WrapperNonLinearGoal<>(goal);
    }



}
