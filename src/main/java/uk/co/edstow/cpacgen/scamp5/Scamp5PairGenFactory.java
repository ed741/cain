package uk.co.edstow.cpacgen.scamp5;

import uk.co.edstow.cpacgen.Atom;
import uk.co.edstow.cpacgen.Goal;
import uk.co.edstow.cpacgen.ReverseSearch;
import uk.co.edstow.cpacgen.Transformation;
import uk.co.edstow.cpacgen.pairgen.Distance;
import uk.co.edstow.cpacgen.pairgen.PairGenFactory;
import uk.co.edstow.cpacgen.util.Bounds;
import uk.co.edstow.cpacgen.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.co.edstow.cpacgen.scamp5.Scamp5Transformation.*;

public class Scamp5PairGenFactory implements PairGenFactory {


    public static class Config {
        private final SearchStrategy strategy;

        boolean useMov2x;
        boolean useAdd3;

        boolean useAddx;
        boolean useAdd2x;
        boolean useSubx;
        boolean useSub2x;

        boolean subPowerOf2;

        final int availableRegisters;

        final int depth;

        public enum SearchStrategy {
            Exhaustive, AtomDistance, SortedAtomDistance
        }

        public Config(SearchStrategy strategy, int availableRegisters, int depth) {
            this.strategy = strategy;
            this.availableRegisters = availableRegisters;
            this.depth = depth;
        }

        public void useAll(){
            this.useMov2x = true;
            this.useAdd3 = true;
            this.useAddx = true;
            this.useAdd2x = true;
            this.useSubx = true;
            this.useSub2x = true;
        }

        public void useBasicOps(){
            this.useMov2x = false;
            this.useAdd3 = false;
            this.useAddx = false;
            this.useAdd2x = false;
            this.useSubx = false;
            this.useSub2x = false;
        }

        public void useSubPowerOf2(){
            this.subPowerOf2 = true;
        }

        public Config(SearchStrategy strategy, boolean useMov2x, boolean useAdd3, boolean useAddx, boolean useAdd2x, boolean useSubx, boolean useSub2x, int availableRegisters, int depth) {
            this.strategy = strategy;

            this.useMov2x = useMov2x;
            this.useAdd3 = useAdd3;
            this.useAddx = useAddx;
            this.useAdd2x = useAdd2x;
            this.useSubx = useSubx;
            this.useSub2x = useSub2x;

            this.depth = depth;
            this.availableRegisters = availableRegisters;
        }

    }

    public interface ConfigGetter {
        Config getConfig(List<Goal> goals, int depth, ReverseSearch rs, boolean initialGoal);
    }

    @Override
    public Collection<Tuple<List<Goal.Pair>, Goal>> applyAllUnaryOpForwards(List<Goal> initialGoals, int depth, Goal goal) {
        Config conf = confGet.getConfig(initialGoals, depth, rs, true);
        ArrayList<Tuple<List<Goal.Pair>, Goal>> list = new ArrayList<>();

        //Negate
        Neg neg = new Neg(goal, true);
        if(initialGoals.contains(neg.a)) {
            list.add(new Tuple<>(Collections.singletonList(new Goal.Pair(neg.applyForwards(), neg.a, neg)), neg.a));
        }
        //Divide
        try{
            if(goal.countUnique()==1 && goal.get(0).x==0 && goal.get(0).y==0 && goal.get(0).positive) {
                int z = goal.get(0).z;
                Goal ic = null;
                for (Goal i : initialGoals) {
                    if(i.contains(goal.get(0))){
                        ic = i;
                    }
                }
                if(ic != null) {
                    List<Goal.Pair> pairs = new ArrayList<>();
                    Divq div = new Divq(goal, true);
                    pairs.add(new Goal.Pair(div.applyForwards(), Collections.singletonList(div.a), div));
                    while (div.a.atomCount() < ic.atomCount()){
                        div = new Divq(div.a, true);
                        pairs.add(new Goal.Pair(div.applyForwards(), Collections.singletonList(div.a), div));
                    }
                    if(div.a.equals(ic)){
                        list.add(new Tuple<>(pairs, ic));
                    }
                }
            }

        } catch (Transformation.TransformationApplicationException e) {
            e.printStackTrace();
            // Should be unreachable
            assert false;
        }

        //Move x
        for (Dir dir: Dir.values()) {
            Movx movx = new Movx(goal, dir, true);
            if(initialGoals.contains(movx.a)) {
                list.add(new Tuple<>(Collections.singletonList(new Goal.Pair(goal, movx.a, movx)), movx.a));
            }
        }

        //Move 2x
        if(conf.useMov2x) {
            for (Dir dir1 : Dir.values()) {
                Mov2x mov2xa = new Mov2x(goal, dir1, dir1, true);
                if(initialGoals.contains(mov2xa.a)) {
                    list.add(new Tuple<>(Collections.singletonList(new Goal.Pair(goal, mov2xa.a, mov2xa)), mov2xa.a));
                }
                Mov2x mov2xb = new Mov2x(goal, dir1, dir1.cw(), true);
                if(initialGoals.contains(mov2xb.a)) {
                    list.add(new Tuple<>(Collections.singletonList(new Goal.Pair(goal, mov2xb.a, mov2xb)), mov2xb.a));
                }
            }
        }

        return list;
    }


