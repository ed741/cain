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

public class Scamp5PairGenFactory implements PairGenFactory {


    public static class Config {
        private final SearchStrategy stratergy;

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
            Exhuastive, AtomDistance, SortedAtomDistance;
        }

        public Config(SearchStrategy stratergy, int availableRegisters, int depth) {
            this.stratergy = stratergy;
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

        public Config(SearchStrategy stratergy, boolean useMov2x, boolean useAdd3, boolean useAddx, boolean useAdd2x, boolean useSubx, boolean useSub2x, int availableRegisters, int depth) {
            this.stratergy = stratergy;

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
        Config getConfig(Goal.Bag goals, int depth, ReverseSearch rs, boolean initalGoal);
    }

    @Override
    public Collection<Tuple<List<Goal.Pair>, Goal>> applyAllUnaryOpForwards(Goal initialGoal, int depth, Goal goal) {
        Config conf = confGet.getConfig(new Goal.Bag(initialGoal), depth, rs, true);
        ArrayList<Tuple<List<Goal.Pair>, Goal>> list = new ArrayList<>();

        //Negate
        Scamp5Transformation.Neg neg = new Scamp5Transformation.Neg(initialGoal);
        if(goal.same(neg.applyForwards())) {
            list.add(new Tuple<>(Collections.singletonList(new Goal.Pair(goal, initialGoal, neg)), neg.applyForwards()));
        }
        //Divide
        try{
            List<Goal.Pair> pairs = new ArrayList<>();
            Scamp5Transformation.Divq div = new Scamp5Transformation.Divq(initialGoal);
            boolean d = false;
            while(div.isPossbile() && div.applyForwards().atomCount() >= goal.atomCount()){
                Goal upper = div.applyForwards();
                if(div.applyForwards().same(goal)) {
                    upper = goal;
                }
                pairs.add(0, new Goal.Pair(upper, div.a, div));
                div = new Scamp5Transformation.Divq(div.applyForwards());
                d = true;
            }
            if(d) {
                list.add(new Tuple<>(pairs, div.a));
            }

        } catch (Transformation.TransformationApplicationException e) {
            e.printStackTrace();
            // Should be unreachable
            assert false;
        }

        //Move x
        for (Scamp5Transformation.Dir dir: Scamp5Transformation.Dir.values()) {
            Scamp5Transformation.Movx movx = new Scamp5Transformation.Movx(initialGoal, dir);
            if(goal.same(movx.applyForwards())) {
                list.add(new Tuple<>(Collections.singletonList(new Goal.Pair(goal, initialGoal, movx)), movx.applyForwards()));
            }
        }

        //Move 2x
        if(conf.useMov2x) {
            for (Scamp5Transformation.Dir dir1 : Scamp5Transformation.Dir.values()) {
                Scamp5Transformation.Mov2x mov2xa = new Scamp5Transformation.Mov2x(initialGoal, dir1, dir1);
                if(goal.same(mov2xa.applyForwards())) {
                    list.add(new Tuple<>(Collections.singletonList(new Goal.Pair(goal, initialGoal, mov2xa)), mov2xa.applyForwards()));
                }
                Scamp5Transformation.Mov2x mov2xb = new Scamp5Transformation.Mov2x(initialGoal, dir1, dir1.cw());
                if(goal.same(mov2xb.applyForwards())) {
                    list.add(new Tuple<>(Collections.singletonList(new Goal.Pair(goal, initialGoal, mov2xb)), mov2xb.applyForwards()));
                }
            }
        }

        return list;
    }


    private ReverseSearch rs;
    private final ConfigGetter confGet;
    private Goal initialGoal;



    public Scamp5PairGenFactory(ConfigGetter confGet) {
        this.confGet = confGet;
    }
    private int initialDivisions;
    @Override
    public void init(ReverseSearch rs) {
        this.initialDivisions = rs.getInitialDivisions();
        this.rs =rs;
        this.initialGoal = rs.getInitialGoal();
    }



    @Override
    public PairGen generatePairs(Goal.Bag goals, int depth) {
       Config conf = confGet.getConfig(goals, depth, rs, false);
       switch (conf.stratergy){
           case Exhuastive:
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
//            this.it = goals.parallelStream()
//                    .flatMap((Goal upper) ->
//                            Stream.concat(
//                                    getNaryOpStream(upper),
//                                    getUnaryOpStream(upper)
//                            )
//                    ).map(pair -> new Tuple<>(pair, getCost(pair)))
//                    .sorted(comparator)
//                    .map(Tuple::getA)
//                    .iterator();
            List<Tuple<Goal.Pair, Double>> list = goals.parallelStream()
                    .flatMap((Goal upper) ->
                            Stream.concat(
                                    getNaryOpStream(upper),
                                    getUnaryOpStream(upper)
                            )
                    ).map(pair -> new Tuple<>(pair, getCost(pair, goals, conf)))
                    .sorted(comparator).collect(Collectors.toList());
            this.it = list.stream().map(Tuple::getA).iterator();
        }

        @Override
        public Goal.Pair next() {
            return it.hasNext()?it.next():null;
        }


        private Stream<Goal.Pair> getUnaryOpStream(Goal upper) {
            ArrayList<Goal.Pair> pairs = new ArrayList<>();

            //Negate
            Scamp5Transformation.Neg neg = new Scamp5Transformation.Neg(upper, true);
            pairs.add(new Goal.Pair(upper, neg.a, neg));

            //Divide
            Scamp5Transformation.Divq div = new Scamp5Transformation.Divq(upper, true);
            pairs.add(new Goal.Pair(upper, div.a, div));

            //Mov
            Scamp5Transformation.Mov mov = new Scamp5Transformation.Mov(upper, true);
            pairs.add(new Goal.Pair(upper, mov.a, mov));

            //Move x
            for (Scamp5Transformation.Dir dir: Scamp5Transformation.Dir.values()) {
                Scamp5Transformation.Movx movx = new Scamp5Transformation.Movx(upper, dir, true);
                pairs.add(new Goal.Pair(upper, movx.a, movx));
            }

            if(conf.useMov2x) {
                for (Scamp5Transformation.Dir dir1 : Scamp5Transformation.Dir.values()) {
                    Scamp5Transformation.Mov2x mov2xa = new Scamp5Transformation.Mov2x(upper, dir1, dir1, true);
                    pairs.add(new Goal.Pair(upper, mov2xa.a, mov2xa));
                    Scamp5Transformation.Mov2x mov2xb = new Scamp5Transformation.Mov2x(upper, dir1, dir1.cw(), true);
                    pairs.add(new Goal.Pair(upper, mov2xb.a, mov2xb));
                }
            }

            return pairs.stream();
        }

        private Stream<Goal.Pair> getNaryOpStream(Goal upper) {
            ArrayList<Goal.Pair> pairs = new ArrayList<>();
            List<Goal> splits = upper.allSplitsRecursive();
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
                    Scamp5Transformation.Add_2 add = new Scamp5Transformation.Add_2(a, b);
                    pairs.add(new Goal.Pair(upper, Arrays.asList(a, b), add));
                    if (conf.useAddx) {
                        for (Scamp5Transformation.Dir dir : Scamp5Transformation.Dir.values()) {
                            Goal mova = a.translated(-dir.x, -dir.y, 0);
                            Goal movb = b.translated(-dir.x, -dir.y, 0);
                            Scamp5Transformation.Addx addx = new Scamp5Transformation.Addx(mova, movb, dir);
                            pairs.add(new Goal.Pair(upper, Arrays.asList(mova, movb), addx));
                        }
                    }
                    if (conf.useAdd2x) {
                        for (Scamp5Transformation.Dir dir1 : Scamp5Transformation.Dir.values()) {
                            Goal amova = a.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                            Goal bmova = b.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                            Scamp5Transformation.Add2x add2xa = new Scamp5Transformation.Add2x(amova, bmova, dir1, dir1);
                            pairs.add(new Goal.Pair(upper, Arrays.asList(amova, bmova), add2xa));
                            Scamp5Transformation.Dir dir2 = dir1.cw();
                            Goal amovb = a.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                            Goal bmovb = b.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                            Scamp5Transformation.Add2x add2xb = new Scamp5Transformation.Add2x(amovb, bmovb, dir1, dir2);
                            pairs.add(new Goal.Pair(upper, Arrays.asList(amovb, bmovb), add2xb));
                        }
                    }
                }

                Goal negB = b.negative();
                Scamp5Transformation.Sub sub = new Scamp5Transformation.Sub(a, negB);
                pairs.add(new Goal.Pair(upper, Arrays.asList(a, negB), sub));
                if (conf.useSubx) {
                    for (Scamp5Transformation.Dir dir : Scamp5Transformation.Dir.values()) {
                        Goal mova = a.translated(-dir.x, -dir.y, 0);
                        Scamp5Transformation.Subx subx = new Scamp5Transformation.Subx(mova, negB, dir);
                        pairs.add(new Goal.Pair(upper, Arrays.asList(mova, negB), subx));
                    }
                }
                if (conf.useSub2x) {
                    for (Scamp5Transformation.Dir dir1 : Scamp5Transformation.Dir.values()) {
                        Goal amova = a.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                        Scamp5Transformation.Sub2x sub2xa = new Scamp5Transformation.Sub2x(amova, negB, dir1, dir1);
                        pairs.add(new Goal.Pair(upper, Arrays.asList(amova, negB), sub2xa));
                        Scamp5Transformation.Dir dir2 = dir1.cw();
                        Goal amovb = a.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                        Scamp5Transformation.Sub2x sub2xb = new Scamp5Transformation.Sub2x(amovb, negB, dir1, dir2);
                        pairs.add(new Goal.Pair(upper, Arrays.asList(amovb, negB), sub2xb));
                    }
                }

                if (conf.useAdd3) {
                    Set<Goal> subSeen = new HashSet<>();

                    Collection<Goal> subSplits = a.allSplitsRecursive();
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
                            Scamp5Transformation.Add_3 add = new Scamp5Transformation.Add_3(aa, ab, b);
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

        public AtomDistanceListItem() {
        }
        
        public AtomDistanceListItem(AtomDistanceListItem item) {
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
        Iterator<Tuple<Integer, Integer>> ijGetter;

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

        protected void fillCurrentList(){
            while (currentList.isEmpty()){
                if(!ijGetter.hasNext()){
                    return;
                }
                Tuple<Integer, Integer> ij = ijGetter.next();
                Goal a = goals.get(ij.getA());
                Goal b = goals.get(ij.getB());

                boolean diaganal = ij.getA().equals(ij.getB());
                List<AtomDistanceListItem> inList = getAtomDistanceList(a, b, diaganal);
                List<AtomDistanceListItem> outList = new ArrayList<>();
                inList.sort(atomDistanceComparator);
                addPairs(a, diaganal, inList, outList);
                outList.forEach(item -> currentList.add(item.pair));

            }
        }
        protected void addPairs(Goal a, boolean diaganal, List<AtomDistanceListItem> inList, List<AtomDistanceListItem> outList) {
            if(!diaganal) {
                for (AtomDistanceListItem item : inList) {
                    addAtomDistancePairs(item, conf, outList);
                }
            } else {
                // diaganal == True
                if(goals.size() < conf.availableRegisters) {
                    addDirectMov(a, outList);
                }

                for (AtomDistanceListItem item : inList) {
                    addAtomDistanceDiagonalPairs(item, conf, outList);
                }
                if(initialGoal.hasSubGoal(a)){
                    Goal l = new Goal.Factory(a).addAll(a).get();
                    AtomDistanceListItem newItem = new AtomDistanceListItem();
                    newItem.a = a;
                    newItem.distance = new Distance(0,0,0);
                    newItem.pair = new Goal.Pair(a, l, new Scamp5Transformation.Divq(l));
                    outList.add(newItem);
                }
            }
        }

        protected void addDirectMov(Goal a, List<AtomDistanceListItem> outList) {
            Distance centre = new Distance(a.getAveragePos());
            if(centre.manhattan()>0){
                Transformation.Direction d1 = centre.majorDirection();
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(d1);
                Scamp5Transformation.Movx movx = new Scamp5Transformation.Movx(a, dir1, true);
                AtomDistanceListItem newItem = new AtomDistanceListItem();
                newItem.a = a;
                newItem.distance = new Distance(d1, 1);
                newItem.pair = new Goal.Pair(a, movx.a, movx);
                outList.add(newItem);
            }
            if(conf.useMov2x && centre.manhattan()>1){
                Transformation.Direction d1 = centre.majorDirection();
                Transformation.Direction d2 = centre.then(d1.opposite()).majorDirection();
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(d1).opposite();
                Scamp5Transformation.Dir dir2 = Scamp5Transformation.Dir.fromDirection(d2).opposite();
                Scamp5Transformation.Mov2x mov2x = new Scamp5Transformation.Mov2x(a, dir1, dir2, true);
                AtomDistanceListItem newItem = new AtomDistanceListItem();
                newItem.a = a;
                newItem.distance = new Distance(d1, 1).then(d2);
                newItem.pair = new Goal.Pair(a, mov2x.a, mov2x);
                outList.add(newItem);
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
                boolean diaganal = ij.getA().equals(ij.getB());
                List<AtomDistanceListItem> inList = getAtomDistanceList(a, b, diaganal);
                inList.sort(atomDistanceComparator);
                addPairs(a, diaganal, inList, outList);
            }
            outList.parallelStream().forEach(item -> item.cost = getCost(item.pair, goals, conf));
            outList.sort(Comparator.comparingDouble((AtomDistanceListItem item) -> item.cost).reversed());
            currentList = outList.stream().map(item -> item.pair).collect(Collectors.toList());
        }
    }

    private static Comparator<AtomDistanceListItem> atomDistanceComparator = Comparator.comparingInt((AtomDistanceListItem i) -> i.to.size()).thenComparingInt(i -> -i.distance.manhattan());

    private double getCost(Goal.Pair pair, Goal.Bag goals, Config config) {
        Goal.Bag proposedGoals = new Goal.Bag(goals);
        proposedGoals.remove(pair.getUpper());

        List<Goal> toAdd = new ArrayList<>();
        for (Goal goal : pair.getLowers()) {
            proposedGoals.remove(goal);
            toAdd.add(goal);
        }
        proposedGoals.addAll(toAdd);
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
        Bounds bounds = new Bounds(proposedGoals);
        for (int i = 0; i < goalList.size(); i++) {
            Goal goal = goalList.get(i);
            List<Goal> toRemove = patternRepeated(bounds, goalList, goal);
            goalList.removeAll(toRemove);
            if(i > goalList.size()){
                System.out.println("I " + i);
                System.out.println("Goal " + goal);
                System.out.println("Goal List " + goalList);
                System.out.println("toRemove " + toRemove);
                System.out.println("GoalSet " + proposedGoals);
                System.out.println("Bounds" + bounds);
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

        cost += (1<<initialDivisions) / min;
        cost += max / (1<<initialDivisions);

        return cost;
    }

    private List<Goal> patternRepeated(Bounds bounds, Collection<Goal> goals, Goal pattern){
        List<Goal> matches = new ArrayList<>();
        int bx = bounds.xMax-bounds.xMin;
        int by = bounds.yMax-bounds.yMin;
        for (int i = -bx; i <= bx; i++) {
            for (int j = -by; j <= by; j++) {
                Goal tmp = pattern.translated(i, j, 0);
                if (goals.contains(tmp)){
                    matches.add(tmp);
                }

            }
        }
        return matches;
    }

    private void addAtomDistanceDiagonalPairs(AtomDistanceListItem item,
                                              Config conf, List<AtomDistanceListItem> outList) {
        Distance centre = new Distance(item.a.getAveragePos());
        Goal aWithoutTo = item.a.without(item.to);
        //add_2, sub
        if(!item.negate) {
            Goal split1 = aWithoutTo;
            Goal split2 = item.to;
            AtomDistanceListItem newItem = new AtomDistanceListItem(item);
			newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), new Scamp5Transformation.Add_2(split1, split2));
			outList.add(newItem);
        } else {
            Goal split1 = aWithoutTo;
            Goal split2 = item.to.negative();
            AtomDistanceListItem newItem = new AtomDistanceListItem(item);
			newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), new Scamp5Transformation.Sub(split1, split2));
			outList.add(newItem);
        }

        if (conf.useAdd3) {
            Goal tmpMov = item.distance.inverse().translate(item.to);
            Goal tmp = tmpMov;
            if (item.negate) {
                tmp = tmpMov.negative();
            }
            Goal split1 = aWithoutTo.without(tmp);
            if(!split1.isEmpty()) {
                Goal split2 = tmp;
                Goal split3 = item.to;
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
			newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2, split3), new Scamp5Transformation.Add_3(split1, split2, split3));
			outList.add(newItem);
            }
        }
        //addx
        if(conf.useAddx && centre.manhattan()>0){
            Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(centre.majorDirection());
            Goal split1 = aWithoutTo.translated(-dir1.x, -dir1.y, 0);
            Goal split2 = item.to.translated(-dir1.x, -dir1.y, 0);
            Scamp5Transformation.Addx addx = new Scamp5Transformation.Addx(split1, split2, dir1);
            AtomDistanceListItem newItem = new AtomDistanceListItem(item);
			newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), addx);
			outList.add(newItem);
        }

        //add2x
        if(conf.useAddx && centre.manhattan()>1){
            Transformation.Direction d1 = centre.majorDirection();
            Transformation.Direction d2 = centre.then(d1.opposite()).majorDirection();
            Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(d1);
            Scamp5Transformation.Dir dir2 = Scamp5Transformation.Dir.fromDirection(d2);
            Goal split1 = aWithoutTo.translated(-dir1.x -dir2.x, -dir1.y-dir2.y, 0);
            Goal split2 = item.to.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
            Scamp5Transformation.Add2x add2x = new Scamp5Transformation.Add2x(split1, split2, dir1, dir2);
            AtomDistanceListItem newItem = new AtomDistanceListItem(item);
			newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), add2x);
			outList.add(newItem);
        }
    }

    private void addAtomDistancePairs(AtomDistanceListItem item,
                                      Config conf, List<AtomDistanceListItem> outList) {
        Goal tmpMov = item.distance.inverse().translate(item.to);
        Goal tmp = tmpMov;
        if(item.negate){
            tmp = tmpMov.negative();
        }
        if(tmp.same(item.a)){
            if(conf.useMov2x && item.distance.manhattan()>1){
                //mov2x
                Transformation.Direction d1 = item.distance.majorDirection();
                Transformation.Direction d2 = item.distance.then(d1.opposite()).majorDirection();
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(d1).opposite();
                Scamp5Transformation.Dir dir2 = Scamp5Transformation.Dir.fromDirection(d2).opposite();
                Scamp5Transformation.Mov2x mov2x = new Scamp5Transformation.Mov2x(item.a, dir1, dir2, true);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, mov2x.a, mov2x);
                outList.add(newItem);
            } else if (item.distance.manhattan() > 0){
                //movx
                Transformation.Direction d1 = item.distance.majorDirection();
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(d1).opposite();
                Scamp5Transformation.Movx movx = new Scamp5Transformation.Movx(item.a, dir1, true);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, movx.a, movx);
                outList.add(newItem);
            } else if(item.negate){
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, item.to, new Scamp5Transformation.Neg(item.to));
                outList.add(newItem);
            }
        } else {
            Goal aWithoutTmp = item.a.without(tmp);

            //Add_2
            {
                Goal split2 = aWithoutTmp;
                List<Goal> lowers = Arrays.asList(tmp, split2);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, lowers, new Scamp5Transformation.Add_2(tmp, split2));
                outList.add(newItem);
            }

            //Sub
            {
                Goal split2 = aWithoutTmp.negative();
                List<Goal> lowers = Arrays.asList(tmp, split2);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, lowers, new Scamp5Transformation.Sub(tmp, split2));
                outList.add(newItem);
            }
            //TODO add_3 support?

            //addx
            if(conf.useAddx && item.distance.manhattan()>0){
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(item.distance.majorDirection()).opposite();
                Goal split1 = aWithoutTmp.translated(-dir1.x, -dir1.y, 0);
                Goal split2 = tmp.translated(-dir1.x, -dir1.y, 0);
                Scamp5Transformation.Addx addx = new Scamp5Transformation.Addx(split1, split2, dir1);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), addx);
                outList.add(newItem);
            }

            //add2x
            if(conf.useAdd2x && item.distance.manhattan()>1){
                Transformation.Direction d1 = item.distance.majorDirection();
                Transformation.Direction d2 = item.distance.then(d1.opposite()).majorDirection();
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(d1).opposite();
                Scamp5Transformation.Dir dir2 = Scamp5Transformation.Dir.fromDirection(d2).opposite();
                Goal split1 = aWithoutTmp.translated(-dir1.x -dir2.x, -dir1.y-dir2.y, 0);
                Goal split2 = tmp.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
                Scamp5Transformation.Add2x add2x = new Scamp5Transformation.Add2x(split1, split2, dir1, dir2);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), add2x);
                outList.add(newItem);
            }

            //Subx
            if(conf.useSubx && item.distance.manhattan()>0){
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(item.distance.majorDirection()).opposite();
                Goal split1 = tmp.translated(-dir1.x, -dir1.y, 0);
                Goal split2 = aWithoutTmp.negative();
                Scamp5Transformation.Subx subx = new Scamp5Transformation.Subx(split1, split2, dir1);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new Goal.Pair(item.a, Arrays.asList(split1, split2), subx);
                outList.add(newItem);
            }

            //Sub2x
            if(conf.useSub2x && item.distance.manhattan()>1){
                Transformation.Direction d1 = item.distance.majorDirection();
                Transformation.Direction d2 = item.distance.then(d1.opposite()).majorDirection();
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(d1).opposite();
                Scamp5Transformation.Dir dir2 = Scamp5Transformation.Dir.fromDirection(d2).opposite();
                Goal split1 = tmp.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
                Goal split2 = aWithoutTmp.negative();
                Scamp5Transformation.Sub2x sub2x = new Scamp5Transformation.Sub2x(split1, split2, dir1, dir2);
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
        private int geti(){
            if (dia >=0){
                return dia;
            }
            return ii;
        }
        private int getj(){
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
            return getj() < maxSize && geti() < maxSize;
        }

        @Override
        public Tuple<Integer, Integer> next() {
            Tuple<Integer, Integer> t = new Tuple<>(geti(), getj());
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
