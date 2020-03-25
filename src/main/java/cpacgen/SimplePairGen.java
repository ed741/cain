package cpacgen;

import cpacgen.util.Bounds;
import cpacgen.util.Tuple;

import java.util.*;

public class SimplePairGen implements Iterator<Goal.Pair> {

    private List<Goal> goalList;
    private Goal.Bag final_goals;
    private List<Goal.Pair> currentList;
    private Goal.Pair n;
    private Bounds bounds;

    public SimplePairGen(Goal.Bag goals, Goal.Bag final_goals) {
        goalList = new ArrayList<>(goals);
        currentList = new ArrayList<>();
        this.final_goals = final_goals;
        bounds = new Bounds(final_goals);

        n = get_next();
        while(n != null && !check(n)){
            n = get_next();
        }
    }

    @Override
    public boolean hasNext() {
        return  n != null;
    }

    @Override
    public Goal.Pair next() {
        Goal.Pair p = n;
        n = get_next();
        while(n != null && !check(n)){
            n = get_next();
        }
        return p;

    }

    private boolean check(Goal.Pair p){
        for (Goal l: p.lowers) {
            for (Atom a : l) {
                if (!bounds.includes(a)) {
                    return false;
                }
            }
        }
        return true;

    }

    private Goal.Pair get_next() {
        if (!currentList.isEmpty()){
            Goal.Pair p = currentList.remove(0);
            return p;

        } else if (!goalList.isEmpty()){
            Goal upper = goalList.remove(0);

            // Addition
            Collection<Goal> splits = upper.allSplits();
            for(Goal a: splits){
                Goal b = upper.without(a);
                if (b.isEmpty()){
                    continue;
                }
                Transformation.Add add = new Transformation.Add(a, b);
                try {
                    if (!add.applyForwards().same(upper)) throw new AssertionError();
                } catch (Transformation.TransformationApplicationException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                List<Goal> lowers = new ArrayList<>();
                lowers.add(a);
                lowers.add(b);
                currentList.add(new Goal.Pair(upper, lowers, add));
            }

            // Mov and Div

            Collection<Tuple<? extends Transformation, Goal>> ts = Transformation.applyAllUnaryOpBackwards(upper);
            for (Tuple<? extends Transformation, Goal> t: ts){
                currentList.add(new Goal.Pair(upper, t.getB(), t.getA()));
            }

            return this.get_next();
        }
        return null;
    }


}