    private ReverseSearch rs;
    private final ConfigGetter confGet;
    private List<Goal> initialGoals;



    public Scamp5PairGenFactory(ConfigGetter confGet) {
        this.confGet = confGet;
    }
    private int[] initialDivisions;
    private int initialDivisionsMax;
    private int initialDivisionsMin;
    @Override
    public void init(ReverseSearch rs) {
        this.initialDivisions = rs.getInitialDivisions();
        this.initialDivisionsMax = Arrays.stream(this.initialDivisions).max().getAsInt();
        this.initialDivisionsMin = Arrays.stream(this.initialDivisions).min().getAsInt();
        this.rs =rs;
        this.initialGoals = rs.getInitialGoals();
    }



    @Override
    public PairGen generatePairs(Goal.Bag goals, int depth) {
       Config conf = confGet.getConfig(goals, depth, rs, false);
       switch (conf.strategy){
           case Exhaustive:
               return new ExhaustivePairGen(goals, conf);
           case AtomDistance:
               return new AtomDistancePairGen(goals, conf);
           case SortedAtomDistance:
               return new AtomDistanceSortedPairGen(goals, conf);
           default:
               throw new IllegalArgumentException();
       }
    }

    private class ExhaustivePairGen implements PairGen{
        private final Config conf;
        private final Iterator<Goal.Pair> it;
        private final Goal.Bag goals;
        private final Bounds bounds;


        private ExhaustivePairGen(Goal.Bag goals, Config conf) {
            this.goals = goals;
            this.conf = conf;
            this.bounds = new Bounds(goals);
            Comparator<Tuple<Goal.Pair, Double>> comparator = Comparator.comparingDouble(Tuple::getB);
            this.it = goals.parallelStream()
                    .flatMap((Goal upper) ->
                            Stream.concat(
                                    getNaryOpStream(upper),
                                    getUnaryOpStream(upper)
                            )
                    ).map(pair -> new Tuple<>(pair, getCost(pair, goals, conf))).filter(t -> t.getB()>=0)
                    .sorted(comparator)
                    .map(Tuple::getA)
                    .iterator();
//            List<Tuple<Goal.Pair, Double>> list = goals.parallelStream()
//                    .flatMap((Goal upper) ->
//                            Stream.concat(
//                                    getNaryOpStream(upper),
//                                    getUnaryOpStream(upper)
//                            )
//                    ).map(pair -> new Tuple<>(pair, getCost(pair, goals, conf))).filter(t -> t.getB()>=0)
//                    .sorted(comparator).collect(Collectors.toList());
//            this.it = list.stream().map(Tuple::getA).iterator();
        }

        @Override
        public Goal.Pair next() {
            return it.hasNext()?it.next():null;
        }


        private Stream<Goal.Pair> getUnaryOpStream(Goal upper) {
            ArrayList<Goal.Pair> pairs = new ArrayList<>();

            //Negate
            Neg neg = new Neg(upper, true);
            pairs.add(new Goal.Pair(upper, neg.a, neg));

            //Divide
            Divq div = new Divq(upper, true);
            pairs.add(new Goal.Pair(upper, div.a, div));

            //Mov
            Mov mov = new Mov(upper, true);
            pairs.add(new Goal.Pair(upper, mov.a, mov));

            //Move x
            for (Dir dir: Dir.values()) {
                Movx movx = new Movx(upper, dir, true);
                pairs.add(new Goal.Pair(upper, movx.a, movx));
            }

            if(conf.useMov2x) {
                for (Dir dir1 : Dir.values()) {
                    Mov2x mov2xa = new Mov2x(upper, dir1, dir1, true);
                    pairs.add(new Goal.Pair(upper, mov2xa.a, mov2xa));
                    Mov2x mov2xb = new Mov2x(upper, dir1, dir1.cw(), true);
                    pairs.add(new Goal.Pair(upper, mov2xb.a, mov2xb));
                }
            }

            return pairs.stream();
        }

