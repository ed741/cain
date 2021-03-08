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

    public static String Str(int val, int count){
        char[] chars = new char[count];
        for (int i = chars.length - 1; i >= 0; i--) {
            chars[i] = (val & 1) == 1? '1': '0';
            val = val >> 1;
        }
        return new String(chars);
    }
}
