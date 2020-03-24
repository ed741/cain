package cpacgen;

import cpacgen.util.Tuple;

import java.util.*;


public class ReverseSpilt {
    int divisions = 0;
    Goal inital_goal;
    List<Goal> final_goals;
    List<Plan> plans;
    int maxDepth = 8;
    long maxTime = 10000;//milliseconds
    long startTime;

    public ReverseSpilt(int divisions, List<Goal> final_goals) {
        this.divisions = divisions;
        this.final_goals = final_goals;
        this.inital_goal = new Goal();
        for( int i = 0; i < 1 << divisions; i++){
            this.inital_goal.add(new Atom(0,0,0));
        }
        plans = new ArrayList<>();
    }

    public void search(){
        startTime = System.currentTimeMillis();
        List<Goal> goals = new ArrayList<>();
        goals.addAll(final_goals);
        rSearch(0, goals, new Plan(final_goals, "final goals"));
    }

    private void rSearch(int depth, List<Goal> goals, Plan currentPlan){
        if (depth >= maxDepth || System.currentTimeMillis() > startTime+maxTime){
            //System.out.println("Max Depth!");
            return;
        }
        if (goals.size() == 1) {
            Goal g = goals.iterator().next();
            Transformation t = isTransformable(g);
            if(t != null){
                List<Goal> ini = new ArrayList<>();
                ini.add(inital_goal);
                Plan p = currentPlan.newAdd(new Goal.Pair(g, inital_goal, t), "final step");
                addPlan(p);
                return;
            }
        }
        Iterator<Goal.Pair> goalPairs = generatePairs(goals);
        while (goalPairs.hasNext()) {
            Goal.Pair goalPair = goalPairs.next();
            List<Goal> newGoals = new ArrayList<>();
            for (Goal g: goals){
                if (!g.same(goalPair.upper)){
                    newGoals.add(g);

                }
            }
            for(Goal l: goalPair.lowers){
                boolean add = true;
                for (Goal g: newGoals){
                    if(g.same(l)){
                        add = false;
                        break;
                    }
                }
                if (add){
                    newGoals.add(l);
                }
            }
            //rSearch(depth+1, newGoals_list, currentPlan.newAdd(new Plan.Step(goals,newGoals_list , goalPair, "r_step")));
            rSearch(depth+1, newGoals, currentPlan.newAdd(goalPair, "r_step"));
        }
        //System.out.println("Exiting search, no pairs found!");
        return;

    }

    private void addPlan(Plan p){
        System.out.println("Found new Plan! length: " + p.depth() + " Cost: "+p.cost());
        System.out.println(p);
        this.maxDepth = p.depth();
        this.plans.add(p);

    }

    private Iterator<Goal.Pair> generatePairs(List<Goal> goals) {
        return new SimplePairGen(goals, this.final_goals);
    }

    /**
     *
     * @param goal a goal to find a trnsformation for.
     * @return the Transformation initial_goal -> goal; iff the initial goal can be directly transformed into goal, else null
     */
    private Transformation isTransformable(Goal goal) {
        assert !goal.isEmpty();
        boolean scale = true;
        for(Tuple<? extends Transformation, Goal> tuple : Transformation.applyAllUnaryOpForwards(inital_goal)){
            Transformation t = tuple.getA();
            Goal g = tuple.getB();
            if(goal.same(g)){
                return t;
            }
        }
        return null;
    }



}
