package uk.co.edstow.cain;

import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.util.Tuple;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;


public class ReverseSearch {

    @SuppressWarnings({"UnusedReturnValue", "unused"})
    public static class RunConfig {
        //interface
        private boolean liveCounter;
        private int livePrintPlans;

        // Search capabilities
        private int workers;

        // Search limits
        private int searchTime;
        private boolean timeOut;

        // Search Rules
        private Supplier<? extends TraversalSystem> traversalAlgorithm;
        private int initialMaxDepth;
        private int forcedDepthReduction; // once a plan is found at depth 10, cull searches at "depth - forcedDepthReduction".
        private int initialMaxCost;
        private int forcedCostReduction;
        private RegisterAllocator registerAllocator; // The register allocator to ensure plans are allocatable
        private Function<Plan, Integer> costFunction;

        // Search Heuristics
        private int allowableAtomsCoefficient; // cull plans that use more atoms than: "finalGoal * allowableAtomsCoefficient + initialGoals".
        private int goalReductionsPerStep; // cull plans that have more active goals than steps before max depth to reduce the number to one.
        private int goalReductionsTolerance; // add a tolerance to allow for a "less conservative" (lower) goalReductionsPerStep if larger reductions are unlikely.

        public RunConfig(boolean liveCounter, int livePrintPlans, int workers, int searchTime, boolean timeOut, Supplier<TraversalSystem> traversalAlgorithm, Function<Plan, Integer> costFunction, int initialMaxDepth, int forcedDepthReduction, int initialMaxCost, int forcedCostReduction, RegisterAllocator registerAllocator, int allowableAtomsCoefficient, int goalReductionsPerStep, int goalReductionsTolerance) {
            this.liveCounter = liveCounter;
            this.livePrintPlans = livePrintPlans;
            this.workers = workers;
            this.searchTime = searchTime;
            this.timeOut = timeOut;
            this.traversalAlgorithm = traversalAlgorithm;
            this.costFunction = costFunction;
            this.initialMaxDepth = initialMaxDepth;
            this.forcedDepthReduction = forcedDepthReduction;
            this.initialMaxCost = initialMaxCost;
            this.forcedCostReduction = forcedCostReduction;
            this.registerAllocator=registerAllocator;
            this.allowableAtomsCoefficient = allowableAtomsCoefficient;
            this.goalReductionsPerStep = goalReductionsPerStep;
            this.goalReductionsTolerance = goalReductionsTolerance;
        }

        public RunConfig() {
            this.liveCounter = true;
            this.livePrintPlans = 2;
            this.workers = 1;
            this.searchTime = 60000;
            this.timeOut = false;
            this.traversalAlgorithm = TraversalSystem.SOTFactory();
            this.costFunction = p -> p.maxCircuitDepth() + 200*p.depth();
            this.initialMaxDepth = 200;
            this.forcedDepthReduction = 1;
            this.initialMaxCost = Integer.MAX_VALUE;
            this.forcedCostReduction = 0;
            this.allowableAtomsCoefficient = 2;
            this.goalReductionsPerStep = 1;
            this.goalReductionsTolerance = 1;
        }

