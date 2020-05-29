package uk.co.edstow.cpacgen;

import java.util.*;

public abstract class Transformation {

    public abstract String code(List<RegisterAllocator.Register> uppers, List<RegisterAllocator.Register> lowers, List<RegisterAllocator.Register> trash);

    public static class TransformationApplicationException extends Exception{

        public TransformationApplicationException(String s) {
            super(s);
        }

    }
    @SuppressWarnings("WeakerAccess")
    public abstract int inputCount();
    public abstract int outputCount();

    public abstract boolean[] inputRegisterOutputInterference(int u);
    public int ExtraRegisterCount(){
        int count = 0;
        for (int u = 0; u < outputCount(); u++) {
            for (boolean b : inputRegisterOutputInterference(u)) {
                if(b){
                    count++;
                    break;
                }
            }
        }
        return count;
    }
    public abstract int[] inputRegisterIntraInterference();
    public abstract boolean clobbersInput(int i);
    //public abstract List<Goal> applyBackwards()throws TransformationApplicationException;
    public abstract double cost();
    public abstract String toStringN();

    @Override
    public String toString() {
        return "MiscTransformation";
    }

    public static class Null extends Transformation {
        private final int inputCount;
        private final int outputCount;

        public Null(int inputCount, int outputCount) {
            this.inputCount = inputCount;
            this.outputCount = outputCount;
        }

        @Override
        public boolean[] inputRegisterOutputInterference(int u){
            return new boolean[inputCount()];
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            int[] out = new int[inputCount()];
            for (int i = 0; i < out.length; i++) {
                out[i]=i;
            }
            return out;
        }

        @Override
        public boolean clobbersInput(int i) {
            return false;
        }


        @Override
        public String code(List<RegisterAllocator.Register> uppers, List<RegisterAllocator.Register> lowers, List<RegisterAllocator.Register> trash) {
            return String.format("//Null Instruction: %s <- %s", uppers, lowers);
        }

        @Override
        public int inputCount() {
            return inputCount;
        }

        @Override
        public int outputCount() {
            return outputCount;
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


}
