package uk.co.edstow.cain.nonlinear;

import uk.co.edstow.cain.structures.Bounds;
import uk.co.edstow.cain.structures.Goal;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

//public abstract class NonLinearGoal<G extends Goal<G>> implements Goal<NonLinearGoal<G>> {
public abstract class NonLinearGoal<G extends Goal<G>> implements Goal<NonLinearGoal<G>> {


    public abstract G identity();
    protected abstract int rank();

    public static class WrapperNonLinearGoal<G extends Goal<G>> extends NonLinearGoal<G> {
        final G goal;

        public WrapperNonLinearGoal(G goal) {
            this.goal = goal;
        }

        @Override
        public G identity() {
            return goal;
        }

        @Override
        public boolean same(NonLinearGoal<G> goal) {
            return goal.identity() != null && this.goal.same(goal.identity());
        }

        @Override
        public boolean equivalent(NonLinearGoal<G> goal) {
            return goal instanceof WrapperNonLinearGoal && ((WrapperNonLinearGoal<G>) goal).goal == this.goal;
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
        public int compareTo(NonLinearGoal<G> goal) {
            int c = Integer.compare(rank(), goal.rank());
            if ( c!= 0){
                return c;
            }
            assert goal instanceof WrapperNonLinearGoal;
            return ((WrapperNonLinearGoal<G>) goal).goal.compareTo(this.goal);
        }
    }

    public static class UnaryNonLinearGoal<G extends Goal<G>> extends NonLinearGoal<G> {

        final NonLinearGoal<G> goal;
        final UnaryNonLinearFunction func;

        public UnaryNonLinearGoal(NonLinearGoal<G> goal, UnaryNonLinearFunction func) {
            this.goal = goal;
            this.func = func;
        }

        @Override
        public boolean same(NonLinearGoal<G> goal) {
            if(this == goal) return true;
            if(goal instanceof UnaryNonLinearGoal){
                UnaryNonLinearGoal<G> other = (UnaryNonLinearGoal<G>) goal;
                return other.func == func && this.goal.same(other.goal);
            }
            return false;
        }

        @Override
        public boolean equivalent(NonLinearGoal<G> goal) {
            if(this == goal) return true;
            if(goal instanceof UnaryNonLinearGoal){
                UnaryNonLinearGoal<G> other = (UnaryNonLinearGoal<G>) goal;
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
        public int compareTo(NonLinearGoal<G> goal) {

            int c = Integer.compare(rank(), goal.rank());
            if ( c!= 0){
                return c;
            }
            assert goal instanceof UnaryNonLinearGoal;
            UnaryNonLinearGoal<G> other = (UnaryNonLinearGoal<G>) goal;
            int f = this.func.compareTo(other.func);
            if (f!=0) return f;
            return this.goal.compareTo(other.goal);
        }

        @Override
        public G identity() {
            return null;
        }

        @Override
        protected int rank() {
            return 1;
        }
    }
    public enum UnaryNonLinearFunction implements Comparable<UnaryNonLinearFunction> {
        Absolute, Relu, Sin, Cos

    }

    public static <G extends Goal<G>> List<NonLinearGoal<G>> idOfGoals(List<G> input){
        return input.stream().map(NonLinearGoal::idOfGoal).collect(Collectors.toList());
    }

    public static <G extends Goal<G>> NonLinearGoal<G> idOfGoal(G goal){
        return new WrapperNonLinearGoal<>(goal);
    }

}
