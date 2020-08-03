package uk.co.edstow.cain.util;

import java.util.Random;

public class RandomKernel {
    public static int[][] makeRandom(Random r, int size, int min, int max, double sparsity){

        int[][] filter = new int[size][size];
        for (int i = 0; i < filter.length; i++) {
            for (int j = 0; j < filter[i].length; j++) {
                if(r.nextDouble() > sparsity){
                    filter[i][j] = r.nextInt((max+1)-min)+min;
                }
            }
        }
        return filter;
    }
}