        private Stream<Goal.Pair> getNaryOpStream(Goal upper) {
            ArrayList<Goal.Pair> pairs = new ArrayList<>();
            List<Goal> splits = upper.allSplits();
            int normal = splits.size();
            if(conf.subPowerOf2){
                Goal.Factory sub1 = new Goal.Factory();
                Goal.Factory sub2 = new Goal.Factory();
                boolean tryPower = false;
                Iterator<Tuple<Atom, Integer>> it = upper.uniqueCountIterator();
                while(it.hasNext()){
                    Tuple<Atom, Integer> t = it.next();
                    if(t.getB() == 7 || t.getB() == 14 || t.getB() == 15){
                        tryPower = true;
                        sub1.add(t.getA().negate(), 2-(t.getB() & 1));
                        sub2.add(t.getA(), t.getB()<<1 & ~t.getB());
                    } else {
                        sub1.add(t.getA(), t.getB());
                    }
                }
                if(tryPower){
                    splits.add(sub1.get());
                    splits.add(sub2.get());
                }
            }
            Set<Goal> seen = new HashSet<>();
            for (int i = 0; i < splits.size(); i++) {
                Goal a = splits.get(i);
                boolean skipAdd_2 = false;
                if (seen.contains(a)) {
                    skipAdd_2 = true;
                }
                Goal b;
                if(i<normal){
                    b = upper.without(a);
                } else {
                    b = upper.subtract(a);
                }
                seen.add(b);

                if (a.isEmpty() || b.isEmpty()) {
                    continue;
                }
                if (!skipAdd_2) {
                    Add_2 add = new Add_2(a, b);
                    pairs.add(new Goal.Pair(upper, Arrays.asList(a, b), add));
                    if (conf.useAddx) {
                        for (Dir dir : Dir.values()) {
                            Goal movA = a.translated(-dir.x, -dir.y, 0);
                            Goal movB = b.translated(-dir.x, -dir.y, 0);
                            Addx addx = new Addx(movA, movB, dir);
                            pairs.add(new Goal.Pair(upper, Arrays.asList(movA, movB), addx));
                        }
                    }
                    if (conf.useAdd2x) {
                        for (Dir dir1 : Dir.values()) {
                            Goal aMovA = a.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                            Goal bMovA = b.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                            Add2x add2xa = new Add2x(aMovA, bMovA, dir1, dir1);
                            pairs.add(new Goal.Pair(upper, Arrays.asList(aMovA, bMovA), add2xa));
                            Dir dir2 = dir1.cw();
                            Goal aMovB = a.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                            Goal bMovB = b.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                            Add2x add2xb = new Add2x(aMovB, bMovB, dir1, dir2);
                            pairs.add(new Goal.Pair(upper, Arrays.asList(aMovB, bMovB), add2xb));
                        }
                    }
                }

                Goal negB = b.negative();
                Sub sub = new Sub(a, negB);
                pairs.add(new Goal.Pair(upper, Arrays.asList(a, negB), sub));
                if (conf.useSubx) {
                    for (Dir dir : Dir.values()) {
                        Goal movA = a.translated(-dir.x, -dir.y, 0);
                        Subx subx = new Subx(movA, negB, dir);
                        pairs.add(new Goal.Pair(upper, Arrays.asList(movA, negB), subx));
                    }
                }
                if (conf.useSub2x) {
                    for (Dir dir1 : Dir.values()) {
                        Goal aMovA = a.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                        Sub2x sub2xa = new Sub2x(aMovA, negB, dir1, dir1);
                        pairs.add(new Goal.Pair(upper, Arrays.asList(aMovA, negB), sub2xa));
                        Dir dir2 = dir1.cw();
                        Goal aMovB = a.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                        Sub2x sub2xb = new Sub2x(aMovB, negB, dir1, dir2);
                        pairs.add(new Goal.Pair(upper, Arrays.asList(aMovB, negB), sub2xb));
                    }
                }

                if (conf.useAdd3) {
                    Set<Goal> subSeen = new HashSet<>();

                    Collection<Goal> subSplits = a.allSplits();
                    for (Goal aa : subSplits) {
                        boolean skipAdd_3 = false;
                        if (subSeen.contains(aa)) {
                            skipAdd_3 = true;
                        }
                        Goal ab = a.without(aa);
                        subSeen.add(ab);
                        if (aa.isEmpty() || ab.isEmpty()) {
                            continue;
                        }
                        if (!skipAdd_3) {
                            Add_3 add = new Add_3(aa, ab, b);
                            pairs.add(new Goal.Pair(upper, Arrays.asList(aa, ab, b), add));
                        }
                    }
                }
            }

            return pairs.stream();
        }
    }

