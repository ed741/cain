package uk.co.edstow.cain;

import java.util.*;

class Main {
    public static void main(String[] args) {
        System.out.println(Arrays.toString(args));
        for (int i = 0; i < args.length; i++) {
            FileRun<?,?,?> fileRun = FileRun.loadFromJson(args[i]);
            fileRun.run();

            String code = fileRun.getBest();
            List<? extends FileRun<?, ?, ?>.Result> results = fileRun.getResults();
            results.sort(Comparator.comparingInt(result -> result.cost));
            System.out.println(results.get(0).plan.toGoalsString());

            System.out.println(code);
        }
    }
}
