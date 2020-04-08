package cpacgen.scamp5;

import cpacgen.Atom;
import cpacgen.Goal;
import cpacgen.ReverseSplit;
import cpacgen.Transformation;
import cpacgen.pairgen.PairGenFactory;
import cpacgen.util.Bounds;
import cpacgen.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cpacgen.scamp5.Scamp5Transformation.*;

public class Scamp5PairGenFactory implements PairGenFactory {


    public static class Config {
        boolean useMov2x;

        boolean useAdd3;
        boolean useAddx;
        boolean useAdd2x;
        boolean useSubx;
        boolean useSub2x;
        boolean exhaustiveSearch;

        int depth;

        public Config(boolean useMov2x, boolean useAdd3, boolean useAddx, boolean useAdd2x, boolean useSubx, boolean useSub2x, boolean exhaustiveSearch, int depth) {
            this.useMov2x = useMov2x;
            this.useAdd3 = useAdd3;
            this.useAddx = useAddx;
            this.useAdd2x = useAdd2x;
            this.useSubx = useSubx;
            this.useSub2x = useSub2x;
            this.exhaustiveSearch = exhaustiveSearch;
            this.depth = depth;
        }

    }

    public interface ConfigGetter {
        Config getConfig(Goal.Bag goals, int depth, ReverseSplit rs, boolean initalGoal);
    }

    @Override
    public Collection<Tuple<? extends Transformation, Goal>> applyAllUnaryOpForwards(Goal initialGoal, int depth) {
        Config conf = confGet.getConfig(new Goal.Bag(initialGoal), depth, rs, true);
        ArrayList<Tuple<? extends Transformation, Goal>> list = new ArrayList<>();

        //Negate
        Neg neg = new Neg(initialGoal);
        list.add(new Tuple<>(neg, neg.applyForwards()));

        //Divide
        Divq div = new Divq(initialGoal);
        if(div.isPossbile()){
            try {
                list.add(new Tuple<>(div, div.applyForwards()));
            } catch (TransformationApplicationException e) {
                e.printStackTrace();
                // Should be unreachable
                assert false;
            }
        }

        //Move x
        for (Dir dir: Dir.values()) {
            Movx movx = new Movx(initialGoal, dir);
            list.add(new Tuple<>(movx, movx.applyForwards()));
        }

        //Move 2x
        if(conf.useMov2x) {
            for (Dir dir1 : Dir.values()) {
                Mov2x mov2xa = new Mov2x(initialGoal, dir1, dir1);
                list.add(new Tuple<>(mov2xa, mov2xa.applyForwards()));
                Mov2x mov2xb = new Mov2x(initialGoal, dir1, dir1.cw());
                list.add(new Tuple<>(mov2xb, mov2xb.applyForwards()));
            }
        }
        return list;
    }


    private ReverseSplit rs;
    private final ConfigGetter confGet;



    public Scamp5PairGenFactory(ConfigGetter confGet) {
        this.confGet = confGet;
    }

    @Override
    public void init(ReverseSplit rs) {
    }

    @Override
    public PairGen generatePairs(Goal.Bag goals, int depth) {
       Config conf = confGet.getConfig(goals, depth, rs, false);
        if(conf.exhaustiveSearch){
            return new ExhaustivePairGen(goals, conf);
        }
        return null;
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
                    ).map(pair -> new Tuple<>(pair, getCost(pair)))
                    .sorted(comparator).collect(Collectors.toList());
            this.it = list.stream().map(Tuple::getA).iterator();
        }

        @Override
        public Goal.Pair next() {
            return it.hasNext()?it.next():null;
        }

        private double getCost(Goal.Pair pair) {
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
                cost += Math.pow(g.atomCount(), 2);
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


        private Stream<Goal.Pair> getUnaryOpStream(Goal upper) {
            ArrayList<Goal.Pair> pairs = new ArrayList<>();

            //Negate
            Neg neg = new Neg(upper, true);
            pairs.add(new Goal.Pair(upper, neg.a, neg));

            //Divide
            Divq div = new Divq(upper, true);
            pairs.add(new Goal.Pair(upper, div.a, div));

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
                    Add_2 add = new Add_2(a, b);
                    pairs.add(new Goal.Pair(upper, Arrays.asList(a, b), add));
                    if(conf.useAddx) {
                        for (Dir dir : Dir.values()) {
                            Goal mova = a.translated(-dir.x, -dir.y, 0);
                            Goal movb = b.translated(-dir.x, -dir.y, 0);
                            Addx addx = new Addx(mova, movb, dir);
                            pairs.add(new Goal.Pair(upper, Arrays.asList(mova, movb), addx));
                        }
                    }
                    if(conf.useAdd2x){
                        for (Dir dir1 : Dir.values()) {
                            Goal amova = a.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                            Goal bmova = b.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                            Add2x add2xa = new Add2x(amova, bmova, dir1, dir1);
                            pairs.add(new Goal.Pair(upper, Arrays.asList(amova, bmova), add2xa));
                            Dir dir2 = dir1.cw();
                            Goal amovb = a.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                            Goal bmovb = b.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                            Add2x add2xb = new Add2x(amovb, bmovb, dir1, dir2);
                            pairs.add(new Goal.Pair(upper, Arrays.asList(amovb, bmovb), add2xb));
                        }
                    }
                }

                Goal negB = b.negative();
                Sub sub = new Sub(a, negB);
                pairs.add(new Goal.Pair(upper, Arrays.asList(a, negB), sub));
                if(conf.useSubx) {
                    for (Dir dir : Dir.values()) {
                        Goal mova = a.translated(-dir.x, -dir.y, 0);
                        Subx subx = new Subx(mova, negB, dir);
                        pairs.add(new Goal.Pair(upper, Arrays.asList(mova, negB), subx));
                    }
                }
                if(conf.useSub2x) {
                    for (Dir dir1 : Dir.values()) {
                        Goal amova = a.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                        Sub2x sub2xa = new Sub2x(amova, negB, dir1, dir1);
                        pairs.add(new Goal.Pair(upper, Arrays.asList(amova, negB), sub2xa));
                        Dir dir2 = dir1.cw();
                        Goal amovb = a.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                        Sub2x sub2xb = new Sub2x(amovb, negB, dir1, dir2);
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
                            Add_3 add = new Add_3(aa, ab, b);
                            pairs.add(new Goal.Pair(upper, Arrays.asList(aa, ab, b), add));
                        }
                    }

                }


            }

            return pairs.stream();
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