    private class AtomDistanceListItem {
        Goal.Pair pair;
        double cost;
        Goal a;
        Goal b;
        Distance distance;
        boolean negate;
        Goal to;

        AtomDistanceListItem() {
        }
        
        AtomDistanceListItem(AtomDistanceListItem item) {
            this.pair = item.pair;
            this.cost = item.cost;
            this.a = item.a;
            this.b = item.b;
            this.distance = item.distance;
            this.negate = item.negate;
            this.to = item.to;

        }
    }

    private class AtomDistancePairGen implements PairGen {
        final Config conf;
        final Goal.Bag goals;
        final Iterator<Tuple<Integer, Integer>> ijGetter;

        List<Goal.Pair> currentList = new ArrayList<>();

        private AtomDistancePairGen(Goal.Bag goals, Config conf) {
            this.goals = goals;
            this.conf = conf;
            this.ijGetter = new SteppedCombinationIterator(goals.size());
        }

        private AtomDistancePairGen(Goal.Bag goals, Config conf, Iterator<Tuple<Integer, Integer>> ijGetter) {
            this.goals = goals;
            this.conf = conf;
            this.ijGetter = ijGetter;
        }

        @SuppressWarnings("WeakerAccess")
        protected void fillCurrentList(){
            while (currentList.isEmpty()){
                if(!ijGetter.hasNext()){
                    return;
                }
                Tuple<Integer, Integer> ij = ijGetter.next();
                Goal a = goals.get(ij.getA());
                Goal b = goals.get(ij.getB());

                boolean diagonal = ij.getA().equals(ij.getB());
                List<AtomDistanceListItem> inList = getAtomDistanceList(a, b, diagonal);
                List<AtomDistanceListItem> outList = new ArrayList<>();
                inList.sort(atomDistanceComparator);
                addPairs(a, diagonal, inList, outList);
                outList.forEach(item -> currentList.add(item.pair));

            }
        }
        @SuppressWarnings("WeakerAccess")
        protected void addPairs(Goal a, boolean diagonal, List<AtomDistanceListItem> inList, List<AtomDistanceListItem> outList) {
            if(!diagonal) {
                for (AtomDistanceListItem item : inList) {
                    addAtomDistancePairs(item, conf, outList);
                }
            } else {
                // diagonal == True
                if(goals.size() < conf.availableRegisters) {
                    addDirectMov(a, outList);
                }

                for (AtomDistanceListItem item : inList) {
                    addAtomDistanceDiagonalPairs(item, conf, outList);
                }
                for(Goal initialGoal: initialGoals) {
                    if (initialGoal.hasSubGoal(a)) {
                        Goal l = new Goal.Factory(a).addAll(a).get();
                        AtomDistanceListItem newItem = new AtomDistanceListItem();
                        newItem.a = a;
                        newItem.distance = new Distance(0, 0, 0);
                        newItem.pair = new Goal.Pair(a, l, new Divq(l));
                        outList.add(newItem);
                    }
                }
            }
        }

