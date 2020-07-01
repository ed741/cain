package uk.co.edstow.cain;

import java.util.*;

class Main {
    public static void main(String[] args) {
        System.out.println(Arrays.toString(args));
        for (int i = 0; i < args.length; i++) {
            FileRun fileRun = FileRun.loadFromJson(args[i]);
            fileRun.run();
            String code = fileRun.getBest();
            System.out.println(code);
        }
    }
}
