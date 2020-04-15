package uk.co.edstow.cpacgen;

import uk.co.edstow.cpacgen.pairgen.PairGenFactory;
import uk.co.edstow.cpacgen.util.Tuple;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;


public class ReverseSplit {

    private final int divisions;
    private final Goal initialGoal;
    private final Goal.Bag finalGoals;
    private final List<Plan> plans;
    private final GoalsCache cache;
    private final PairGenFactory pairGenFactory;
    private final BlockingQueue<State> workQueue;
    private final int workers;
    private boolean[] activeWorkers;
    private double[] averageWorkerDepth;
    private int[] minWorkerDepth;
    private ReentrantLock activeWorkersLock;
    private AtomicIntegerArray plansFound;
    private AtomicBoolean end;
    private Semaphore workersFinished;
    private long startTime;

    private final int availableRegisters;
    private AtomicInteger maxDepth;
    private final boolean timeout;
    private long maxTime;
    private int allowableAtomsCoefficent;

    public ReverseSplit(int divisions, List<Goal> finalGoals, PairGenFactory pairGenFactory, int availableRegisters, int maxDepth) {
        this.divisions = divisions;
        this.finalGoals = new Goal.Bag();
        this.finalGoals.addAll(finalGoals);
        this.finalGoals.setImmutable();
        Goal.Factory initialGoalFactory = new Goal.Factory();
        for( int i = 0; i < 1 << this.divisions; i++){
            initialGoalFactory.add(new Atom(0,0,0, true));
        }
        this.initialGoal = initialGoalFactory.get();
        plans = new ArrayList<>();
        cache = new GoalsCache();
        this.pairGenFactory = pairGenFactory;
        this.pairGenFactory.init(this);
        workers = 4;

        activeWorkers = new boolean[workers];
        averageWorkerDepth = new double[workers];
        minWorkerDepth = new int[workers];
        activeWorkersLock = new ReentrantLock();
        plansFound = new AtomicIntegerArray(workers);
        workQueue = new LinkedBlockingQueue<>();

        allowableAtomsCoefficent = 2;
        this.availableRegisters = availableRegisters;
        this.maxDepth = new AtomicInteger(maxDepth);
        maxTime = 3400;//milliseconds
        end = new AtomicBoolean(false);
        workersFinished = new Semaphore(1-this.workers);
        timeout = false;

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
        boolean s = false;
        try {
            activeWorkersLock.lock();
            s =  workQueue.isEmpty() && !activeWorkers[(id+1)%workers];
//            if (s) {
//                activeWorkers[(id+1)%workers]= true;
//            }
        } finally {
            activeWorkersLock.unlock();
        }
        return s;

    }
    private boolean allWorkersFinished(){
        try {
            boolean finished = true;
            activeWorkersLock.lock();
            for (boolean activeWorker : activeWorkers) {
                finished = finished && activeWorker;
            }
            return finished;
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
                    System.out.println("Press enter to stop");
                    Scanner scan = new Scanner(System.in);
                    scan.nextLine();
                    System.out.println("Search ended by user");
                    endTime();
                    mainThread.interrupt();
                }
            }.start();
        }

        while (!finish()) {
            try {
                Thread.sleep(100);
                String active = workersActive();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < averageWorkerDepth.length; i++) {
                    String v = String.valueOf(((double) Math.round(averageWorkerDepth[i] * 100)) /100);
                    sb.append(v);
                    for(int j = v.length(); j<5; j++){
                        sb.append(' ');
                    }
                    if(i == averageWorkerDepth.length-1){
                        sb.append("]");
                        break;
                    }
                    sb.append(',');

                }
                String avgdepth = sb.toString();
                System.out.print("\rRunning for: " + Duration.ofMillis(System.currentTimeMillis() - startTime).getSeconds() + " seconds - " + active + " : " + avgdepth + " : " + Arrays.toString(minWorkerDepth));
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

        public State(int depth, Goal.Bag goals, Plan currentPlan) {
            this.depth = depth;
            this.goals = goals;
            this.currentPlan = currentPlan;
        }

    }

    private class Worker implements Runnable {
        int id;

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
                        rSearch(s.depth, s.goals, s.currentPlan);
                    }
                }
            } catch (InterruptedException e) {
                //ignore
                //System.out.println(id + " interpreted!");
            }
            System.out.println("Worker " + id + " Finished");
            workersFinished.release();
        }


        private void rSearch(int depth, Goal.Bag goals, Plan currentPlan) {
            averageWorkerDepth[id] = averageWorkerDepth[id]*0.99 + depth*0.01;
            minWorkerDepth[id] = Math.min(minWorkerDepth[id], depth);
            if (finish()){
                return;
            }
            int currentMaxDepth = maxDepth.get();
            if (depth < currentMaxDepth-3){
                //System.out.println(id + " depth: " + depth);
            }
//            System.out.println(depth);
//            System.out.println(currentPlan.toGoalsString());
//            System.out.println("Current Goals");
//            System.out.println(goals.toGoalsString());

////            System.out.println("search");
            if (depth >= currentMaxDepth) {
                return;
            }
            if (currentMaxDepth-depth < (goals.size()-2)){
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
                if(g.equals(initialGoal)){
                    addPlan(currentPlan, id, depth);
                    return;
                }
                List<Goal.Pair> pairs = isTransformable(g, depth);
                if (pairs!= null && !pairs.isEmpty()) {
                    Plan p = currentPlan;
                    for (int i = 0; i < pairs.size(); i++) {
                        Goal.Pair pair = pairs.get(i);
                        goals.remove(pair.getUpper());
                        goals.addAll(pair.getLowers());
                        p = p.newAdd(pair, new Goal.Bag(goals), "final step "+i);
                    }
                    addPlan(p, id, depth);
                    return;
                }
            }
            PairGenFactory.PairGen goalPairs = pairGenFactory.generatePairs(goals, depth);
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
                if (finish()){
                    return;
                }
                if(shareWork(id)) {
                    System.out.println("Sharing work "+id);
                    workQueue.add(new State(depth + 1, newGoals, currentPlan.newAdd(goalPair, newGoals, "r_step")));
                } else {
                    rSearch(depth+1, newGoals, currentPlan.newAdd(goalPair, newGoals, "r_step"));
                }

                goalPair = goalPairs.next();
            }

        }

    }

    private synchronized void addPlan(Plan p, int id, int depth){
        for (int i = 0; i < minWorkerDepth.length; i++) {
            minWorkerDepth[i] = depth;
        }
        plansFound.getAndIncrement(id);

        System.out.println("PlansFound by workers: "+ plansFound.toString());
        System.out.println("Found new Plan! (id:"+id+") length: " + p.depth() + " Cost: "+p.cost());
        System.out.println(p);
        maxDepth.getAndUpdate(x->Math.min(depth, x));

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
