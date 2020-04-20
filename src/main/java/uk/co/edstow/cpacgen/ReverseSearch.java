package uk.co.edstow.cpacgen;

import uk.co.edstow.cpacgen.pairgen.PairGenFactory;
import uk.co.edstow.cpacgen.util.Tuple;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;


public class ReverseSearch {

    public static class RunConfig {
        // Search capabilities
        public int workers;

        // Search limits
        public int searchTime;
        public boolean timeOut;

        //Search Heuristics
        public int initialMaxDepth;
        public int forcedDepthReduction; // once a plan is found at depth 10, cull searches at "depth - forcedDepthReduction".
        public int availableRegisters; // The maximum number of current goals

        public int allowableAtomsCoefficent; // cull plans that use more atoms than: "finalGoal * allowableAtomsCoefficent + initalGoal".
        public int goalReductionsPerStep; // cull plans that have more active goals than steps before max depth to reduce the number to one.
        public int goalReductionsTolorance; // add a tolerance to allow for a "less conservative" (lower) goalReductionsPerStep if larger reductions are unlikely.
        public int maxChildrenInDFS; // Number of goal pairs to search at each point before returning (pairs not searched are saved into the work queue for later).

        public RunConfig(int workers, int searchTime, boolean timeOut, int initialMaxDepth, int forcedDepthReduction, int availableRegisters, int allowableAtomsCoefficent, int goalReductionsPerStep, int goalReductionsTolorance, int maxChildrenInDFS) {
            this.workers = workers;
            this.searchTime = searchTime;
            this.timeOut = timeOut;
            this.initialMaxDepth = initialMaxDepth;
            this.forcedDepthReduction = forcedDepthReduction;
            this.availableRegisters = availableRegisters;
            this.allowableAtomsCoefficent = allowableAtomsCoefficent;
            this.goalReductionsPerStep = goalReductionsPerStep;
            this.goalReductionsTolorance = goalReductionsTolorance;
            this.maxChildrenInDFS = maxChildrenInDFS;
        }

        public RunConfig() {
            workers = 1;
            this.searchTime = 60000;
            this.timeOut = false;
            this.initialMaxDepth = 200;
            this.forcedDepthReduction = 1;
            this.allowableAtomsCoefficent = 2;
            this.goalReductionsPerStep = 1;
            this.goalReductionsTolorance = 1;
            this.maxChildrenInDFS = Integer.MAX_VALUE;
        }

        public RunConfig setWorkers(int workers) {
            if(workers < 1){
                throw new IllegalArgumentException("workers must be greater than 0");
            }
            this.workers = workers;
            return this;
        }

        public RunConfig setSearchTime(int searchTime) {
            if(searchTime < 0){
                throw new IllegalArgumentException("searchTime must be non-negative");
            }
            this.searchTime = searchTime;
            return this;
        }

        public RunConfig setTimeOut(boolean timeOut) {
            this.timeOut = timeOut;
            return this;
        }

        public RunConfig setInitialMaxDepth(int initialMaxDepth) {
            if(initialMaxDepth < 0){
                throw new IllegalArgumentException("depth must be non-negative");
            }
            this.initialMaxDepth = initialMaxDepth;
            return this;
        }

        public RunConfig setForcedDepthReduction(int forcedDepthReduction) {
            this.forcedDepthReduction = forcedDepthReduction;
            return this;
        }

        public RunConfig setAvailableRegisters(int availableRegisters) {
            if(availableRegisters < 1){
                throw new IllegalArgumentException("Available registers must be greater than 0 (and no less than the number of final goals)");
            }
            this.availableRegisters = availableRegisters;
            return this;
        }

        public RunConfig setAllowableAtomsCoefficent(int allowableAtomsCoefficent) {
            if(allowableAtomsCoefficent < 0){
                throw new IllegalArgumentException("allowableAtomsCoefficent must be non-negative");
            }
            this.allowableAtomsCoefficent = allowableAtomsCoefficent;
            return this;
        }

        public RunConfig setGoalReductionsPerStep(int goalReductionsPerStep) {
            if(goalReductionsPerStep < 0){
                throw new IllegalArgumentException("goalReductionsPerStep must be non-negative");
            }
            this.goalReductionsPerStep = goalReductionsPerStep;
            return this;
        }

