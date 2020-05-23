package uk.co.edstow.cpacgen;

import uk.co.edstow.cpacgen.util.Bounds;

import java.util.ArrayList;
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

    public Plan(List<Goal> finalGoals, List<Goal> initialGoals, String comment){
        Goal.Pair p = new Goal.Pair(null, finalGoals, new Transformation.Null(finalGoals.size()));
        Goal[] translation  = new Goal[finalGoals.size()];
        for (int i = 0; i < translation.length; i++) {
            translation[i] = finalGoals.get(i);
        }

        this.step = new Step(p, new Goal.Bag(finalGoals), translation, comment);
        this.previous = null;
        this.initialGoals = initialGoals;
        this.depth = 0;
    }

    private Plan(Plan previous, List<Goal> initialGoals, Step step){
        this.previous = previous;
        this.initialGoals = initialGoals;
        this.step = step;
        this.depth = previous.depth + 1;
    }

    public Plan newAdd(Goal.Pair newPair, Goal.Bag currentGoals, Goal[] translation, String comment) {
        Step newStep = new Step(newPair, currentGoals, translation, comment);
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
        for (int i = all.size()-1; i >= 0; i--) {
            Step step = all.get(i);
            RegisterAllocator.Register upper = step.getUpper()!=null?registerMap.get(step.getUpper()):null;
            List<RegisterAllocator.Register> lowers = new ArrayList<>();
            for (int j = 0; j < step.getLowers().size(); j++) {
                Goal lowerGoal = step.getLowerTrueGoal(j);
                lowers.add(registerMap.get(lowerGoal));
            }
            sb.append(step.code(upper, lowers));
            sb.append("\n");
        }
        return sb.toString();
    }

    public static class Step {
        private final String comment;
        private final Goal.Pair goalPair;
        private final Goal.Bag currentGoals;
        private final Goal[] translation;
        private int idx;
        private List<Step> forwardsLinks;
        private List<Step> backwardsLinks;

        private Step(Goal.Pair t, Goal.Bag currentGoals, Goal[] translation, String comment) {
            goalPair = t;
            this.comment = comment;
            this.currentGoals = new Goal.Bag(currentGoals);
            this.translation = translation;
        }

        @SuppressWarnings("unused")
        public Goal.Bag liveGoals(){
            return currentGoals;
        }
        public Goal getUpper(){
            return goalPair.getUpper();
        }
        public List<Goal> getLowers(){
            return goalPair.getLowers();
        }
        public Transformation getTransformation(){
            return goalPair.getTransformation();
        }


        @Override
        public String toString() {
            List<String> forward = forwardsLinks==null?new ArrayList<>():forwardsLinks.stream().map(s->String.valueOf(s.idx)).collect(Collectors.toList());
            List<String> backward = backwardsLinks==null?new ArrayList<>():backwardsLinks.stream().map(s->String.valueOf(s.idx)).collect(Collectors.toList());
            return "Step("+ idx +")"+forward+""+backward+"{" +
                    "goalPair=" + goalPair.toString() +
                    ", " + comment +
                    '}';
        }

        private String toStringN() {
            return idx +" " + goalPair.toStringN() +
                    "\n" + comment;
        }

        private String toGoalsString(List<Goal> input) {

            Bounds b = new Bounds(new Bounds(currentGoals), new Atom(0,0,0, true));
            int height = 1 + b.yMax - b.yMin;
            int width = 1 + b.xMax - b.xMin;
            List<String[][]> arrays = new ArrayList<>();

            for (Goal currentGoal : currentGoals) {
                boolean in = input.contains(currentGoal);
                boolean out = this.goalPair.getLowers().contains(currentGoal);
                String[][] tableArray = currentGoal.getCharTable(b, width, height, out, in, true, true);
                arrays.add(tableArray);
            }

            StringBuilder sb = new StringBuilder();
            for (int j = height+1; j >= 0; j--) {
                for (String[][] array : arrays) {
                    for (int i = 0; i < array[j].length; i++) {
                        sb.append(array[j][i]);
                    }
                    sb.append(' ');
                }
                sb.append("\n");
            }
            return sb.toString();
        }

        @SuppressWarnings("WeakerAccess")
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            return goalPair.getTransformation().code(upper, lowers);
        }

        public Goal getLowerTrueGoal(int i) {
            return translation[i];
        }
    }

    public double cost(){
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
            for (Goal lower : step.getLowers()) {
                int j = i + 1;
                for (; j < all.size(); j++) {
                    if (all.get(j).getUpper().same(lower)) {
                        step.backwardsLinks.add(all.get(j));
                        all.get(j).forwardsLinks.add(step);
                        break;
                    }
                }
                assert j != all.size() || initialGoals.contains(lower);

            }
        }
        linked = this;
    }

    public int[] circuitDepths(){
        List<Step> all = getAll();
        return link(()->{
            int[] depths = new int[all.get(0).getLowers().size()];
            for (int i = 0; i < depths.length; i++) {
                int[] depth = new int[all.size()];
                for (int k = 0; k < depth.length; k++) {
                    depth[k] = -1;
                }

                depth[all.get(0).backwardsLinks.get(i).idx] = 0;
                for (int j = all.get(0).backwardsLinks.get(i).idx + 1; j < all.size(); j++) {
                    int max = Integer.MIN_VALUE;
                    for (Step forwardsLink : all.get(j).forwardsLinks) {
                        if (depth[forwardsLink.idx] >= 0) {
                            max = Math.max(max, 1 + depth[forwardsLink.idx]);
                        }
                    }
                    depth[j] = max;
                }
                System.out.println("Depth of filter " + i + " is: " + depth[depth.length - 1]);
                depths[i] = depth[depth.length-1];
            }
            return depths;
        });
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
                input.add(all.get(i+1).getUpper());
            }
            sb.append(all.get(i).toGoalsString(input));
            sb.append('\n');
        }
        return sb.toString();
    }


}
