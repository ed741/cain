package cpacgen;

import cpacgen.util.Tuple;

import java.util.*;


public class ReverseSplit {
    int divisions = 0;
    Goal initialGoal;
    Goal.Bag finalGoals;
    List<Plan> plans;
    long startTime;
    GoalsCache cache;

    int maxDepth = 8;
    boolean timeout = true;
    long maxTime = 10000;//milliseconds
    int allowableAtomsCoefficent = 2;

    public ReverseSplit(int divisions, List<Goal> finalGoals) {
        this.divisions = divisions;
        this.finalGoals = new Goal.Bag();
        this.finalGoals.addAll(finalGoals);
        this.finalGoals.setImmutable();
        Goal.Factory initialGoalFactory = new Goal.Factory();
        for( int i = 0; i < 1 << divisions; i++){
            initialGoalFactory.add(new Atom(0,0,0));
        }
        this.initialGoal = initialGoalFactory.get();
        plans = new ArrayList<>();
        cache = new GoalsCache();
    }

    public void search(){
        startTime = System.currentTimeMillis();
        Goal.Bag goals = new Goal.Bag(finalGoals);
        rSearch(0, goals, new Plan(finalGoals, "final goals"));
    }

    private void rSearch(int depth, Goal.Bag goals, Plan currentPlan){
        if (depth >= maxDepth || (timeout && System.currentTimeMillis() > startTime+maxTime)){
            //System.out.println("Max Depth!" + maxDepth);
            return;
        }
        if (!cache.isBest(goals, currentPlan.cost())){
            cache.hit();
            return;
        }
        if (goals.atomCount() >= ((allowableAtomsCoefficent * this.finalGoals.atomCount()) + this.initialGoal.atomCount())){
            return;
        }
        if (goals.size() == 1) {
            Goal g = goals.iterator().next();
            Transformation t = isTransformable(g);
            if(t != null){
                Plan p = currentPlan.newAdd(new Goal.Pair(g, initialGoal, t), "final step");
                addPlan(p);
                return;
            }
        }
        Iterator<Goal.Pair> goalPairs = generatePairs(goals);
        while (goalPairs.hasNext()) {
            Goal.Pair goalPair = goalPairs.next();
            Goal.Bag newGoals = new Goal.Bag();
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

    }

    private void addPlan(Plan p){
        System.out.println("Found new Plan! length: " + p.depth() + " Cost: "+p.cost());
        System.out.println(p);
        this.maxDepth = p.depth();
        this.plans.add(p);

    }

    private Iterator<Goal.Pair> generatePairs(Goal.Bag goals) {
        return new SimplePairGen(goals, this.finalGoals);
    }

    /**
     *
     * @param goal a goal to find a trnsformation for.
     * @return the Transformation initial_goal -> goal; iff the initial goal can be directly transformed into goal, else null
     */
    private Transformation isTransformable(Goal goal) {
        assert !goal.isEmpty();
        boolean scale = true;
        for(Tuple<? extends Transformation, Goal> tuple : Transformation.applyAllUnaryOpForwards(initialGoal)){
            Transformation t = tuple.getA();
            Goal g = tuple.getB();
            if(goal.same(g)){
                return t;
            }
        }
        return null;
    }



}
