package cpacgen;

import cpacgen.pairgen.PairGenFactory;
import cpacgen.pairgen.V1PairGenFactory;
import cpacgen.util.Tuple;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;


public class ReverseSplit {
    private final int divisions;
    private final Goal initialGoal;
    private final Goal.Bag finalGoals;
    private final List<Plan> plans;
    private final GoalsCache cache;
    private final PairGenFactory pairGenFactory;
    private final ExecutorService pool;
    private final BlockingQueue<State> workQueue;
    private final int workers;
    private volatile boolean[] activeWorkers;
    private volatile int[] plansFound;
    private long startTime;

    private final int availableRegisters;
    private volatile int maxDepth;
    private boolean timeout;
    private long maxTime;
    private int allowableAtomsCoefficent;

    public ReverseSplit(int divisions, List<Goal> finalGoals) {
        this.divisions = divisions;
        this.finalGoals = new Goal.Bag();
        this.finalGoals.addAll(finalGoals);
        this.finalGoals.setImmutable();
        Goal.Factory initialGoalFactory = new Goal.Factory();
        for( int i = 0; i < 1 << divisions; i++){
            initialGoalFactory.add(new Atom(0,0,0, true));
        }
        this.initialGoal = initialGoalFactory.get();
        plans = new ArrayList<>();
        cache = new GoalsCache();
        this.pairGenFactory = new V1PairGenFactory();
        pairGenFactory.init(this);
        workers = 1;

        pool = Executors.newFixedThreadPool(workers);
        activeWorkers = new boolean[workers];
        plansFound = new int[workers];
        workQueue = new LinkedBlockingQueue<>();

        allowableAtomsCoefficent = 2;
        availableRegisters = 8;
        maxDepth = 25;
        maxTime = 1000;//milliseconds
        timeout = true;


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

    public Goal getInitialGoal() {
        return initialGoal;
    }

    public Goal.Bag getFinalGoals() {
        return finalGoals;
    }

    public List<Plan> getPlans() {
        return plans;
    }

    private synchronized boolean useTimeout(){
        return timeout;
    }

    private synchronized void endTime(){
        timeout = true;
    }

    private synchronized void setWorking(int id, boolean working){
        activeWorkers[id] = working;
    };
    private synchronized boolean shareWork(int id){
        boolean s =  workQueue.isEmpty() && !activeWorkers[id == workers-1?0:id+1];
        if (s) {
            activeWorkers[id == workers-1?0:id+1] = true;
        }
        return s;

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
                waiting = !pool.awaitTermination(1000, TimeUnit.MILLISECONDS);
                System.out.print("\rRunning for: " + Duration.ofMillis(System.currentTimeMillis()-startTime).getSeconds() + " seconds");
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
                State s = null;
                try {
                    s = workQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (s == null) {
                    setWorking(id, false);
                } else {
                    setWorking(id, true);
                    rSearch(s.depth, s.goals, s.currentPlan);
                }
            }
            System.out.println("Worker " + id + " Finished");
            pool.shutdown();
        }


        private void rSearch(int depth, Goal.Bag goals, Plan currentPlan) {
            //System.out.println("search");
            if (depth >= maxDepth) {
                return;
            }
            if (!cache.isBest(goals, currentPlan.cost())) {
                cache.hit();
                return;
            }
            if (goals.atomCount() >= ((allowableAtomsCoefficent * finalGoals.atomCount()) + initialGoal.atomCount())) {
                return;
            }
            if (goals.size() > availableRegisters){
                return;
            }
            if (goals.size() == 1) {
                Goal g = goals.iterator().next();
                Transformation t = isTransformable(g);
                if (t != null) {
                    Plan p = currentPlan.newAdd(new Goal.Pair(g, initialGoal, t), new Goal.Bag(initialGoal), "final step");
                    addPlan(p, id);
                    return;
                }
            }
            PairGenFactory.PairGen goalPairs = pairGenFactory.generatePairs(goals);
            Goal.Pair goalPair = goalPairs.next();
            while (goalPair != null) {



                Goal.Bag newGoals = new Goal.Bag();
                for (Goal g : goals) {
                    if (!g.same(goalPair.getUpper())) {
                        newGoals.add(g);

                    }
                }
                for (Goal l : goalPair.getLowers()) {
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
                if (!(!useTimeout() || System.currentTimeMillis() < startTime + maxTime)){
                    return;
                }
                if(shareWork(id)) {
                    System.out.println("Sharing work "+id);
                    workQueue.add(new State(depth + 1, newGoals, currentPlan.newAdd(goalPair, newGoals, "r_step")));
                    //activeWorkers[id == workers-1?0:id+1] = true;
                } else {
                    rSearch(depth+1, newGoals, currentPlan.newAdd(goalPair, newGoals, "r_step"));
                }

                goalPair = goalPairs.next();
            }

        }

    }

    private synchronized void addPlan(Plan p, int id){
        plansFound[id] += 1;
        System.out.println("PlansFound by workers: "+ Arrays.toString(plansFound));
        System.out.println("Found new Plan! (id:"+id+") length: " + p.depth() + " Cost: "+p.cost());
        System.out.println(p);
        maxDepth = p.depth()-1;
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


    public void printStats(){
        System.out.println(cache);
        System.out.println("Number of Plans:" + plans.size());
        System.out.println("Current Max Depth: " + maxDepth);
    }



}
