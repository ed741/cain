package cpacgen;

import cpacgen.pairgen.PairGenFactory;
import cpacgen.util.Tuple;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;


public class ReverseSplit {

    private final int divisions;
    private final Goal initialGoal;
    private final Goal.Bag finalGoals;
    private final List<Plan> plans;
    private final GoalsCache cache;
    private final PairGenFactory pairGenFactory;
    private final BlockingQueue<State> workQueue;
    private final int workers;
    private AtomicIntegerArray activeWorkers;
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

        activeWorkers = new AtomicIntegerArray(workers);
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
        activeWorkers.set(id, working?1:0);
    };
    private boolean shareWork(int id){
        boolean s =  workQueue.isEmpty() && activeWorkers.get((id+1)%workers)==0;
        if (s) {
            activeWorkers.set((id+1)%workers, 1);
        }
        return s;

    }
    private String workersActive(){

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < activeWorkers.length(); i++) {
            boolean b = activeWorkers.get(i)==1;
            sb.append(b);
            if(i <activeWorkers.length()-1){
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
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
                System.out.print("\rRunning for: " + Duration.ofMillis(System.currentTimeMillis() - startTime).getSeconds() + " seconds - " + active);
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
                    addPlan(currentPlan, id);
                    return;
                }
                Transformation t = isTransformable(g, depth);
                if (t != null) {
                    Plan p = currentPlan.newAdd(new Goal.Pair(g, initialGoal, t), new Goal.Bag(initialGoal), "final step");
                    addPlan(p, id);
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
                    //System.out.println("Sharing work "+id);
                    workQueue.add(new State(depth + 1, newGoals, currentPlan.newAdd(goalPair, newGoals, "r_step")));
                } else {
                    rSearch(depth+1, newGoals, currentPlan.newAdd(goalPair, newGoals, "r_step"));
                }

                goalPair = goalPairs.next();
            }

        }

    }

    private synchronized void addPlan(Plan p, int id){
        plansFound.getAndIncrement(id);

        System.out.println("PlansFound by workers: "+ plansFound.toString());
        System.out.println("Found new Plan! (id:"+id+") length: " + p.depth() + " Cost: "+p.cost());
        System.out.println(p);
        maxDepth.getAndUpdate(x->Math.min(p.depth()-1, x));

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
    private Transformation isTransformable(Goal goal, int depth) {
        assert !goal.isEmpty();
        for(Tuple<? extends Transformation, Goal> tuple : this.pairGenFactory.applyAllUnaryOpForwards(initialGoal, depth)){
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
