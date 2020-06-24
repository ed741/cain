package uk.co.edstow.cain.structures;

import uk.co.edstow.cain.RegisterAllocator;
import uk.co.edstow.cain.Transformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Plan {
    private static final ReentrantLock linkLock = new ReentrantLock();
    private static Plan linked = null;

    private final Step step;
    private final Plan previous;
    private final List<Goal> initialGoals;
    private final int depth;
    private final int[] depths;

    public Plan(List<Goal> finalGoals, List<Goal> initialGoals){
        GoalPair p = new GoalPair((Goal) null, finalGoals, new Transformation.Null(finalGoals.size(), 0));
        Goal[] translation  = new Goal[finalGoals.size()];
        for (int i = 0; i < translation.length; i++) {
            translation[i] = finalGoals.get(i);
        }

        this.step = new Step(p, new GoalBag(finalGoals), translation, -1);
        this.previous = null;
        this.initialGoals = initialGoals;
        this.depth = 0;
        this.depths = new int[finalGoals.size()];
    }

    private Plan(Plan previous, List<Goal> initialGoals, Step step){
        this.previous = previous;
        this.initialGoals = initialGoals;
        this.step = step;
        this.depth = previous.depth + 1;
        this.depths = new int[step.currentGoals.size()];

        int d = 0;
        for (Goal upper : step.getUppers()) {
            for (int i = 0; i < previous.step.currentGoals.size(); i++) {
                if(upper.equivalent(previous.step.currentGoals.get(i))){
                    d = Math.max(d, previous.depths[i]+1);
                }
            }
        }
        for (int i = 0; i < step.currentGoals.size(); i++) {
            Goal g = step.currentGoals.get(i);
            for (int j = 0; j < previous.step.currentGoals.size(); j++) {
                if(g.equivalent(previous.step.currentGoals.get(j))){
                    this.depths[i] = previous.depths[j];
                    break;
                }
            }
            for (int j = 0; j < step.getLowers().size(); j++) {
                Goal l = step.getLowerTrueGoal(j);
                if(g.equivalent(l)){
                    this.depths[i] = Math.max(this.depths[i], d);
                }
            }

        }

    }

    public Plan newAdd(GoalPair newPair, GoalBag currentGoals, Goal[] translation, int child) {
        Step newStep = new Step(newPair, currentGoals, translation, child);
        return new Plan(this, this.initialGoals, newStep);
    }

    public List<Step> getAll() {
        List<Step> steps = new ArrayList<>();
        Plan c = this;
        while (c != null){
            steps.add(c.step);
            c = c.previous;
        }
        Collections.reverse(steps);
        return Collections.unmodifiableList(steps);
    }

    public String produceCode(RegisterAllocator.Mapping registerMap) {
        List<Step> all = getAll();
        StringBuilder sb = new StringBuilder("//Kernel Code!\n");
        sb.append("//Inputs in: ").append(Arrays.toString(registerMap.initRegisters())).append("\n");
        for (int i = all.size()-1; i >= 0; i--) {
            Step step = all.get(i);
            List<RegisterAllocator.Register> uppers = new ArrayList<>();
            for (int j = 0; j < step.getUppers().size(); j++) {
                Goal upperGoal = step.getUppers().get(j);
                uppers.add(registerMap.get(upperGoal));
            }
            List<RegisterAllocator.Register> lowers = new ArrayList<>();
            for (int j = 0; j < step.getLowers().size(); j++) {
                Goal lowerGoal = step.getLowerTrueGoal(j);
                lowers.add(registerMap.get(lowerGoal));
            }
            sb.append(step.code(uppers, lowers, registerMap.getTrash(i)));
//            sb.append("//").append(registerMap.getTrash(i));
            sb.append("\n");
        }
        return sb.toString();
    }

    public static class Step {
        private final GoalPair goalPair;
        private final GoalBag currentGoals;
        private final Goal[] translation;
        private final int child;
        private int idx;
        private List<Step> forwardsLinks;
        private List<Step> backwardsLinks;

        private Step(GoalPair t, GoalBag currentGoals, Goal[] translation, int child) {
            goalPair = t;
            this.currentGoals = new GoalBag(currentGoals);
            this.translation = translation;
            this.child = child;

        }

        @SuppressWarnings("unused")
        public GoalBag liveGoals(){
            return currentGoals;
        }
        public List<Goal> getUppers(){
            return goalPair.getUppers();
        }
        public List<Goal> getLowers(){
            return goalPair.getLowers();
        }
        public Transformation getTransformation(){
            return goalPair.getTransformation();
        }

        public int getSearchOrderIdx() {
            return child;
        }

        @Override
        public String toString() {
            List<String> forward = forwardsLinks==null?new ArrayList<>():forwardsLinks.stream().map(s->String.valueOf(s.idx)).collect(Collectors.toList());
            List<String> backward = backwardsLinks==null?new ArrayList<>():backwardsLinks.stream().map(s->String.valueOf(s.idx)).collect(Collectors.toList());
            return "Step("+ idx +")"+forward+""+backward+"<"+child+">{" +
                    "goalPair=" + goalPair.toString() +
                    '}';
        }

        private String toStringN() {
            return idx +" " + goalPair.toStringN() +
                    "\n";
        }

        private String toGoalsString(List<Goal> input) {

            boolean[] tops = new boolean[currentGoals.size()];
            boolean[] bottoms = new boolean[currentGoals.size()];
            for (int i = 0; i < currentGoals.size(); i++) {
                Goal currentGoal = currentGoals.get(i);
                boolean in = input.contains(currentGoal);
                boolean out = this.goalPair.getLowers().contains(currentGoal);
                tops[i] = out;
                bottoms[i] = in;
            }
            return GoalBag.toGoalsString(currentGoals, tops, bottoms, true, true);
        }

        @SuppressWarnings("WeakerAccess")
        public String code(List<RegisterAllocator.Register> uppers, List<RegisterAllocator.Register> lowers,  List<RegisterAllocator.Register> trash) {
            return goalPair.getTransformation().code(uppers, lowers, trash);
        }

        public Goal getLowerTrueGoal(int i) {
            return translation[i];
        }
    }

    public int maxCircuitDepth(){
        int max = 0;
        for (int i : this.depths) {
            max = Math.max(max, i);
        }
        return max;
    }

    public double totalInstructionCost(){
        double cost = 0;
        Plan c = this;
        while (c != null){
            cost += c.step.getTransformation().cost();
            c = c.previous;
        }
        return cost;
    }

    public int depth(){
        return depth;
    }

    @SuppressWarnings("WeakerAccess")
    public <T> T link(Supplier<T> action){
        try {
            linkLock.lock();
            doLinking();
            return action == null? null:action.get();
        } finally {
            linkLock.unlock();

        }
    }


    private void doLinking(){
        assert linkLock.isHeldByCurrentThread();
        if(linked == this){
            return;
        }
        List<Step> all = getAll();
        for (int i = 0; i < all.size(); i++) {
            Step s = all.get(i);
            s.backwardsLinks = new ArrayList<>();
            s.forwardsLinks = new ArrayList<>();
            s.idx = i;
        }
        for (int i = 0; i < all.size(); i++) {
            Step step = all.get(i);
            List<Goal> lowers = step.getLowers();
            for (int l = 0; l < lowers.size(); l++) {
                Goal lower = step.getLowerTrueGoal(l);
                int j = i + 1;
                jloop:
                for (; j < all.size(); j++) {
                    for (Goal upper : all.get(j).getUppers()) {
                        if (upper.equivalent(lower)) {
                            step.backwardsLinks.add(all.get(j));
                            all.get(j).forwardsLinks.add(step);
                            break jloop;
                        }
                    }
                }
                if(!(j != all.size() || initialGoals.contains(lower))) {
                    assert j != all.size() || initialGoals.contains(lower);
                }

            }
        }
        linked = this;
    }

    public int[] circuitDepths(){
        List<Step> all = getAll();
        int[] depths = new int[all.get(0).getLowers().size()];
        for (int i = 0; i < depths.length; i++) {
            int[] depth = new int[all.size()];
            int start = -1;
            Goal lower = all.get(0).getLowerTrueGoal(i);
            for (int j = 1; start < 0 && j < all.size(); j++){
                for (Goal upper : all.get(j).getUppers()) {
                    if(lower.equivalent(upper)){
                        start = j;
                        break;
                    }
                }
            }
            if(start>=0) {
                depth[start] = 1;
                for (int j = start + 1; j < all.size(); j++) {
                    int max = Integer.MIN_VALUE;
                    for (Goal upper : all.get(j).getUppers()) {
                        for (int k = j - 1; k >= start; k--) {
                            for (int l = 0; l < all.get(k).getLowers().size(); l++) {
                                if (upper.equivalent(all.get(k).getLowerTrueGoal(l))) {
                                    max = Math.max(max, 1 + depth[k]);
                                }
                            }
                        }
                    }
                    depth[j] = max;
                }

                for (int j = depth.length - 1; j >= 0; j--) {
                    if (depth[j] >= 0) {
                        depths[i] = depth[j];
                        break;
                    }
                }
            }
//            System.out.println("Depth of filter " + i + " is: " + depths[i]);
        }
        return depths;
    }


    @Override
    public String toString() {
        List<Step> all = getAll();
        StringBuilder sb = new StringBuilder("plan:\n");
        for (int i = 0; i<all.size(); i++) {
            sb.append(i);
            sb.append(": ");
            sb.append(all.get(i).toString());
            sb.append('\n');
        }
        return sb.toString();
    }

    public String toGoalsString(){
        List<Step> all = getAll();
        StringBuilder sb = new StringBuilder("plan:\n");
        for (int i = 0; i<all.size(); i++) {
            sb.append(i);
            sb.append(": ");
            sb.append(all.get(i).toStringN());
            sb.append("\n");
            List<Goal> input = new ArrayList<>();
            if(i+1<all.size()){
                input.addAll(all.get(i+1).getUppers());
            }
            sb.append(all.get(i).toGoalsString(input));
            sb.append('\n');
        }
        return sb.toString();
    }


}
