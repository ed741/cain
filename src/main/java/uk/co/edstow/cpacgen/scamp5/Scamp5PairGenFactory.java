package uk.co.edstow.cpacgen.scamp5;

import uk.co.edstow.cpacgen.Atom;
import uk.co.edstow.cpacgen.Goal;
import uk.co.edstow.cpacgen.ReverseSplit;
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
        boolean useMov2x;
        boolean useAdd3;

        boolean useAddx;
        boolean useAdd2x;
        boolean useSubx;
        boolean useSub2x;

        boolean exhaustiveSearch;
        boolean atomDistanceSortSearch;
        boolean combinationSearch;

        int depth;
        public int threshold;

        public Config(boolean useMov2x, boolean useAdd3, boolean useAddx, boolean useAdd2x, boolean useSubx, boolean useSub2x, boolean exhaustiveSearch, boolean atomDistanceSortSearch, boolean combinationSearch, int threshold, int depth) {
            this.useMov2x = useMov2x;
            this.useAdd3 = useAdd3;
            this.useAddx = useAddx;
            this.useAdd2x = useAdd2x;
            this.useSubx = useSubx;
            this.useSub2x = useSub2x;
            this.exhaustiveSearch = exhaustiveSearch;
            this.atomDistanceSortSearch = atomDistanceSortSearch;
            this.depth = depth;
            this.combinationSearch = combinationSearch;
            this.threshold = threshold;
        }

    }

    public interface ConfigGetter {
        Config getConfig(Goal.Bag goals, int depth, ReverseSplit rs, boolean initalGoal);
    }

    @Override
    public Collection<Tuple<List<Goal.Pair>, Goal>> applyAllUnaryOpForwards(Goal initialGoal, int depth, Goal goal) {
        Config conf = confGet.getConfig(new Goal.Bag(initialGoal), depth, rs, true);
        ArrayList<Tuple<List<Goal.Pair>, Goal>> list = new ArrayList<>();

        //Negate
        Scamp5Transformation.Neg neg = new Scamp5Transformation.Neg(initialGoal);
        list.add(new Tuple<>(Collections.singletonList(new Goal.Pair(neg.applyForwards(), initialGoal, neg)), neg.applyForwards()));

        //Divide
        try{
            List<Goal.Pair> pairs = new ArrayList<>();
            Scamp5Transformation.Divq div = new Scamp5Transformation.Divq(initialGoal);
            boolean d = false;
            while(div.isPossbile() && div.applyForwards().atomCount() >= goal.atomCount()){
                pairs.add(0, new Goal.Pair(div.applyForwards(), div.a, div));
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
            list.add(new Tuple<>(Collections.singletonList(new Goal.Pair(movx.applyForwards(), initialGoal, movx)), movx.applyForwards()));        }

        //Move 2x
        if(conf.useMov2x) {
            for (Scamp5Transformation.Dir dir1 : Scamp5Transformation.Dir.values()) {
                Scamp5Transformation.Mov2x mov2xa = new Scamp5Transformation.Mov2x(initialGoal, dir1, dir1);
                list.add(new Tuple<>(Collections.singletonList(new Goal.Pair(mov2xa.applyForwards(), initialGoal, mov2xa)), mov2xa.applyForwards()));
                Scamp5Transformation.Mov2x mov2xb = new Scamp5Transformation.Mov2x(initialGoal, dir1, dir1.cw());
                list.add(new Tuple<>(Collections.singletonList(new Goal.Pair(mov2xb.applyForwards(), initialGoal, mov2xb)), mov2xb.applyForwards()));
            }
        }

        return list;
    }


    private ReverseSplit rs;
    private final ConfigGetter confGet;
    private Goal initialGoal;



    public Scamp5PairGenFactory(ConfigGetter confGet) {
        this.confGet = confGet;
    }
    private int initialDivisions;
    @Override
    public void init(ReverseSplit rs) {
        this.initialDivisions = rs.getInitialDivisions();
        this.rs =rs;
        this.initialGoal = rs.getInitialGoal();
    }

    @Override
    public PairGen generatePairs(Goal.Bag goals, int depth) {
       Config conf = confGet.getConfig(goals, depth, rs, false);
       if(conf.combinationSearch){
           return new ThresholdCombinationPairGen(conf, goals);
       } else if(conf.exhaustiveSearch){
           return new ExhaustivePairGen(goals, conf);
       } else if(conf.atomDistanceSortSearch){
           return new AtomDistanceSortedPairGen(goals, conf);
       } else {
           return new AtomDistancePairGen(goals, conf);
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
                    ).map(pair -> new Tuple<>(pair, getCost(pair, bounds, goals)))
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
            Collection<Goal> splits = upper.allSplits();
            Set<Goal> seen = new HashSet<>();
            for (Goal a : splits) {
                boolean skipAdd_2 = false;
                if (seen.contains(a)){
                    skipAdd_2 = true;
                }
                Goal b = upper.without(a);
                seen.add(b);

                if(a.isEmpty() || b.isEmpty()){
                    continue;
                }
                if(!skipAdd_2) {
                    Scamp5Transformation.Add_2 add = new Scamp5Transformation.Add_2(a, b);
                    pairs.add(new Goal.Pair(upper, Arrays.asList(a, b), add));
                    if(conf.useAddx) {
                        for (Scamp5Transformation.Dir dir : Scamp5Transformation.Dir.values()) {
                            Goal mova = a.translated(-dir.x, -dir.y, 0);
                            Goal movb = b.translated(-dir.x, -dir.y, 0);
                            Scamp5Transformation.Addx addx = new Scamp5Transformation.Addx(mova, movb, dir);
                            pairs.add(new Goal.Pair(upper, Arrays.asList(mova, movb), addx));
                        }
                    }
                    if(conf.useAdd2x){
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
                if(conf.useSubx) {
                    for (Scamp5Transformation.Dir dir : Scamp5Transformation.Dir.values()) {
                        Goal mova = a.translated(-dir.x, -dir.y, 0);
                        Scamp5Transformation.Subx subx = new Scamp5Transformation.Subx(mova, negB, dir);
                        pairs.add(new Goal.Pair(upper, Arrays.asList(mova, negB), subx));
                    }
                }
                if(conf.useSub2x) {
                    for (Scamp5Transformation.Dir dir1 : Scamp5Transformation.Dir.values()) {
                        Goal amova = a.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                        Scamp5Transformation.Sub2x sub2xa = new Scamp5Transformation.Sub2x(amova, negB, dir1, dir1);
                        pairs.add(new Goal.Pair(upper, Arrays.asList(amova, negB), sub2xa));
                        Scamp5Transformation.Dir dir2 = dir1.cw();
                        Goal amovb = a.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                        Scamp5Transformation.Sub2x sub2xb = new Scamp5Transformation.Sub2x(amovb, negB, dir1, dir2);
                        pairs.add(new Goal.Pair(upper, Arrays.asList(amova, negB), sub2xb));
                    }
                }

                if(conf.useAdd3){
                    Set<Goal> subSeen = new HashSet<>();

                    Collection<Goal> subSplits = a.allSplits();
                    for (Goal aa : subSplits) {
                        boolean skipAdd_3 = false;
                        if (subSeen.contains(aa)) {
                            skipAdd_3 = true;
                        }
                        Goal ab = a.without(aa);
                        subSeen.add(ab);
                        if(aa.isEmpty() || ab.isEmpty()){
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

        protected void fillCurrentList(){
            while (currentList.isEmpty()){
                if(!ijGetter.hasNext()){
                    return;
                }
                Tuple<Integer, Integer> ij = ijGetter.next();
                Goal a = goals.get(ij.getA());
                Goal b = goals.get(ij.getB());

                boolean diaganal = ij.getA().equals(ij.getB());
                List<Tuple<Tuple<Distance, Boolean>, Goal>> list = getAtomDistanceList(a, b, diaganal);
                list.sort(atomDistanceComparator);
                if(!diaganal) {
                    for (Tuple<Tuple<Distance, Boolean>, Goal> tuple : list) {
                        addAtomDistancePairs(a, tuple.getA().getA(), tuple.getA().getB(), tuple.getB(), conf, currentList);
                    }
                } else {
                    // diaganal == True
                    addDirectMov(a);
                    for (Tuple<Tuple<Distance, Boolean>, Goal> tuple : list) {
                        addAtomDistanceDiagonalPairs(a, tuple.getA().getA(), tuple.getA().getB(), tuple.getB(), conf, currentList);
                    }
                }
                if(initialGoal.hasSubGoal(a)){
                    Goal l = new Goal.Factory(a).addAll(a).get();
                    currentList.add(new Goal.Pair(a, l, new Scamp5Transformation.Divq(l)));
                }
            }
        }
        protected void addDirectMov(Goal a) {
            Distance centre = new Distance(a.getAveragePos());
            if(centre.manhattan()>0){
                Transformation.Direction d1 = centre.majorDirection();
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(d1).opposite();
                Scamp5Transformation.Movx movx = new Scamp5Transformation.Movx(a, dir1, true);
                currentList.add(new Goal.Pair(a, movx.a, movx));
            }
            if(conf.useMov2x && centre.manhattan()>1){
                Transformation.Direction d1 = centre.majorDirection();
                Transformation.Direction d2 = centre.then(d1.opposite()).majorDirection();
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(d1).opposite();
                Scamp5Transformation.Dir dir2 = Scamp5Transformation.Dir.fromDirection(d2).opposite();
                Scamp5Transformation.Mov2x mov2x = new Scamp5Transformation.Mov2x(a, dir1, dir2, true);
                currentList.add(new Goal.Pair(a, mov2x.a, mov2x));
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
            super(goals, conf);
            this.bounds = new Bounds(goals);

        }

        @Override
        protected void fillCurrentList() {
            while (ijGetter.hasNext()){
                Tuple<Integer, Integer> ij = ijGetter.next();
                Goal a = goals.get(ij.getA());
                Goal b = goals.get(ij.getB());
                boolean diaganal = ij.getA().equals(ij.getB());
                List<Tuple<Tuple<Distance, Boolean>, Goal>> list = getAtomDistanceList(a, b, diaganal);
                list.sort(atomDistanceComparator);
                if(!diaganal) {
                    for (Tuple<Tuple<Distance, Boolean>, Goal> tuple : list) {
                        addAtomDistancePairs(a, tuple.getA().getA(), tuple.getA().getB(), tuple.getB(), conf, currentList);
                    }
                } else {
                    // diaganal == True
                    addDirectMov(a);

                    for (Tuple<Tuple<Distance, Boolean>, Goal> tuple : list) {
                        addAtomDistanceDiagonalPairs(a, tuple.getA().getA(), tuple.getA().getB(), tuple.getB(), conf, currentList);
                    }
                }
                if(initialGoal.hasSubGoal(a)){
                    Goal l = new Goal.Factory(a).addAll(a).get();
                    currentList.add(new Goal.Pair(a, l, new Scamp5Transformation.Divq(l)));
                }

            }
            currentList = currentList.stream().map(pair -> new Tuple<>(pair, getCost(pair, bounds, goals)))
                    .sorted(Comparator.comparingDouble((Tuple<Goal.Pair, Double> t) -> t.getB()).reversed()).map(Tuple::getA).collect(Collectors.toList());
        }
    }

    private class ThresholdCombinationPairGen implements PairGen {
        final PairGen pg;

        private ThresholdCombinationPairGen(Config conf, Goal.Bag goals) {
            int max = Integer.MIN_VALUE;
            for (Goal goal : goals) {
                max = Math.max(max, goal.atomCount());
            }
            if (max < conf.threshold){
                pg = new ExhaustivePairGen(goals, conf);
            } else {
                pg = new AtomDistancePairGen(goals, conf);
            }
        }

        @Override
        public Goal.Pair next() {
            return pg.next();
        }
    }

    private static Comparator<Tuple<Tuple<Distance, Boolean>, Goal>> atomDistanceComparator = Comparator.comparingInt((Tuple<Tuple<Distance, Boolean>, Goal> t) -> t.getB().size()).thenComparingInt(t -> -t.getA().getA().manhattan());

    private double getCost(Goal.Pair pair, Bounds bounds, Goal.Bag goals) {
        HashSet<Goal> goalSet = new HashSet<>(goals);
        goalSet.remove(pair.getUpper());

        goalSet.addAll(pair.getLowers());
        double cost = 0;
        for (Goal g : goalSet) {
            int subset = 0;
            for(Goal g2 : goalSet){
                if(g2.hasSubGoal(g)){
                    subset++;
                }
                if(subset > 1){
                    break;
                }
            }
            if(subset < 2 && initialGoal.hasSubGoal(g)){
                subset++;
            }
            if(subset < 2) {
                for (Atom a : g) {
                    cost += Math.abs(a.x) + Math.abs(a.y) + Math.abs(a.z);
                }
            }
        }

        List<Goal> goalList = new ArrayList<>(goalSet);
        for (int i = 0; i < goalList.size(); i++) {
            Goal goal = goalList.get(i);
            List<Goal> toRemove = patternRepeated(bounds, goalSet, goal);
            goalList.removeAll(toRemove);
            goalList.add(i, goal);
        }
        for (Goal g: goalList){
            if(!initialGoal.hasSubGoal(g)) {
                cost += Math.pow(g.atomCount(), 2);
            }
            int min = g.minimumCount();
            //cost += (initialDivisions - (Math.log(min)/Math.log(2)));
            cost += (1<<initialDivisions) / min;

        }
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

    private void addAtomDistanceDiagonalPairs(Goal a, Distance distance, Boolean negate, Goal b,
                                              Config conf, List<Goal.Pair> currentList) {
        Distance centre = new Distance(a.getAveragePos());

        //add_2, sub
        if(!negate) {
            Goal split1 = a.without(b);
            Goal split2 = b;
            currentList.add(new Goal.Pair(a, Arrays.asList(split1, split2), new Scamp5Transformation.Add_2(split1, split2)));
        } else {
            Goal split1 = a.without(b);
            Goal split2 = b.negative();
            currentList.add(new Goal.Pair(a, Arrays.asList(split1, split2), new Scamp5Transformation.Sub(split1, split2)));
        }

        if (conf.useAdd3) {
            Goal tmpMov = distance.inverse().translate(b);
            Goal tmp = tmpMov;
            if (negate) {
                tmp = tmpMov.negative();
            }
            Goal split1 = a.without(b).without(tmp);
            if(!split1.isEmpty()) {
                Goal split2 = tmp;
                Goal split3 = b;
                currentList.add(new Goal.Pair(a, Arrays.asList(split1, split2, split3), new Scamp5Transformation.Add_3(split1, split2, split3)));
            }
        }
        //addx
        if(conf.useAddx && centre.manhattan()>0){
            Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(centre.majorDirection());
            Goal split1 = a.without(b).translated(-dir1.x, -dir1.y, 0);
            Goal split2 = b.translated(-dir1.x, -dir1.y, 0);
            Scamp5Transformation.Addx addx = new Scamp5Transformation.Addx(split1, split2, dir1);
            currentList.add(new Goal.Pair(a, Arrays.asList(split1, split2), addx));
        }

        //add2x
        if(conf.useAddx && centre.manhattan()>1){
            Transformation.Direction d1 = centre.majorDirection();
            Transformation.Direction d2 = centre.then(d1.opposite()).majorDirection();
            Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(d1);
            Scamp5Transformation.Dir dir2 = Scamp5Transformation.Dir.fromDirection(d2);
            Goal split1 = a.without(b).translated(-dir1.x -dir2.x, -dir1.y-dir2.y, 0);
            Goal split2 = b.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
            Scamp5Transformation.Add2x add2x = new Scamp5Transformation.Add2x(split1, split2, dir1, dir2);
            currentList.add(new Goal.Pair(a, Arrays.asList(split1, split2), add2x));
        }
    }

    private void addAtomDistancePairs(Goal a, Distance distance, Boolean negate, Goal b,
                                      Config conf, List<Goal.Pair> currentList) {
        Goal tmpMov = distance.inverse().translate(b);
        Goal tmp = tmpMov;
        if(negate){
            tmp = tmpMov.negative();
        }
        if(tmp.same(a)){
            if(conf.useMov2x && distance.manhattan()>1){
                //mov2x
                Transformation.Direction d1 = distance.majorDirection();
                Transformation.Direction d2 = distance.then(d1.opposite()).majorDirection();
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(d1).opposite();
                Scamp5Transformation.Dir dir2 = Scamp5Transformation.Dir.fromDirection(d2).opposite();
                Scamp5Transformation.Mov2x mov2x = new Scamp5Transformation.Mov2x(a, dir1, dir2, true);
                currentList.add(new Goal.Pair(a, mov2x.a, mov2x));
            } else if (distance.manhattan() > 0){
                //movx
                Transformation.Direction d1 = distance.majorDirection();
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(d1).opposite();
                Scamp5Transformation.Movx movx = new Scamp5Transformation.Movx(a, dir1, true);
                currentList.add(new Goal.Pair(a, movx.a, movx));
            } else if(negate){
                currentList.add(new Goal.Pair(a, b, new Scamp5Transformation.Neg(b)));
            }
        } else {
            //Add_2
            {
                Goal split2 = a.without(tmp);
                List<Goal> lowers = Arrays.asList(tmp, split2);
                currentList.add(new Goal.Pair(a, lowers, new Scamp5Transformation.Add_2(tmp, split2)));
            }

            //Sub
            {
                Goal split2 = a.without(tmp).negative();
                List<Goal> lowers = Arrays.asList(tmp, split2);
                currentList.add(new Goal.Pair(a, lowers, new Scamp5Transformation.Sub(tmp, split2)));
            }
            //TODO add_3 support?

            //addx
            if(conf.useAddx && distance.manhattan()>0){
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(distance.majorDirection()).opposite();
                Goal split1 = a.without(tmp).translated(-dir1.x, -dir1.y, 0);
                Goal split2 = tmp.translated(-dir1.x, -dir1.y, 0);
                Scamp5Transformation.Addx addx = new Scamp5Transformation.Addx(split1, split2, dir1);
                currentList.add(new Goal.Pair(a, Arrays.asList(split1, split2), addx));
            }

            //add2x
            if(conf.useAdd2x && distance.manhattan()>1){
                Transformation.Direction d1 = distance.majorDirection();
                Transformation.Direction d2 = distance.then(d1.opposite()).majorDirection();
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(d1).opposite();
                Scamp5Transformation.Dir dir2 = Scamp5Transformation.Dir.fromDirection(d2).opposite();
                Goal split1 = a.without(tmp).translated(-dir1.x -dir2.x, -dir1.y-dir2.y, 0);
                Goal split2 = tmp.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
                Scamp5Transformation.Add2x add2x = new Scamp5Transformation.Add2x(split1, split2, dir1, dir2);
                currentList.add(new Goal.Pair(a, Arrays.asList(split1, split2), add2x));
            }

            //Subx
            if(conf.useSubx && distance.manhattan()>0){
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(distance.majorDirection()).opposite();
                Goal split1 = tmp.translated(-dir1.x, -dir1.y, 0);
                Goal split2 = a.without(tmp).negative();
                Scamp5Transformation.Subx subx = new Scamp5Transformation.Subx(split1, split2, dir1);
                currentList.add(new Goal.Pair(a, Arrays.asList(split1, split2), subx));
            }

            //Sub2x
            if(conf.useSub2x && distance.manhattan()>1){
                Transformation.Direction d1 = distance.majorDirection();
                Transformation.Direction d2 = distance.then(d1.opposite()).majorDirection();
                Scamp5Transformation.Dir dir1 = Scamp5Transformation.Dir.fromDirection(d1).opposite();
                Scamp5Transformation.Dir dir2 = Scamp5Transformation.Dir.fromDirection(d2).opposite();
                Goal split1 = tmp.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
                Goal split2 = a.without(tmp).negative();
                Scamp5Transformation.Sub2x sub2x = new Scamp5Transformation.Sub2x(split1, split2, dir1, dir2);
                currentList.add(new Goal.Pair(a, Arrays.asList(split1, split2), sub2x));
            }
        }
    }

    private List<Tuple<Tuple<Distance, Boolean>, Goal>> getAtomDistanceList(Goal a, Goal b, boolean diagonal) {
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
        List<Tuple<Tuple<Distance, Boolean>, Goal>> list = new ArrayList<>(distanceMap.size());
        distanceMap.forEach((key, value) -> list.add(new Tuple<>(key, value.get())));
        if (!diagonal) {
            list.removeIf(t -> !(b.equals(t.getB())));
        }
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
