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
        //interface
        public boolean liveCounter;

        // Search capabilities
        public int workers;

        // Search limits
        public int searchTime;
        public boolean timeOut;

        //Search Heuristics
        public int initialMaxDepth;
        public int forcedDepthReduction; // once a plan is found at depth 10, cull searches at "depth - forcedDepthReduction".
        //public int availableRegisters; // The maximum number of current goals
        public RegisterAllocator registerAllocator; // The register allocator to ensure plans are allocatable

        public int allowableAtomsCoefficent; // cull plans that use more atoms than: "finalGoal * allowableAtomsCoefficent + initalGoal".
        public int goalReductionsPerStep; // cull plans that have more active goals than steps before max depth to reduce the number to one.
        public int goalReductionsTolorance; // add a tolerance to allow for a "less conservative" (lower) goalReductionsPerStep if larger reductions are unlikely.
        public int maxChildrenInDFS; // Number of goal pairs to search at each point before returning (pairs not searched are saved into the work queue for later).

        public RunConfig(boolean liveCounter, int workers, int searchTime, boolean timeOut, int initialMaxDepth, int forcedDepthReduction, RegisterAllocator registerAllocator, int allowableAtomsCoefficent, int goalReductionsPerStep, int goalReductionsTolorance, int maxChildrenInDFS) {
            this.liveCounter = liveCounter;
            this.workers = workers;
            this.searchTime = searchTime;
            this.timeOut = timeOut;
            this.initialMaxDepth = initialMaxDepth;
            this.forcedDepthReduction = forcedDepthReduction;
            this.registerAllocator=registerAllocator;
            this.allowableAtomsCoefficent = allowableAtomsCoefficent;
            this.goalReductionsPerStep = goalReductionsPerStep;
            this.goalReductionsTolorance = goalReductionsTolorance;
            this.maxChildrenInDFS = maxChildrenInDFS;
        }

        public RunConfig() {
            this.liveCounter = true;
            this.workers = 1;
            this.searchTime = 60000;
            this.timeOut = false;
            this.initialMaxDepth = 200;
            this.forcedDepthReduction = 1;
            this.allowableAtomsCoefficent = 2;
            this.goalReductionsPerStep = 1;
            this.goalReductionsTolorance = 1;
            this.maxChildrenInDFS = Integer.MAX_VALUE;
        }

        public RunConfig setLiveCounter(boolean liveCounter){
            this.liveCounter = liveCounter;
            return this;
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

        public RunConfig setRegisterAllocator(RegisterAllocator registerAllocator) {
            if (registerAllocator == null){
                throw new IllegalArgumentException("Register Allocator cannot be null");
            }
            if(registerAllocator.getAvailableRegisters() < 1){
                throw new IllegalArgumentException("Available registers must be greater than 0 (and no less than the number of final goals)");
            }
            this.registerAllocator = registerAllocator;
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
    private final ReentrantLock planLock;
    private final GoalsCache cache;

    private final PairGenFactory pairGenFactory;

    private final BlockingQueue<State> workQueue;
    private final int workers;
    private AtomicBoolean end;
    private Semaphore workersFinished;

    private long startTime;
    private final boolean liveCounter;

    private final boolean timeout;
    private final long maxTime;
    private final AtomicInteger maxDepth;
    private final RegisterAllocator registerAllocator;
    private final int availableRegisters;

    private final int allowableAtomsCoefficent;
    private final int forcedDepthReduction;
    private final int goalReductionsPerStep;
    private final int goalReductionsTolerance;
    private final int maxChildrenInDFS;

    public ReverseSearch(int divisions, List<Goal> finalGoals, PairGenFactory pairGenFactory, RunConfig runConfig) {
        this.liveCounter = runConfig.liveCounter;

        // Set up final and initial Goals
        this.finalGoals = Collections.unmodifiableList(new ArrayList<>(finalGoals));
        this.finalGoalAtoms = new Goal.Bag(this.finalGoals).atomCount();
        this.divisions = divisions;
        this.initialGoal = new Goal.Factory().add(new Atom(0,0,0, true), 1<<this.divisions).get();

        // Init Internal components
        this.plans = new ArrayList<>();
        this.planLock = new ReentrantLock();
        this.cache = new GoalsCache();


        // Init worker thread trackers and job distribution
        this.workers = runConfig.workers;
        this.workQueue = new LinkedBlockingQueue<>();

        this.end = new AtomicBoolean(false);
        this.workersFinished = new Semaphore(1-this.workers);


        // Init search parameters
        this.availableRegisters = runConfig.registerAllocator.getAvailableRegisters();
        this.maxDepth = new AtomicInteger(runConfig.initialMaxDepth);
        this.maxTime = runConfig.searchTime;
        this.timeout = runConfig.timeOut;
        this.registerAllocator = runConfig.registerAllocator;

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

    private String getWorkerInfo(List<Worker> workers){
        StringBuilder sb = new StringBuilder();
        sb.append("Active:[");
        for (int i = 0; i < workers.size(); i++) {
            sb.append(workers.get(i).active ? 'A' : 'W');
            sb.append(i==workers.size()-1?']':',');
        }
        sb.append(" | MinDepth:[");
        for (int i = 0; i < workers.size(); i++) {
            sb.append(workers.get(i).workerMinDepth);
            sb.append(i==workers.size()-1?']':',');
        }
        sb.append(" | MaxDepth:[");
        for (int i = 0; i < workers.size(); i++) {
            sb.append(workers.get(i).workerMaxDepth);
            sb.append(i==workers.size()-1?']':',');
        }
        sb.append(" | PlansFound:[");
        for (int i = 0; i < workers.size(); i++) {
            sb.append(workers.get(i).plansFound);
            sb.append(i==workers.size()-1?']':',');
        }
        sb.append(" | CacheHits:[");
        for (int i = 0; i < workers.size(); i++) {
            sb.append(workers.get(i).cacheHits);
            sb.append(i==workers.size()-1?']':',');
        }
        sb.append(" | CacheChecks:[");
        for (int i = 0; i < workers.size(); i++) {
            sb.append(workers.get(i).cacheChecks);
            sb.append(i==workers.size()-1?']':',');
        }
        return sb.toString();
    }

    public void search(){
        startTime = System.currentTimeMillis();
        Goal.Bag goals = new Goal.Bag(finalGoals);
        workQueue.add(new State(0, goals, new Plan(finalGoals, initialGoal, "final goals")));
        List<Worker> workersThreads = new ArrayList<>();
        for (int i = 0; i < this.workers; i++) {
            Worker worker = new Worker(i);
            workersThreads.add(worker);
            if(i>0){
                worker.next = workersThreads.get(i-1);
            }
        }
        workersThreads.get(0).next = workersThreads.get(workersThreads.size()-1);
        workersThreads.forEach(Thread::start);
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
                            for (Worker workersThread : workersThreads) {
                                workersThread.workerMinDepth = Integer.MAX_VALUE;
                                workersThread.workerMaxDepth = 0;
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
                if(liveCounter) {
                    System.out.print("\rRunning for: " + Duration.ofMillis(System.currentTimeMillis() - startTime).getSeconds() + " seconds"
                            + " | " + getWorkerInfo(workersThreads) + " | Cache Size: " + cache.size());
                }
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

    private class Worker extends Thread {
        int id;
        boolean finishing = false;
        boolean active = false;
        int workerMinDepth = 0;
        int workerMaxDepth = 0;
        Worker next;
        int plansFound = 0;
        int cacheChecks = 0;
        int cacheHits = 0;

        public Worker(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            System.out.println("Worker " + id + " Starting");
            try {
                while (!finish()) {
                    ReverseSearch.State s = workQueue.poll();
                    if (s == null) {
                        active = false;
                        s = workQueue.take();
                    }
                    if (s == null) {
                        active = false;
                    } else {
                        active = true;
                        rSearch(s.depth, s.goals, s.currentPlan, s.pairGen);
                    }
                }
            } catch (InterruptedException e) {
                //ignore
                //System.out.println(id + " interpreted!");
            }
            System.out.println(String.format("Worker %d Finished:  Active: %b,  Min Depth: %d,  Max Depth: %d,  PlansFound: %d, CacheHits: %d,  CacheChecks: %d  ", id, active, workerMinDepth, workerMaxDepth, plansFound, cacheHits, cacheChecks));
            workersFinished.release();
        }

        private boolean shareWork(int id){
            return workQueue.isEmpty() && next != null && !next.active;
        }


        private void rSearch(int depth, Goal.Bag goals, Plan currentPlan, PairGenFactory.PairGen iterator) {
            this.workerMinDepth = Math.min(this.workerMinDepth, depth);
            this.workerMaxDepth = Math.max(this.workerMaxDepth, depth);
            if (finish()){
                if(!finishing) {
                    System.out.println("Finishing before complete");
//                    System.out.println(currentPlan.toGoalsString());
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

            cacheChecks++;
            if (!cache.isBest(goals, currentPlan.cost())) {
                cacheHits++;
                return;
            }

            if (goals.size() == 1) {
                Goal g = goals.iterator().next();
                if(g.equals(initialGoal)){
                    if(addPlan(currentPlan.copy(), id, depth)){
                        return;
                    }
                }
                List<Goal.Pair> pairs = isTransformable(g, depth);
                if (pairs!= null && !pairs.isEmpty()) {
                    Plan p = currentPlan.copy();
                    for (int i = 0; i < pairs.size(); i++) {
                        Goal.Pair pair = pairs.get(i);
                        boolean e = goals.removeEquivalent(pair.getUpper());
                        if(!e){

                            System.out.println(currentPlan.toGoalsString());

                            System.out.println("ERROR"+id + " \n" + pair.getUpper().getCharTableString(true, true, true, true) + "--");
                            System.out.println("ERROR"+id + " \n" + pair.getTransformation().toString() + "--");
                            System.out.println("ERROR"+id + " \n" + pair.getTransformation().toStringN() + "--");
                            System.exit(-1);

                        }
                        assert pair.getLowers().size() == 1;
                        goals.addAll(pair.getLowers());
                        Goal[] translation = new Goal[1];
                        translation[0] = pair.getLowers().get(0);
                        p.push(pair, new Goal.Bag(goals), translation, "final step "+i);
                    }
                    if(addPlan(p, id, depth)){
                        return;
                    }
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
//                System.out.println(goalPair.toStringN());

                Goal.Bag newGoals = new Goal.Bag(goals);
                if(!newGoals.removeEquivalent(goalPair.getUpper())){
                    assert false: "Pair Upper ERROR";
                }
                ArrayList<Goal> toAdd = new ArrayList<>();
                List<Goal> lowers = goalPair.getLowers();
                int[] interference = goalPair.getTransformation().inputRegisterIntraInterference();
                Goal[] translation = new Goal[interference.length];
                for (int i = 0; i < lowers.size(); i++) {
                    Goal l = lowers.get(i);

                    boolean add = true;
                    int c = i;
                    while (c != interference[c]){
                        if(lowers.get(i).same(lowers.get(interference[c]))){
                            add = false;
                            break;
                        }
                        c = interference[c];
                    }
                    if(add) {
                        boolean addNew = true;
                        for (int j = 0; j < newGoals.size(); j++) {
                            Goal goal = newGoals.get(j);
                            if (goal.same(l)) {
                                newGoals.remove(j);
                                toAdd.add(goal);
                                translation[i] = goal;
                                addNew = false;
                                break;
                            }
                        }
                        if(addNew) {
                            toAdd.add(l);
                            translation[i] = l;
                        }
                    } else {
                        translation[i] = translation[interference[c]];
                    }
                }
                newGoals.addAll(toAdd);

                if (finish()){
                    if(!finishing) {
                        System.out.println("Finishing before complete");
//                        System.out.println(currentPlan.toGoalsString());
                    }
                    finishing = true;
                    return;
                }

                if (newGoals.size() +(goalPair.getTransformation().inputRegisterOutputInterferes()?1:0) <= availableRegisters) {
                    if (shareWork(id)) {
                        //System.out.println("Sharing work "+id);
                        workQueue.add(new ReverseSearch.State(depth + 1, newGoals, currentPlan.newAdd(goalPair, newGoals, translation, "r_step"), null));
                    } else {
                        currentPlan.push(goalPair, newGoals, translation, "r_step");
                        rSearch(depth + 1, newGoals, currentPlan, null);
                        currentPlan.pop();
                    }
                }
                count--;
                goalPair = goalPairs.next();
            }
            if (goalPair != null){
                workQueue.add(new ReverseSearch.State(depth, goals, currentPlan.copy(), goalPairs));
            }

        }
        private boolean addPlan(Plan p, int id, int depth){
            RegisterAllocator.Mapping mapping = registerAllocator.solve(p);
            if (mapping == null){
                System.out.println(this.id + " Plan Found but registers cannot be allocated");
                return false;
            }


            Worker w = this;
            do {
                w.workerMinDepth = depth;
                w.workerMaxDepth = 0;
                w = w.next;
            } while(w != this);

            this.plansFound++;

            System.out.println("Found new Plan (id:"+id+"): Length: " + p.depth() + " ("+depth + ") | Cost: "+p.cost());
            System.out.println(p);
            maxDepth.getAndUpdate(x->Math.min(depth+1-forcedDepthReduction, x));
            try {
                planLock.lock();
                plans.add(p);

                if (plans.size() > 1) {
                    Plan lastPlan = plans.get(plans.size() - 2);
                    int i = 0;
                    while (i < p.getAll().size() && i < lastPlan.getAll().size() &&
                            lastPlan.getAll().get(i) == p.getAll().get(i)) {
                        i++;
                    }
                    System.out.println("Diverges from last plan at step: " + i);
                }
            } finally {
                planLock.unlock();
            }
            return true;
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
