package uk.co.edstow.cain.transformations;

import uk.co.edstow.cain.regAlloc.BankedRegisterAllocator;

public interface BankedTransformation extends Transformation<BankedRegisterAllocator.BRegister> {

    /**
     * @param u An index into the Transformation's Uppers
     * @return A boolean array of length equal to inputCount() where values are True iff the register for that upper and
     * that lower cannot be the same.
     */
    boolean[] inputRegisterOutputInterference(int u);

    /**
     * @return the number of extra registers required to use this transformation
     * If we have a goalBag with k registers, n of which are inputs (Lowers) to this transformation, how many more than
     * n or k registers in 'bank' are needed to apply this function
     */
    int ExtraRegisterCount(int bank);
//    {
//        int count = 0;
//        for (int u = 0; u < outputCount(); u++) {
//            for (boolean b : inputRegisterOutputInterference(u)) {
//                if(b){
//                    count++;
//                    break;
//                }
//            }
//        }
//        return count;
//    }
}
