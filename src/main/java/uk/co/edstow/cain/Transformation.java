package uk.co.edstow.cain;

import java.util.*;

public abstract class Transformation {

    public abstract String code(List<RegisterAllocator.Register> uppers, List<RegisterAllocator.Register> lowers, List<RegisterAllocator.Register> trash);

    public static class TransformationApplicationException extends Exception{

        public TransformationApplicationException(String s) {
            super(s);
        }

    }

    /**
     * @return the number of inputs the transformation has (aka the size of Lowers)
     */
    public abstract int inputCount();

    /**
     * @return the number of outputs the transformation has (aka the size of Uppers)
     */
    public abstract int outputCount();


    /**
     * @param u An index into the Transformation's Uppers
     * @return A boolean array of length equal to inputCount() where values are True iff the register for that upper and
     * that lower cannot be the same.
     */
    public abstract boolean[] inputRegisterOutputInterference(int u);


    /**
     * @return the number of extra registers required to use this transformation
     * If we have a goalBag with k registers, n of which are inputs (Lowers) to this transformation, how many more than
     * n or k registers are needed to apply this function
     */
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

    /**
     * @return array of ints, size equal to inputCount() that describes if inputs can be from the same register or not.
     * inputs (lowers) that can be the same register have the value of previous index of the input it can share a register
     * with else it's own index.
     * Eg. for Add(A, B, C) were none of the inputs can be the same register this returns [0,1,2]
     *     for func(A,B,B,C,C,B) where the 6 inputs can be from the same registers as shown: [0,1,1,3,3,1]
     *
     */
    public abstract int[] inputRegisterIntraInterference();
    public abstract boolean clobbersInput(int i);
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
