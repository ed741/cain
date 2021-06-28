package uk.co.edstow.cain.structures;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.regAlloc.RegisterAllocator;
import uk.co.edstow.cain.transformations.Transformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static uk.co.edstow.cain.fileRun.FileRun.printLnVerbose;

public class Plan<G extends Goal<G>, T extends Transformation<R>, R extends Register> {

    private final Step<G,T,R> step;
    private final Plan<G,T,R> previous;
    private final List<G> initialGoals;
    private final int depth;
    private final int[] depths;

    public Plan(List<G> finalGoals, List<G> initialGoals, T initialTransformation){
        GoalPair<G, T, R> p = new GoalPair<>(new ArrayList<>(), finalGoals, initialTransformation);

        this.step = new Step<>(p, new GoalBag<>(finalGoals), finalGoals, -1);
        this.previous = null;
        this.initialGoals = initialGoals;
        this.depth = 0;
        this.depths = new int[finalGoals.size()];
    }

    private Plan(Plan<G,T,R> previous, List<G> initialGoals, Step<G,T,R> step){
        this.previous = previous;
        this.initialGoals = initialGoals;
        this.step = step;
        this.depth = previous.depth + 1;
        this.depths = new int[step.currentGoals.size()];

        int d = 0;
        for (G upper : step.getUppers()) {
            for (int i = 0; i < previous.step.currentGoals.size(); i++) {
                if(upper.equivalent(previous.step.currentGoals.get(i))){
                    d = Math.max(d, previous.depths[i]+1);
                }
            }
        }
        for (int i = 0; i < step.currentGoals.size(); i++) {
            G g = step.currentGoals.get(i);
            for (int j = 0; j < previous.step.currentGoals.size(); j++) {
                if(g.equivalent(previous.step.currentGoals.get(j))){
                    this.depths[i] = previous.depths[j];
                    break;
                }
            }
            for (int j = 0; j < step.getLowers().size(); j++) {
                G l = step.getLowerTrueGoal(j);
                if(g.equivalent(l)){
                    this.depths[i] = Math.max(this.depths[i], d);
                }
            }

        }

    }

    public Plan<G,T,R> newAdd(GoalPair<G, T, R> newPair, GoalBag<G> currentGoals, List<G> translation, int child) {
        Step<G,T,R> newStep = new Step<>(newPair, currentGoals, translation, child);
        return new Plan<>(this, this.initialGoals, newStep);
    }

    public List<Step<G,T,R>> getAll() {
        List<Step<G,T,R>> steps = new ArrayList<>();
        Plan<G,T,R> c = this;
        while (c != null){
            steps.add(c.step);
            c = c.previous;
        }
        Collections.reverse(steps);
        return Collections.unmodifiableList(steps);
    }

    public String produceCode(RegisterAllocator.Mapping<G, R> registerMap) {
        List<Step<G,T,R>> all = getAll();
        StringBuilder sb = new StringBuilder();
        for (int i = all.size()-1; i >= 0; i--) {
            printLnVerbose("ProduceCode Step: %d", i);
            Step<G,T,R> step = all.get(i);
            List<R> uppers = new ArrayList<>();
            for (int j = 0; j < step.getUppers().size(); j++) {
                G upperGoal = step.getUppers().get(j);
                uppers.add(registerMap.get(upperGoal));
            }
            List<R> lowers = new ArrayList<>();
            for (int j = 0; j < step.getLowers().size(); j++) {
                G lowerGoal = step.getLowerTrueGoal(j);
                lowers.add(registerMap.get(lowerGoal));
            }
            sb.append(step.code(uppers, lowers, registerMap.getTrash(i)));
        }
        return sb.toString();
    }

    public Bounds bounds() {
        return new Bounds.SimpleBounds(getAll().stream().map(s->s.liveGoals().bounds()).collect(Collectors.toList()));
    }

    public static class Step<G extends Goal<G>, T extends Transformation<R>, R extends Register> {
        private final GoalPair<G, T, R> goalPair;
        private final GoalBag<G> currentGoals;
        private final List<G> translation;
        private final int child;

        private Step(GoalPair<G, T, R> t, GoalBag<G> currentGoals, List<G> translation, int child) {
            goalPair = t;
            this.currentGoals = new GoalBag<>(currentGoals);
            this.currentGoals.setImmutable();
            this.translation = translation;
            this.child = child;

        }

        @SuppressWarnings("unused")
        public GoalBag<G> liveGoals(){
            return currentGoals;
        }
        public List<G> getUppers(){
            return goalPair.getUppers();
        }
        public List<G> getLowers(){
            return goalPair.getLowers();
        }
        public T getTransformation(){
            return goalPair.getTransformation();
        }

        @Override
        public String toString() {
            return "Step<"+child+">{" +
                    "goalPair=" + goalPair.toString() +
                    '}';
        }

        private String toStringN() {
            return "Step<"+child+">:" + goalPair.toString() +
                    "\n";
        }

        private String toGoalsString(List<G> input) {

            boolean[] tops = new boolean[currentGoals.size()];
            boolean[] bottoms = new boolean[currentGoals.size()];
            for (int i = 0; i < currentGoals.size(); i++) {
                G currentGoal = currentGoals.get(i);
                boolean in = input.contains(currentGoal);
                boolean out = this.goalPair.getLowers().contains(currentGoal);
                tops[i] = out;
                bottoms[i] = in;
            }
            return currentGoals.toGoalsString(tops, bottoms, true, true);
        }

        @SuppressWarnings("WeakerAccess")
        public String code(List<R> uppers, List<R> lowers, List<R> trash) {
            return goalPair.getTransformation().code(uppers, lowers, trash);
        }

        public G getLowerTrueGoal(int i) {
            return translation.get(i);
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
        Plan<G,T,R> c = this;
        while (c != null){
            cost += c.step.getTransformation().cost();
            c = c.previous;
        }
        return cost;
    }

    public int depth(){
        return depth;
    }

    public int[] circuitDepths(){
        List<Step<G,T,R>> all = getAll();
        int[] depths = new int[all.get(0).getLowers().size()];
        for (int i = 0; i < depths.length; i++) {
            int[] depth = new int[all.size()];
            int start = -1;
            G lower = all.get(0).getLowerTrueGoal(i);
            for (int j = 1; start < 0 && j < all.size(); j++){
                for (G upper : all.get(j).getUppers()) {
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
                    for (G upper : all.get(j).getUppers()) {
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
        List<Step<G,T,R>> all = getAll();
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
        List<Step<G,T,R>> all = getAll();
        StringBuilder sb = new StringBuilder("plan:\n");
        for (int i = 0; i<all.size(); i++) {
            sb.append(i);
            sb.append(": ");
            sb.append(all.get(i).toStringN());
            sb.append("\n");
            List<G> input = new ArrayList<>();
            if(i+1<all.size()){
                input.addAll(all.get(i+1).getUppers());
            }
            sb.append(all.get(i).toGoalsString(input));
            sb.append('\n');
        }
        return sb.toString();
    }


}