        @SuppressWarnings("WeakerAccess")
        protected void addDirectMov(Goal a, List<AtomDistanceListItem> outList) {
            Distance centre = new Distance(a.getAveragePos());
            if(centre.manhattanXY()>0){
                Transformation.Direction d1 = centre.majorXYDirection();
                if(d1!= null) {
                    Dir dir1 = Dir.fromDirection(d1);
                    Movx movx = new Movx(a, dir1, true);
                    AtomDistanceListItem newItem = new AtomDistanceListItem();
                    newItem.a = a;
                    newItem.distance = new Distance(d1, 1);
                    newItem.pair = new Goal.Pair(a, movx.a, movx);
                    outList.add(newItem);
                }
            }
            if(conf.useMov2x && centre.manhattanXY()>1){
                Transformation.Direction d1 = centre.majorXYDirection();
                if(d1 != null) {
                    Transformation.Direction d2 = centre.then(d1.opposite()).majorXYDirection();
                    if (d2 != null) {
                        Dir dir1 = Dir.fromDirection(d1).opposite();
                        Dir dir2 = Dir.fromDirection(d2).opposite();
                        Mov2x mov2x = new Mov2x(a, dir1, dir2, true);
                        AtomDistanceListItem newItem = new AtomDistanceListItem();
                        newItem.a = a;
                        newItem.distance = new Distance(d1, 1).then(d2);
                        newItem.pair = new Goal.Pair(a, mov2x.a, mov2x);
                        outList.add(newItem);
                    }
                }
            }
        }

        @Override
        public Goal.Pair next() {
            fillCurrentList();
            return  currentList.isEmpty()? null:currentList.remove(currentList.size()-1);
        }
    }

    private class AtomDistanceSortedPairGen extends AtomDistancePairGen {

        private final Bounds bounds;

        private AtomDistanceSortedPairGen(Goal.Bag goals, Config conf) {
            super(goals, conf, new PlainCombinationIterator(goals.size()));
            this.bounds = new Bounds(goals);

        }

        @Override
        protected void fillCurrentList() {
            if(!currentList.isEmpty()){
                return;
            }
            List<AtomDistanceListItem> outList = new ArrayList<>();
            while (ijGetter.hasNext()){
                Tuple<Integer, Integer> ij = ijGetter.next();
                Goal a = goals.get(ij.getA());
                Goal b = goals.get(ij.getB());
                boolean diagonal = ij.getA().equals(ij.getB());
                List<AtomDistanceListItem> inList = getAtomDistanceList(a, b, diagonal);
                inList.sort(atomDistanceComparator);
                addPairs(a, diagonal, inList, outList);
            }
            outList.parallelStream().forEach(item -> item.cost = getCost(item.pair, goals, conf));
            outList.removeIf(item -> item.cost < 0);
            outList.sort(Comparator.comparingDouble((AtomDistanceListItem item) -> item.cost).reversed());
            currentList = outList.stream().map(item -> item.pair).collect(Collectors.toList());
        }
    }

    private static final Comparator<AtomDistanceListItem> atomDistanceComparator = Comparator.comparingInt((AtomDistanceListItem i) -> i.to.size()).thenComparingInt(i -> -i.distance.manhattanXY());

    private double getCost(Goal.Pair pair, Goal.Bag goals, Config config) {
        Goal.Bag proposedGoals = new Goal.Bag(goals);
        proposedGoals.remove(pair.getUpper());

        List<Goal> toAdd = new ArrayList<>();
        for (Goal goal : pair.getLowers()) {
            proposedGoals.remove(goal);
            toAdd.add(goal);
        }
        proposedGoals.addAll(toAdd);
        if(proposedGoals.size() +(pair.getTransformation().inputRegisterOutputInterferes()?1:0) > config.availableRegisters){
            return -1; // exit early if too many registers are used.
        }
        double cost = 0;
//        cost += Math.pow(proposedGoals.size(), (5-Math.min(5, config.availableRegisters-proposedGoals.size())));
        for (Goal g : proposedGoals) {
            int subset = 0;
            for(Goal g2 : proposedGoals){
                if(g2.hasSubGoal(g)){
                    subset++;
                }
                if(subset > 1){
                    break;
                }
            }
            double atomDistanceCost = 0;
            for (Atom a : g) {
                cost += Math.abs(a.x) + Math.abs(a.y) + Math.abs(a.z) + (a.positive?0:1);
            }
            cost += atomDistanceCost/subset;
        }

        int min = Integer.MAX_VALUE;
        int max = 0;

        List<Goal> goalList = new ArrayList<>(proposedGoals);
        for (int i = 0; i < goalList.size(); i++) {
            Goal goal = goalList.get(i);
            List<Goal> toRemove = patternRepeated(goalList, goal);
            goalList.removeAll(toRemove);
            if(i > goalList.size()){
                System.out.println("I " + i);
                System.out.println("Goal " + goal);
                System.out.println("Goal List " + goalList);
                System.out.println("toRemove " + toRemove);
                System.out.println("GoalSet " + proposedGoals);
                System.exit(-1);
            }
            goalList.add(i, goal);
            cost += toRemove.size()*goal.atomCount();
            if(!goal.allSame()) {
                cost += Math.pow(goal.atomCount(), 2);
            }

            if(min>1){
                min = Math.min(min, goal.minimumCount());
            }
            max = Math.max(max, goal.maximumCount());
        }

        cost += (1<<initialDivisionsMax) / min;
        cost += max / (1<<initialDivisionsMin);

        return cost;
    }

