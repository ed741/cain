package uk.co.edstow.cain.util;

public class Bits {
    public static int log2nlz( int bits ) {
        if( bits == 0 ) return 0;
        return 31 - Integer.numberOfLeadingZeros( bits );
    }

    public static int countOnes(int bits) {
        return Integer.bitCount(bits);
    }

    public static boolean isOne(int val, int bit){
        return ((val >>> bit) & 1) != 0;
    }
}
