package uk.co.edstow.cain.transformations;

import uk.co.edstow.cain.regAlloc.Register;

import java.util.*;

@SuppressWarnings("CommentedOutCode")
public interface Transformation<R extends Register> {

    String code(List<R> uppers, List<R> lowers, List<R> trash);

    class TransformationApplicationException extends Exception{

        public TransformationApplicationException(String s) {
            super(s);
        }

    }


    /**
     * @return array of ints, size equal to inputCount() that describes if inputs can be from the same register or not.
     * inputs (lowers) that can be the same register have the value of previous index of the input it can share a register
     * with else it's own index.
     * Eg. for Add(A, B, C) were none of the inputs can be the same register this returns [0,1,2]
     *     for func(A,B,B,C,C,B) where the 6 inputs can be from the same registers as shown: [0,1,1,3,3,1]
     *
     */
    int[] inputRegisterIntraInterference();
    boolean clobbersInput(int i);
    double cost();



    // ProtoType Null transformation to show what's required by PairGenFactory.getDummyTransformation(...)
//    class Null implements Transformation {
//        private final int inputCount;
//        private final int outputCount;
//
//        public Null(int inputCount, int outputCount) {
//            this.inputCount = inputCount;
//            this.outputCount = outputCount;
//        }
//
//        @Override
//        public boolean[] inputRegisterOutputInterference(int u){
//            return new boolean[inputCount()];
//        }
//
//        @Override
//        public int[] inputRegisterIntraInterference() {
//            int[] out = new int[inputCount()];
//            for (int i = 0; i < out.length; i++) {
//                out[i]=i;
//            }
//            return out;
//        }
//
//        @Override
//        public boolean clobbersInput(int i) {
//            return false;
//        }
//
//
//        @Override
//        public String code(List<? extends RegisterAllocator.Register> uppers, List<? extends RegisterAllocator.Register> lowers, List<? extends RegisterAllocator.Register> trash) {
//            return String.format("//Null Instruction: %s <- %s", uppers, lowers);
//        }
//
//        @Override
//        public int inputCount() {
//            return inputCount;
//        }
//
//        @Override
//        public int outputCount() {
//            return outputCount;
//        }
//
//        @Override
//        public double cost() {
//            return 0;
//        }
//
//        @Override
//        public String toStringN() {
//            return "Null_t";
//        }
//
//        @Override
//        public String toString() {
//            return "Null_t";
//        }
//    }


}
