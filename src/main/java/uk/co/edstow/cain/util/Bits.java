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

    public static String booleanArrayToString(boolean[][] arr){
        StringBuilder sb = new StringBuilder("[");
        for (int x = 0; x < arr.length; x++) {
            sb.append("[");
            for (int y = 0; y < arr[x].length; y++) {
                sb.append(arr[x][y]?"1":"0");
                if(y < arr[x].length-1){
                    sb.append(",");
                }
            }
            sb.append("]");
            if(x < arr.length-1){
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