    private List<Goal> patternRepeated(Collection<Goal> goals, Goal pattern){
        List<Goal> matches = new ArrayList<>();
        for (Goal goal : goals) {
            if (pattern.equivalent(goal)) {
                matches.add(goal);
            } else if (goal.atomCount() == pattern.atomCount()){
                if(pattern.atomCount() == 0){
                    matches.add(goal);
                } else {
                    if (pattern.get(0).positive == goal.get(0).positive) {
                        Distance d = new Distance(pattern.get(0), goal.get(0));
                        int i = 0;
                        for (; i < pattern.size(); i++) {
                            if (pattern.get(i).positive != goal.get(i).positive || !d.same(pattern.get(i), goal.get(i))) {
                                break;
                            }
                        }
                        if (i == pattern.size()) {
                            matches.add(goal);
                        }
                    }
                }
            }

        }
        return matches;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private void addAtomDistanceDiagonalPairs(AtomDistanceListItem item,
                                              Config conf, List<AtomDistanceListItem> outList) {
        Distance centre = new Distance(item.a.getAveragePos());
        Goal aWithoutTo = item.a.without(item.to);
        //add_2, sub
        if(!item.negate) {
            Goal split1 = aWithoutTo;
            Goal split2 = item.to;
            AtomDistanceListItem newItem = new AtomDistanceListItem(item);
			newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), new Add_2(split1, split2));
			outList.add(newItem);
        } else {
            Goal split1 = aWithoutTo;
            Goal split2 = item.to.negative();
            AtomDistanceListItem newItem = new AtomDistanceListItem(item);
			newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), new Sub(split1, split2));
			outList.add(newItem);
        }

        if (conf.useAdd3) {
            Goal tmpMov = item.distance.inverse().translate(item.to);
            Goal tmp = tmpMov;
            if (item.negate) {
                tmp = tmpMov.negative();
            }
            tmp = tmp.without(item.to);
            Goal split1 = aWithoutTo.without(tmp);
            if(!split1.isEmpty() && !tmp.isEmpty()) {
                Goal split2 = tmp;
                Goal split3 = item.to;
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2, split3), new Add_3(split1, split2, split3));
                outList.add(newItem);
            }
        }
        if(conf.useAddx && centre.manhattanXY()>0){
            Dir dir1 = Dir.fromDirection(centre.majorXYDirection());
            Goal split1 = aWithoutTo.translated(-dir1.x, -dir1.y, 0);
            Goal split2 = item.to.translated(-dir1.x, -dir1.y, 0);
            Addx addx = new Addx(split1, split2, dir1);
            AtomDistanceListItem newItem = new AtomDistanceListItem(item);
            newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), addx);
            outList.add(newItem);
        }

        //add2x
        if(conf.useAdd2x && centre.manhattanXY()>1){
            Transformation.Direction d1 = centre.majorXYDirection();
            Transformation.Direction d2 = centre.then(d1.opposite()).majorXYDirection();
            Dir dir1 = Dir.fromDirection(d1);
            Dir dir2 = Dir.fromDirection(d2);
            Goal split1 = aWithoutTo.translated(-dir1.x -dir2.x, -dir1.y-dir2.y, 0);
            Goal split2 = item.to.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
            Add2x add2x = new Add2x(split1, split2, dir1, dir2);
            AtomDistanceListItem newItem = new AtomDistanceListItem(item);
            newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), add2x);
            outList.add(newItem);
        }

        if(conf.useSubx && item.distance.manhattanXY()>0){
            Dir dir1 = Dir.fromDirection(item.distance.majorXYDirection()).opposite();
            Goal split1 = aWithoutTo.translated(-dir1.x, -dir1.y, 0);
            Goal split2 = item.to.negative();
            AtomDistanceListItem newItem = new AtomDistanceListItem(item);
            newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), new Subx(split1, split2, dir1));
            outList.add(newItem);
        }

        if(conf.useSub2x && item.distance.manhattanXY()>1){
            Transformation.Direction d1 = item.distance.majorXYDirection();
            Transformation.Direction d2 = item.distance.then(d1.opposite()).majorXYDirection();
            Dir dir1 = Dir.fromDirection(d1).opposite();
            Dir dir2 = Dir.fromDirection(d2).opposite();
            Goal split1 = aWithoutTo.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
            Goal split2 = item.to.negative();
            AtomDistanceListItem newItem = new AtomDistanceListItem(item);
            newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), new Sub2x(split1, split2, dir1, dir2));
            outList.add(newItem);
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private void addAtomDistancePairs(AtomDistanceListItem item,
                                      Config conf, List<AtomDistanceListItem> outList) {
        Goal tmpMov = item.distance.inverse().translate(item.to);
        Goal tmp = tmpMov;
        if(item.negate){
            tmp = tmpMov.negative();
        }
        if(tmp.same(item.a)){
            if(conf.useMov2x && item.distance.manhattanXY()>1){
                //mov2x
                Transformation.Direction d1 = item.distance.majorXYDirection();
                Transformation.Direction d2 = item.distance.then(d1.opposite()).majorXYDirection();
                Dir dir1 = Dir.fromDirection(d1).opposite();
                Dir dir2 = Dir.fromDirection(d2).opposite();
                Mov2x mov2x = new Mov2x(item.a, dir1, dir2, true);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, mov2x.a, mov2x);
                outList.add(newItem);
            } else if (item.distance.manhattanXY() > 0){
                //movx
                Transformation.Direction d1 = item.distance.majorXYDirection();
                Dir dir1 = Dir.fromDirection(d1).opposite();
                Movx movx = new Movx(item.a, dir1, true);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, movx.a, movx);
                outList.add(newItem);
            } else if(item.negate){
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, item.to, new Neg(item.to));
                outList.add(newItem);
            }
        } else {
            Goal aWithoutTmp = item.a.without(tmp);

            //Add_2
            {
                Goal split2 = aWithoutTmp;
                List<Goal> lowers = Arrays.asList(tmp, split2);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, lowers, new Add_2(tmp, split2));
                outList.add(newItem);
            }

            //Sub
            {
                Goal split2 = aWithoutTmp.negative();
                List<Goal> lowers = Arrays.asList(tmp, split2);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, lowers, new Sub(tmp, split2));
                outList.add(newItem);
            }
            //TODO add_3 support?

            //addx
            if(conf.useAddx && item.distance.manhattanXY()>0){
                Dir dir1 = Dir.fromDirection(item.distance.majorXYDirection()).opposite();
                Goal split1 = aWithoutTmp.translated(-dir1.x, -dir1.y, 0);
                Goal split2 = tmp.translated(-dir1.x, -dir1.y, 0);
                Addx addx = new Addx(split1, split2, dir1);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), addx);
                outList.add(newItem);
            }

            //add2x
            if(conf.useAdd2x && item.distance.manhattanXY()>1){
                Transformation.Direction d1 = item.distance.majorXYDirection();
                Transformation.Direction d2 = item.distance.then(d1.opposite()).majorXYDirection();
                Dir dir1 = Dir.fromDirection(d1).opposite();
                Dir dir2 = Dir.fromDirection(d2).opposite();
                Goal split1 = aWithoutTmp.translated(-dir1.x -dir2.x, -dir1.y-dir2.y, 0);
                Goal split2 = tmp.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
                Add2x add2x = new Add2x(split1, split2, dir1, dir2);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), add2x);
                outList.add(newItem);
            }

            //Subx
            if(conf.useSubx && item.distance.manhattanXY()>0){
                Dir dir1 = Dir.fromDirection(item.distance.majorXYDirection()).opposite();
                Goal split1 = tmp.translated(-dir1.x, -dir1.y, 0);
                Goal split2 = aWithoutTmp.negative();
                Subx subx = new Subx(split1, split2, dir1);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), subx);
                outList.add(newItem);
            }

            //Sub2x
            if(conf.useSub2x && item.distance.manhattanXY()>1){
                Transformation.Direction d1 = item.distance.majorXYDirection();
                Transformation.Direction d2 = item.distance.then(d1.opposite()).majorXYDirection();
                Dir dir1 = Dir.fromDirection(d1).opposite();
                Dir dir2 = Dir.fromDirection(d2).opposite();
                Goal split1 = tmp.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
                Goal split2 = aWithoutTmp.negative();
                Sub2x sub2x = new Sub2x(split1, split2, dir1, dir2);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), sub2x);
                outList.add(newItem);
            }
        }
    }

    private List<AtomDistanceListItem> getAtomDistanceList(Goal a, Goal b, boolean diagonal) {
        Map<Tuple<Distance, Boolean>, Goal.Factory> distanceMap = new HashMap<>();
        for (Iterator<Tuple<Atom, Integer>> ita = a.uniqueCountIterator(); ita.hasNext(); ) {
            Tuple<Atom, Integer> ta = ita.next();
            Atom atomA = ta.getA();
            for (Iterator<Tuple<Atom, Integer>> itb = b.uniqueCountIterator(); itb.hasNext(); ) {
                Tuple<Atom, Integer> tb = itb.next();
                Atom atomB = tb.getA();

                Distance d = new Distance(atomA, atomB);
                boolean negate = atomA.positive ^ atomB.positive;
                Tuple<Distance, Boolean> key = new Tuple<>(d, negate);
                Goal.Factory goalFactory = distanceMap.getOrDefault(key, new Goal.Factory());
                int count = Math.min(ta.getB(), tb.getB());
                if (diagonal && d.isZero()){
                    count /= 2;
                }
                for (int i = 0; i < count; i++) {
                    goalFactory.add(atomB);
                }
                distanceMap.put(key, goalFactory);

            }
        }
        List<AtomDistanceListItem> list = new ArrayList<>(distanceMap.size());
        distanceMap.forEach((key, value) -> {
            AtomDistanceListItem i = new AtomDistanceListItem();
            i.a=a;
            i.b=b;
            i.distance = key.getA();
            i.negate = key.getB();
            i.to = value.get();
            list.add(i);

        });
        if (!diagonal) {
            list.removeIf(t -> !(b.same(t.to)));
        }
        list.removeIf(t->t.to.size()==0);
        return list;
    }

    private class SteppedCombinationIterator implements Iterator<Tuple<Integer, Integer>> {
        int ii = 0;
        int jj = 0;
        int dia = -1;
        final int maxSize;
        private SteppedCombinationIterator(int maxSize) {
            this.maxSize = maxSize;
            updateIJ();
        }
        private int getI(){
            if (dia >=0){
                return dia;
            }
            return ii;
        }
        private int getJ(){
            if (dia >=0){
                return dia;
            }
            return jj - ii;
        }
        private void updateIJ(){
            if (dia < 0) {
                do{
                    if (ii < Math.min(jj, maxSize - 1)) {
                        ii++;
                    } else {
                        jj++;
                        int d = jj - maxSize;
                        if (d < 0) {
                            ii = 0;
                        } else {
                            ii = d + 1;
                        }
                    }
                }
                while(jj-ii == ii);
                if (jj-ii >= maxSize || ii >= maxSize) {
                    dia++;
                }
            } else {
                dia++;
            }

        }

        @Override
        public boolean hasNext() {
            return getJ() < maxSize && getI() < maxSize;
        }

        @Override
        public Tuple<Integer, Integer> next() {
            Tuple<Integer, Integer> t = new Tuple<>(getI(), getJ());
            updateIJ();
            return t;
        }
    }
    private class PlainCombinationIterator implements Iterator<Tuple<Integer, Integer>> {
        int ii = 0;
        int jj = 0;

        final int maxSize;
        private PlainCombinationIterator(int maxSize) {
            this.maxSize = maxSize;
        }
        private void updateIJ(){
            ii++;
            if(ii >= maxSize){
                jj++;
                ii=0;
            }
        }

        @Override
        public boolean hasNext() {
            return jj < maxSize;
        }

        @Override
        public Tuple<Integer, Integer> next() {
            Tuple<Integer, Integer> t = new Tuple<>(ii, jj);
            updateIJ();
            return t;
        }
    }

    private class IteratorPairGen implements PairGen{
        private final Iterator<Goal.Pair> it;

        private IteratorPairGen(Iterator<Goal.Pair> it) {
            this.it = it;
        }

        @Override
        public Goal.Pair next() {
            return it.hasNext()?it.next():null;
        }
    }
}
