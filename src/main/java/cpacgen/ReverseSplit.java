package cpacgen;

import com.sun.xml.internal.stream.util.ThreadLocalBufferAllocator;
import cpacgen.util.Tuple;

import java.util.*;
import java.util.concurrent.*;


public class ReverseSplit {
    int divisions = 0;
    Goal initialGoal;
    Goal.Bag finalGoals;
    List<Plan> plans;
    long startTime;
    GoalsCache cache;
    PairGenFactory pairGenFactory;
    ExecutorService pool;
    ConcurrentLinkedQueue<State> workQueue;
    int workers = 4;
    volatile boolean[] activeWorkers;

    volatile int maxDepth = 16;
    boolean timeout = false;
    long maxTime = 1000;//milliseconds
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
        this.pairGenFactory = new SortPairGenFactory();
        pairGenFactory.init(this);

        pool = Executors.newFixedThreadPool(workers);
        activeWorkers = new boolean[workers];
        workQueue = new ConcurrentLinkedQueue<>();

        if (!useTimeout()){
            new Thread(){
                @Override
                public void run() {
                    super.run();
                    System.out.println("Press enter to stop");
                    Scanner scan = new Scanner(System.in);
                    scan.nextLine();
                    System.out.println("Time is up");
                    pool.shutdown();
                    endTime();
                }
            }.start();
        }

    }

    private synchronized boolean useTimeout(){
        return timeout;
    }

    private synchronized void endTime(){
        timeout = true;
    }

    public void search(){
        startTime = System.currentTimeMillis();
        Goal.Bag goals = new Goal.Bag(finalGoals);
        workQueue.add(new State(0, goals, new Plan(finalGoals, "final goals")));
        for (int i = 0; i < workers; i++) {
            pool.execute(new worker(i));
        }
        boolean waiting = true;
        while (waiting) {
            try {
                waiting = !pool.awaitTermination(maxTime, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                //ignore
                System.out.println("Master is Waiting");
            }
        }
        System.out.println("Master finished waiting");

    }

    private static class State {
        final int depth;
        final Goal.Bag goals;
        final Plan currentPlan;

        public State(int depth, Goal.Bag goals, Plan currentPlan) {
            this.depth = depth;
            this.goals = goals;
            this.currentPlan = currentPlan;
        }

    }

    private class worker implements Runnable {
        int id;

        public worker(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            System.out.println("Worker " + id + " Starting");
            while ((!useTimeout()) || (System.currentTimeMillis() < startTime + maxTime)) {
                State s = workQueue.poll();
                if (s == null) {
                    activeWorkers[id] = false;
                } else {
                    activeWorkers[id] = true;
                    rSearch(s.depth, s.goals, s.currentPlan);
                }
            }
            System.out.println("Worker " + id + " Finished");
        }


        private void rSearch(int depth, Goal.Bag goals, Plan currentPlan) {
            if ((System.currentTimeMillis() > startTime + maxTime) && useTimeout()){
                return;
            }
            if (depth >= maxDepth) {
                //System.out.println("Max Depth!" + maxDepth);
                return;
            }
            if (!cache.isBest(goals, currentPlan.cost())) {
                cache.hit();
                return;
            }
            if (goals.atomCount() >= ((allowableAtomsCoefficent * finalGoals.atomCount()) + initialGoal.atomCount())) {
                return;
            }
            if (goals.size() == 1) {
                Goal g = goals.iterator().next();
                Transformation t = isTransformable(g);
                if (t != null) {
                    Plan p = currentPlan.newAdd(new Goal.Pair(g, initialGoal, t), "final step");
                    addPlan(p);
                    return;
                }
            }
            PairGenFactory.PairGen goalPairs = pairGenFactory.generatePairs(goals);
            Goal.Pair goalPair = goalPairs.next();
            while (goalPair != null) {

                Goal.Bag newGoals = new Goal.Bag();
                for (Goal g : goals) {
                    if (!g.same(goalPair.upper)) {
                        newGoals.add(g);

                    }
                }
                for (Goal l : goalPair.lowers) {
                    boolean add = true;
                    for (Goal g : newGoals) {
                        if (g.same(l)) {
                            add = false;
                            break;
                        }
                    }
                    if (add) {
                        newGoals.add(l);
                    }
                }
                //rSearch(depth+1, newGoals_list, currentPlan.newAdd(new Plan.Step(goals,newGoals_list , goalPair, "r_step")));
                //rSearch(depth+1, newGoals, currentPlan.newAdd(goalPair, "r_step"));
                if(!activeWorkers[id == workers-1?0:id+1]) {
                    System.out.println("Sharing work "+id);
                    workQueue.add(new State(depth + 1, newGoals, currentPlan.newAdd(goalPair, "r_step")));
                } else {
                    rSearch(depth+1, newGoals, currentPlan.newAdd(goalPair, "r_step"));
                }

                goalPair = goalPairs.next();
            }

        }

    }

    private synchronized void addPlan(Plan p){
        System.out.println("Found new Plan! length: " + p.depth() + " Cost: "+p.cost());
        System.out.println(p);
        maxDepth = p.depth();
        plans.add(p);

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