        public RunConfig setGoalReductionsTolorance(int goalReductionsTolorance) {
            if(goalReductionsTolorance < 0){
                throw new IllegalArgumentException("goalReductionsTolorance must be non-negative");
            }
            this.goalReductionsTolorance = goalReductionsTolorance;
            return this;
        }

        public RunConfig setMaxChildrenInDFS(int maxChildrenInDFS) {
            if(maxChildrenInDFS < 0){
                throw new IllegalArgumentException("maxChildrenInDFS must be non-negative");
            }
            this.maxChildrenInDFS = maxChildrenInDFS;
            return this;
        }
    }

    private final int divisions;
    private final Goal initialGoal;
    private final List<Goal> finalGoals;
    private final int finalGoalAtoms;

    private final List<Plan> plans;
    private final GoalsCache cache;

    private final PairGenFactory pairGenFactory;

    private final BlockingQueue<State> workQueue;
    private final int workers;
    private boolean[] activeWorkers;
    private int[] minWorkerDepth;
    private int[] maxWorkerDepth;
    private ReentrantLock activeWorkersLock;
    private AtomicIntegerArray plansFound;
    private AtomicBoolean end;
    private Semaphore workersFinished;

    private long startTime;
    private final boolean timeout;
    private final long maxTime;
    private final AtomicInteger maxDepth;

    private final int availableRegisters;
    private final int allowableAtomsCoefficent;
    private final int forcedDepthReduction;
    private final int goalReductionsPerStep;
    private final int goalReductionsTolerance;
    private final int maxChildrenInDFS;

    public ReverseSearch(int divisions, List<Goal> finalGoals, PairGenFactory pairGenFactory, int availableRegisters, RunConfig runConfig) {

        // Set up final and initial Goals
        this.finalGoals = Collections.unmodifiableList(new ArrayList<>(finalGoals));
        this.finalGoalAtoms = new Goal.Bag(this.finalGoals).atomCount();
        this.divisions = divisions;
        this.initialGoal = new Goal.Factory().add(new Atom(0,0,0, true), 1<<this.divisions).get();

        // Init Internal components
        this.plans = new ArrayList<>();
        this.cache = new GoalsCache();


        // Init worker thread trackers and job distribution
        this.workers = runConfig.workers;
        this.workQueue = new LinkedBlockingQueue<>();

        this.activeWorkers = new boolean[workers];
        this.minWorkerDepth = new int[workers];
        this.maxWorkerDepth = new int[workers];
        this.activeWorkersLock = new ReentrantLock();
        this.end = new AtomicBoolean(false);
        this.workersFinished = new Semaphore(1-this.workers);
        this.plansFound = new AtomicIntegerArray(workers);


        // Init search parameters
        this.availableRegisters = availableRegisters;
        this.maxDepth = new AtomicInteger(runConfig.initialMaxDepth);
        this.maxTime = runConfig.searchTime;
        this.timeout = runConfig.timeOut;

        // Init Heuristics
        this.allowableAtomsCoefficent = runConfig.allowableAtomsCoefficent;
        this.forcedDepthReduction = runConfig.forcedDepthReduction;
        this.goalReductionsPerStep = runConfig.goalReductionsPerStep;
        this.goalReductionsTolerance = runConfig.goalReductionsTolorance;
        this.maxChildrenInDFS = runConfig.maxChildrenInDFS;


        // Init PairGen with complete Object
        this.pairGenFactory = pairGenFactory;
        this.pairGenFactory.init(this);

    }

    public Goal getInitialGoal() {
        return initialGoal;
    }

    public List<Goal> getFinalGoals() {
        return finalGoals;
    }

    public List<Plan> getPlans() {
        return plans;
    }

    public int getInitialDivisions() {
        return divisions;
    }


    private boolean finish(){
        return end.get();
    }

    private void endTime(){
        end.set(true);
    }

    private void setWorking(int id, boolean working){
        try {
            activeWorkersLock.lock();
            activeWorkers[id] = working;
        } finally {
            activeWorkersLock.unlock();
        }
    };
    private boolean shareWork(int id){
        try {
            activeWorkersLock.lock();
            return workQueue.isEmpty() && !activeWorkers[(id+1)%workers];
        } finally {
            activeWorkersLock.unlock();
        }

    }