        public RunConfig setLiveCounter(boolean liveCounter){
            this.liveCounter = liveCounter;
            return this;
        }
        public RunConfig setLivePrintPlans(int printPlans){
            this.livePrintPlans = printPlans;
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

        public RunConfig setTraversalAlgorithm(Supplier<? extends TraversalSystem> traversalAlgorithm) {
            this.traversalAlgorithm = traversalAlgorithm;
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

        public RunConfig setInitialMaxCost(int initialMaxCost) {
            this.initialMaxCost = initialMaxCost;
            return this;
        }

        public RunConfig setForcedCostReduction(int forcedCostReduction) {
            this.forcedCostReduction = forcedCostReduction;
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

        public RunConfig setAllowableAtomsCoefficient(int allowableAtomsCoefficient) {
            if(allowableAtomsCoefficient < 0){
                throw new IllegalArgumentException("allowableAtomsCoefficient must be non-negative");
            }
            this.allowableAtomsCoefficient = allowableAtomsCoefficient;
            return this;
        }

        public RunConfig setGoalReductionsPerStep(int goalReductionsPerStep) {
            if(goalReductionsPerStep < 0){
                throw new IllegalArgumentException("goalReductionsPerStep must be non-negative");
            }
            this.goalReductionsPerStep = goalReductionsPerStep;
            return this;
        }

        public RunConfig setGoalReductionsTolerance(int goalReductionsTolerance) {
            if(goalReductionsTolerance < 0){
                throw new IllegalArgumentException("goalReductionsTolerance must be non-negative");
            }
            this.goalReductionsTolerance = goalReductionsTolerance;
            return this;
        }
    }

    private final int[] divisions;
    private final List<Goal> initialGoals;
    private final int initialGoalsAtomCount;
    private final int inputs;
    private final List<Goal> finalGoals;
    private final int finalGoalAtoms;

    private final List<Plan> plans;
    private final List<Long> planTimes;
    private final ReentrantLock planLock;
    private final GoalsCache cache;

    private final PairGenFactory pairGenFactory;

    private final int workers;
    private final AtomicBoolean end;
    private final Semaphore workersFinished;

    private long startTime;
    private final boolean liveCounter;
    private final int livePrintPlans;

    private final boolean timeout;
    private final long maxTime;
    private final AtomicInteger maxDepth;
    private final AtomicInteger maxCost;
    private final RegisterAllocator registerAllocator;
    private final int availableRegisters;
    private final Supplier<? extends TraversalSystem> traversalAlgorithm;
    public final Function<Plan, Integer> costFunction;

    private final int allowableAtomsCoefficent;
    private final int forcedDepthReduction;
    private final int forcedCostReduction;
    private final int goalReductionsPerStep;
    private final int goalReductionsTolerance;


    public ReverseSearch(int[] divisions, List<Goal> finalGoals, PairGenFactory pairGenFactory, RunConfig runConfig) {
        this.liveCounter = runConfig.liveCounter;
        this.livePrintPlans = runConfig.livePrintPlans;

        // Set up final and initial Goals
        this.finalGoals = Collections.unmodifiableList(new ArrayList<>(finalGoals));
        this.finalGoalAtoms = new Goal.Bag(this.finalGoals).atomCount();
        this.divisions = divisions;
        this.inputs = divisions.length;
        this.initialGoals = new ArrayList<>();
        for (int i = 0; i < this.inputs; i++) {
            int division = this.divisions[i];
            initialGoals.add(new Goal.Factory().add(new Atom(0, 0, i, true), 1 << division).get());
        }
        initialGoalsAtomCount = initialGoals.stream().mapToInt(Goal::atomCount).sum();


        // Init Internal components
        this.plans = new ArrayList<>();
        this.planTimes = new ArrayList<>();
        this.planLock = new ReentrantLock();
        this.cache = new GoalsCache();


        // Init worker thread trackers and job distribution
        this.workers = runConfig.workers;
//        this.workQueue = new LinkedBlockingQueue<>();

        this.end = new AtomicBoolean(false);
        this.workersFinished = new Semaphore(1-this.workers);


        // Init search parameters
        this.traversalAlgorithm = runConfig.traversalAlgorithm;
        this.costFunction = runConfig.costFunction;
        this.availableRegisters = runConfig.registerAllocator.getAvailableRegisters();
        this.maxDepth = new AtomicInteger(runConfig.initialMaxDepth);
        this.maxCost = new AtomicInteger(runConfig.initialMaxCost);
        this.maxTime = runConfig.searchTime;
        this.timeout = runConfig.timeOut;
        this.registerAllocator = runConfig.registerAllocator;

        // Init Heuristics
        this.allowableAtomsCoefficent = runConfig.allowableAtomsCoefficient;
        this.forcedDepthReduction = runConfig.forcedDepthReduction;
        this.forcedCostReduction = runConfig.forcedCostReduction;
        this.goalReductionsPerStep = runConfig.goalReductionsPerStep;
        this.goalReductionsTolerance = runConfig.goalReductionsTolerance;


        // Init PairGen with complete Object
        this.pairGenFactory = pairGenFactory;
        this.pairGenFactory.init(this);

    }

    public List<Goal> getInitialGoals() {
        return initialGoals;
    }

    public List<Goal> getFinalGoals() {
        return finalGoals;
    }

    public List<Plan> getPlans() {
        return plans;
    }

    public int[] getInitialDivisions() {
        return divisions;
    }


    private boolean running(){
        return !end.get();
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
        sb.append(" | NodesExpanded:[");
        for (int i = 0; i < workers.size(); i++) {
            sb.append(workers.get(i).nodesExpanded);
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
        goals.setImmutable();
        List<Worker> workersThreads = new ArrayList<>();

        for (int i = 0; i < this.workers; i++) {
            Worker worker = new Worker(i);
            workersThreads.add(worker);
            if(i>0){
                worker.next = workersThreads.get(i-1);
            }
        }
        workersThreads.get(0).localTraversalSystem.add(new WorkState(0, goals, new Plan(finalGoals, initialGoals, "final goals")));
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
                    String input;
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

        while (running()) {
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

    static class WorkState {
        final int depth;
        final Goal.Bag goals;
        final Plan currentPlan;
        final PairGenFactory.PairGen pairGen;

        WorkState(int depth, Goal.Bag goals, Plan currentPlan, PairGenFactory.PairGen pairGen) {
            this.depth = depth;
            this.goals = goals;
            this.currentPlan = currentPlan;
            this.pairGen = pairGen;
        }

        WorkState(int depth, Goal.Bag goals, Plan currentPlan) {
            this.depth = depth;
            this.goals = goals;
            this.currentPlan = currentPlan;
            this.pairGen = null;
        }

    }

    private class Worker extends Thread {
        final int id;
        boolean active = false;
        int workerMinDepth = 0;
        int workerMaxDepth = 0;
        private Worker next;

        final TraversalSystem localTraversalSystem;
        int plansFound = 0;
        int cacheChecks = 0;
        int cacheHits = 0;
        int steals = 0;
        int nodesExpanded = 0;

        Worker(int id) {
            this.id = id;
            this.localTraversalSystem = traversalAlgorithm.get();

        }

        @Override
        public void run() {
            System.out.println("Worker " + id + " Starting");
            try {
                while (running()) {
                    WorkState s = localTraversalSystem.poll();
                    if (s == null) {
                        active = false;
                        s = stealWork();
                    }
                    active = true;
                    iSearch(s);
                }
            } catch (InterruptedException e) {
                //ignore
                //System.out.println(id + " interpreted!");
            }
            System.out.println(String.format("Worker %d Finished:  Active: %b,  Min Depth: %d,  Max Depth: %d,  PlansFound: %d, CacheHits: %d,  CacheChecks: %d,  Steals: %d, NodesExpanded: %d", id, active, workerMinDepth, workerMaxDepth, plansFound, cacheHits, cacheChecks, steals, nodesExpanded));
            workersFinished.release();
        }

        private WorkState stealWork() throws InterruptedException {
            Worker c = next;
            WorkState s = null;
            while (s==null){
                s = localTraversalSystem.steal(c.localTraversalSystem);
                c = c.next;
            }
            steals++;
            return s;
        }

        private void iSearch(WorkState s){
            int depth = s.depth;
            Goal.Bag goals = s.goals;
            Plan currentPlan = s.currentPlan;
            PairGenFactory.PairGen goalPairs = s.pairGen;



            this.workerMinDepth = Math.min(this.workerMinDepth, depth);
            this.workerMaxDepth = Math.max(this.workerMaxDepth, depth);
            int currentMaxDepth = maxDepth.get();
            int currentMaxCost = maxCost.get();
            if (depth > currentMaxDepth) {
                return;
            }
            if (costFunction.apply(currentPlan) > currentMaxCost) {
                return;
            }

            if (goals.size() - (((currentMaxDepth - depth) * goalReductionsPerStep) + goalReductionsTolerance) > 1) {
                return;
            }

            if(goalPairs == null) {

                if (goals.atomCount() >= ((allowableAtomsCoefficent * finalGoalAtoms) + initialGoalsAtomCount)) {
                    return;
                }

                cacheChecks++;
                if (!cache.isBest(goals, costFunction.apply(currentPlan))) {
                    cacheHits++;
                    return;
                }


                if (tryDirectSolve(depth, goals, currentPlan)) return;

                goalPairs = pairGenFactory.generatePairs(goals, depth);
                nodesExpanded++;
            }
            Goal.Pair goalPair = goalPairs.next();
            if(goalPair == null){
                return;
            }

            Goal.Bag newGoals = new Goal.Bag(goals);

            for (Goal upper : goalPair.getUppers()) {
                boolean removed = newGoals.removeEquivalent(upper);
                assert removed: "Pair Upper ERROR";
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
                    if(!goalPair.getTransformation().clobbersInput(i)) {
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
            newGoals.setImmutable();

            WorkState child = null;
            boolean tryChildren = newGoals.size() +(goalPair.getTransformation().ExtraRegisterCount()) <= availableRegisters;
            if(tryChildren) {
                Plan newPlan = currentPlan.newAdd(goalPair, newGoals, translation, "r_step");
                child = new WorkState(depth + 1, newGoals, newPlan, null);
            }
            WorkState next = new WorkState(depth, goals, currentPlan, goalPairs);
            localTraversalSystem.add(child, next);

        }

        private boolean tryDirectSolve(int depth, Goal.Bag goals, Plan currentPlan) {
            if (goals.size() > inputs) {
                return false;
            }
            if (goals.isEmpty() || goals.containsAll(initialGoals)) {
                if (addPlan(currentPlan, id, depth)) {
                    return true;
                }
            }
            List<Goal.Pair> pairs = isTransformable(goals, depth);
            if (pairs != null && !pairs.isEmpty()) {
                Plan p = currentPlan;
                Goal.Bag currentGoals = new Goal.Bag(goals);

                for (int i = 0; i < pairs.size(); i++) {
                    Goal.Pair pair = pairs.get(i);
                    for (Goal upper : pair.getUppers()) {
                        boolean removed = currentGoals.removeEquivalent(upper);
                        assert removed : "Pair Upper ERROR";
                    }
                    currentGoals.addAll(pair.getLowers());
                    Goal[] translation = new Goal[pair.getLowers().size()];
                    for (int l = 0; l < pair.getLowers().size(); l++) {
                        translation[l] = pair.getLowers().get(l);
                    }
                    boolean tryChildren = currentGoals.size() +(pair.getTransformation().ExtraRegisterCount()) <= availableRegisters;
                    if(!tryChildren){
                        return false;
                    }
                    p = p.newAdd(pair, currentGoals, translation, "final step " + i);
                }
                return addPlan(p, id, depth);
            }

            return false;
        }

        private boolean addPlan(Plan p, int id, int depth){
            RegisterAllocator.Mapping mapping = registerAllocator.solve(p, initialGoals);
            if (mapping == null){
                if (livePrintPlans>0) System.out.println(this.id + " Plan Found but registers cannot be allocated");
                return false;
            }
            maxDepth.updateAndGet(x->Math.min(p.depth()-forcedDepthReduction, x));
            maxCost.updateAndGet(x->Math.min(costFunction.apply(p)-forcedCostReduction, x));
            Worker w = this;
            do {
                w.workerMinDepth = depth;
                w.workerMaxDepth = 0;
                w = w.next;
            } while(w != this);

            this.plansFound++;

            if (livePrintPlans>0) System.out.println("Found new Plan (id:" + id + "): Length: " + p.depth() + " (" + depth + ") | max-Circuit-Depth: " + p.maxCircuitDepth() + " | Cost: " + costFunction.apply(p));
            if (livePrintPlans>1) System.out.println(p);


            try {
                planLock.lock();
                plans.add(p);
                planTimes.add(System.currentTimeMillis()-startTime);

                if (plans.size() > 1) {
                    Plan lastPlan = plans.get(plans.size() - 2);
                    int i = 0;
                    while (i < p.getAll().size() && i < lastPlan.getAll().size() &&
                            lastPlan.getAll().get(i) == p.getAll().get(i)) {
                        i++;
                    }
                    if (livePrintPlans>1) System.out.println("Diverges from last plan at step: " + i);
                }
            } finally {
                planLock.unlock();
            }
            return true;
        }
    }


    private List<Goal.Pair> isTransformable(Goal.Bag goals, int depth) {
        return this.pairGenFactory.applyAllUnaryOpForwards(initialGoals, depth, goals);
    }


    public void printStats(){
        System.out.println(cache);
        System.out.println("Number of Plans: " + plans.size());
        System.out.println("Current Max Depth: " + maxDepth);
        System.out.println("Current Max Cost: " + maxCost);
        try{
            planLock.lock();
            for (int i = 0; i < planTimes.size(); i++) {
                System.out.println("Plan: "+i+" (depth="+plans.get(i).depth()+") (circuit-depth="+Arrays.toString(plans.get(i).circuitDepths())+") found at " + planTimes.get(i) + "ms");
            }
        } finally {
            planLock.unlock();
        }
    }



}
