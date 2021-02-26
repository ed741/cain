package uk.co.edstow.cain.transformations;

import uk.co.edstow.cain.regAlloc.RegisterAllocator;

public interface StandardTransformation extends Transformation<RegisterAllocator.Register> {

    /**
     * @return the number of inputs the transformation has (aka the size of Lowers)
     */
    int inputCount();

    /**
     * @return the number of outputs the transformation has (aka the size of Uppers)
     */
    int outputCount();


    /**
     * @param u An index into the Transformation's Uppers
     * @return A boolean array of length equal to inputCount() where values are True iff the register for that upper and
     * that lower cannot be the same.
     */
    boolean[] inputRegisterOutputInterference(int u);


    /**
     * @return the number of extra registers required to use this transformation
     * If we have a goalBag with k registers, n of which are inputs (Lowers) to this transformation, how many more than
     * n or k registers are needed to apply this function
     */
    default int ExtraRegisterCount(){
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

    String toStringN();

}