    private String workersActive(){
        try {
            activeWorkersLock.lock();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < activeWorkers.length; i++) {
                sb.append(activeWorkers[i]);
                if (i < activeWorkers.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            return sb.toString();
        } finally {
            activeWorkersLock.unlock();
        }
    }

    public void search(){
        startTime = System.currentTimeMillis();
        Goal.Bag goals = new Goal.Bag(finalGoals);
        workQueue.add(new State(0, goals, new Plan(finalGoals, initialGoal, "final goals")));
        List<Thread> workersThreads = new ArrayList<>();
        for (int i = 0; i < this.workers; i++) {
            Thread t = new Thread(new Worker(i));
            workersThreads.add(t);
            t.start();

        }
        Thread mainThread = Thread.currentThread();

        if (!timeout){
            new Thread(){
                @Override
                public void run() {
                    super.run();
                    System.out.println("\"exit\" to stop");
                    Scanner scan = new Scanner(System.in);
                    String input = "";
                    do {
                        input = scan.next();//nextLine();
                        System.out.println('"' + input + '"');
                        if(input.equals("r")){
                            for (int i = 0; i < minWorkerDepth.length; i++) {
                                minWorkerDepth[i] = Integer.MAX_VALUE;
                                maxWorkerDepth[i] = 0;
                            }
                        }

                    } while(!(input.equals("exit") || input.equals("e")));
                    System.out.println("Search ended by user");
                    endTime();
                    mainThread.interrupt();
                }
            }.start();
        }

        while (!finish()) {
            try {
                Thread.sleep(1000);
                String active = workersActive();
                System.out.print("\rRunning for: " + Duration.ofMillis(System.currentTimeMillis() - startTime).getSeconds() + " seconds"
                        + " | Active:" + active
                        + " | MinDepth:" + Arrays.toString(minWorkerDepth)
                        + " | MaxDepth:" + Arrays.toString(maxWorkerDepth)
                        + " | PlansFound:" + plansFound.toString());
                if (timeout && System.currentTimeMillis() > startTime + maxTime) {
                    System.out.println("\nTime is up!");
                    endTime();
                }
            } catch (InterruptedException e) {
                //ignore
                //System.out.println("Master interrupted");
            }
        }
        workersThreads.forEach(Thread::interrupt);
        try {
            workersFinished.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Master finished waiting");

    }

    private static class State {
        final int depth;
        final Goal.Bag goals;
        final Plan currentPlan;
        final PairGenFactory.PairGen pairGen;

        public State(int depth, Goal.Bag goals, Plan currentPlan, PairGenFactory.PairGen pairGen) {
            this.depth = depth;
            this.goals = goals;
            this.currentPlan = currentPlan;
            this.pairGen = pairGen;
        }

        public State(int depth, Goal.Bag goals, Plan currentPlan) {
            this.depth = depth;
            this.goals = goals;
            this.currentPlan = currentPlan;
            this.pairGen = null;
        }

    }

    private class Worker implements Runnable {
        int id;
        boolean finishing = false;

        public Worker(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            System.out.println("Worker " + id + " Starting");
            try {
                while (!finish()) {
                    State s = workQueue.poll();
                    if (s == null) {
                        setWorking(id, false);
                        s = workQueue.take();
                    }
                    if (s == null) {
                        setWorking(id, false);
                    } else {
                        setWorking(id, true);
                        rSearch(s.depth, s.goals, s.currentPlan, s.pairGen);
                    }
                }
            } catch (InterruptedException e) {
                //ignore
                //System.out.println(id + " interpreted!");
            }
            System.out.println("Worker " + id + " Finished");
            workersFinished.release();
        }


        private void rSearch(int depth, Goal.Bag goals, Plan currentPlan, PairGenFactory.PairGen iterator) {
            minWorkerDepth[id] = Math.min(minWorkerDepth[id], depth);
            maxWorkerDepth[id] = Math.max(maxWorkerDepth[id], depth);
            if (finish()){
                if(!finishing) {
                    System.out.println("Finishing before complete");
                    System.out.println(currentPlan.toGoalsString());
                }
                finishing = true;
                return;
            }
            int currentMaxDepth = maxDepth.get();
//            System.out.println(depth);
//            System.out.println(currentPlan.toGoalsString());
//            System.out.println("Current Goals");
//            System.out.println(goals.toGoalsString());

////            System.out.println("search");
            if (depth > currentMaxDepth) {
                return;
            }
            if(goals.size() - (((currentMaxDepth-depth)*goalReductionsPerStep)+goalReductionsTolerance) > 1){
                return;
            }
            if (goals.atomCount() >= ((allowableAtomsCoefficent * finalGoalAtoms) + initialGoal.atomCount())) {
                return;
            }
            if (goals.size() > availableRegisters){
                return;
            }
            if (!cache.isBest(goals, currentPlan.cost())) {
                return;
            }
            if (goals.size() == 1) {
                Goal g = goals.iterator().next();
                if(g.equals(initialGoal)){
                    addPlan(currentPlan.copy(), id, depth);
                    return;
                }
                List<Goal.Pair> pairs = isTransformable(g, depth);
                if (pairs!= null && !pairs.isEmpty()) {
                    Plan p = currentPlan.copy();
                    for (int i = 0; i < pairs.size(); i++) {
                        Goal.Pair pair = pairs.get(i);
                        goals.remove(pair.getUpper());
                        goals.addAll(pair.getLowers());
                        //p = p.newAdd(pair, new Goal.Bag(goals), "final step "+i);
                        p.push(pair, new Goal.Bag(goals), "final step "+i);
                    }
                    addPlan(p, id, depth);
                    return;
                }
            }
            PairGenFactory.PairGen goalPairs;
            if(iterator == null) {
                goalPairs = pairGenFactory.generatePairs(goals, depth);
            } else {
                goalPairs = iterator;
            }
            Goal.Pair goalPair = goalPairs.next();
            int count = maxChildrenInDFS;
            while (goalPair != null && count > 0) {

                Goal.Bag newGoals = new Goal.Bag(goals);
                newGoals.remove(goalPair.getUpper());

                for (Goal l : goalPair.getLowers()) {
                    newGoals.addIfUnique(l);
                }

                if (finish()){
                    if(!finishing) {
                        System.out.println("Finishing before complete");
                        System.out.println(currentPlan.toGoalsString());
                    }
                    finishing = true;
                    return;
                }
                if(shareWork(id)) {
                    //System.out.println("Sharing work "+id);
                    workQueue.add(new State(depth + 1, newGoals, currentPlan.newAdd(goalPair, newGoals, "r_step"), null));
                } else {
                    currentPlan.push(goalPair, newGoals, "r_step");
                    rSearch(depth+1, newGoals, currentPlan, null);
                    currentPlan.pop();
                }
                count--;
                goalPair = goalPairs.next();
            }
            if (goalPair != null){
                workQueue.add(new State(depth, goals, currentPlan.copy(), goalPairs));
            }

        }

    }

    private synchronized void addPlan(Plan p, int id, int depth){
        for (int i = 0; i < minWorkerDepth.length; i++) {
            minWorkerDepth[i] = depth;
            maxWorkerDepth[i] = 0;
        }
        plansFound.getAndIncrement(id);

        System.out.println("PlansFound by workers: "+ plansFound.toString());
        System.out.println("Found new Plan (id:"+id+"): Length: " + p.depth() + " ("+depth + ") | Cost: "+p.cost());
        System.out.println(p);
        maxDepth.getAndUpdate(x->Math.min(p.depth()-forcedDepthReduction, x));
        plans.add(p);

        if(plans.size()>1) {
            Plan lastPlan = plans.get(plans.size() - 2);
            int i = 0;
            while (1 < p.getAll().size() && lastPlan.getAll().get(i) == p.getAll().get(i)) {
                i++;
            }
            System.out.println("Diverges from last plan at step: " + i);
        }
    }



    /**
     *
     * @param goal a goal to find a trnsformation for.
     * @return the Transformation initial_goal -> goal; iff the initial goal can be directly transformed into goal, else null
     */
    private List<Goal.Pair> isTransformable(Goal goal, int depth) {
        assert !goal.isEmpty();
        for(Tuple<List<Goal.Pair>, Goal> tuple : this.pairGenFactory.applyAllUnaryOpForwards(initialGoal, depth, goal)){
            List<Goal.Pair> pairs = tuple.getA();
            Goal g = tuple.getB();
            if(goal.same(g)){
                return pairs;
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
