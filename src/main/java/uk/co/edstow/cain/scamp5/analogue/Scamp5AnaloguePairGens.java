package uk.co.edstow.cain.scamp5.analogue;

import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.atomGoal.pairGen.Distance;
import uk.co.edstow.cain.goals.atomGoal.pairGen.SimpleTransformation;
import uk.co.edstow.cain.pairgen.*;
import uk.co.edstow.cain.goals.atomGoal.Atom;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.co.edstow.cain.scamp5.analogue.Scamp5AnalogueTransformation.*;

public class Scamp5AnaloguePairGens {

    public static class ExhaustivePairGen<G extends Kernel3DGoal<G>> extends uk.co.edstow.cain.pairgen.ExhaustivePairGen<G, Scamp5AnalogueTransformation<G>, Register>{

        Scamp5AnalogueConfig scamp5AnalogueConfig;
        public ExhaustivePairGen(GoalBag<G> goals, Context<G, Scamp5AnalogueTransformation<G>, Register> context, Scamp5AnalogueConfig scamp5AnalogueConfig, CostHeuristic<G, Scamp5AnalogueTransformation<G>, Register> heuristic) {
            super(goals, context, heuristic);
            this.scamp5AnalogueConfig = scamp5AnalogueConfig;
        }

        protected Stream<GoalPair<G, Scamp5AnalogueTransformation<G>, Register>> getUnaryOpStream(G upper) {
            ArrayList<GoalPair<G, Scamp5AnalogueTransformation<G>, Register>> pairs = new ArrayList<>();
            if(!scamp5AnalogueConfig.onlyMov) {
                //Negate
                if(scamp5AnalogueConfig.useNeg) {
                    Neg<G> neg = new Neg<>(upper, true, scamp5AnalogueConfig);
                    pairs.add(new GoalPair<>(upper, neg.a, neg));
                }

                if (upper.allZero()) {
                    if(scamp5AnalogueConfig.useRes) {
                        Res<G> res = new Res<>(upper, scamp5AnalogueConfig);
                        pairs.add(new GoalPair<>(upper, Collections.emptyList(), res));

                    }
                    if(scamp5AnalogueConfig.useRes2) {
                        G other = null;
                        for (G g : goals) {
                            if (g.allZero() && !g.equivalent(upper)) {
                                other = g;
                                break;
                            }
                        }
                        if (other != null) {
                            Res_2<G> res_2 = new Res_2<>(upper, other, scamp5AnalogueConfig);
                            pairs.add(new GoalPair<>(Arrays.asList(upper, other), Collections.emptyList(), res_2));
                        }
                    }
                }


                //Divide
                if(scamp5AnalogueConfig.useDiv3) {
                    Div<G> divC = new Div<>(upper, true, true, scamp5AnalogueConfig);
                    pairs.add(new GoalPair<>(upper, divC.a, divC));
                }
                if(scamp5AnalogueConfig.useDiv4) {
                    Div<G> div = new Div<>(upper, true, false, scamp5AnalogueConfig);
                    pairs.add(new GoalPair<>(upper, div.a, div));
                }
                //Divide
                if(scamp5AnalogueConfig.useDivq) {
                    Divq<G> divq = new Divq<>(upper, true, scamp5AnalogueConfig);
                    pairs.add(new GoalPair<>(upper, divq.a, divq));
                }
            }

            //Mov
            if(scamp5AnalogueConfig.useMov) {
                Mov<G> mov = new Mov<>(upper, true, scamp5AnalogueConfig);
                pairs.add(new GoalPair<>(upper, mov.a, mov));
            }

            //Move x
            if(scamp5AnalogueConfig.useMovx) {
                for (Dir dir : Dir.values()) {
                    Movx<G> movx = new Movx<>(upper, dir, true, scamp5AnalogueConfig);
                    pairs.add(new GoalPair<>(upper, movx.a, movx));
                }
            }

            if(scamp5AnalogueConfig.useMov2x) {
                for (Dir dir1 : Dir.values()) {
                    Mov2x<G> mov2xa = new Mov2x<>(upper, dir1, dir1, true, scamp5AnalogueConfig);
                    pairs.add(new GoalPair<>(upper, mov2xa.a, mov2xa));
                    Mov2x<G> mov2xb = new Mov2x<>(upper, dir1, dir1.cw(), true, scamp5AnalogueConfig);
                    pairs.add(new GoalPair<>(upper, mov2xb.a, mov2xb));
                }
            }

            return pairs.stream();
        }

        protected Stream<GoalPair<G, Scamp5AnalogueTransformation<G>, Register>> getNaryOpStream(G upper) {
            if(this.scamp5AnalogueConfig.onlyMov){
                return Stream.empty();
            }
            ArrayList<GoalPair<G, Scamp5AnalogueTransformation<G>, Register>> pairs = new ArrayList<>();
            List<G> splits = upper.allSplits();
            splits.remove(0);
            int normal = splits.size();
            if(scamp5AnalogueConfig.subPowerOf2){
                Kernel3DGoal.Kernel3DGoalFactory<G> sub1 = upper.newFactory();
                Kernel3DGoal.Kernel3DGoalFactory<G> sub2 = upper.newFactory();
                boolean tryPower = false;
                Iterator<Tuple<Atom, Integer>> it = upper.uniqueCountIterator();
                while(it.hasNext()){
                    Tuple<Atom, Integer> t = it.next();
                    if(t.getB() == 7 || t.getB() == 14 || t.getB() == 15){
                        tryPower = true;
                        sub1.sub(t.getA().x,t.getA().y,t.getA().z, 2-(t.getB() & 1));
                        sub2.add(t.getA().x,t.getA().y,t.getA().z, t.getB()<<1 & ~t.getB());
                    } else {
                        sub1.add(t.getA().x,t.getA().y,t.getA().z,t.getA().positive?t.getB():-t.getB());
                    }
                }
                if(tryPower){
                    splits.add(sub1.get());
                    splits.add(sub2.get());
                }
            }
            Set<G> seen = new HashSet<>();
            for (int i = 0; i < splits.size(); i++) {
                G a = splits.get(i);
                boolean skipAdd_2 = false;
                if (seen.contains(a)) {
                    skipAdd_2 = true;
                }
                G b;
                if(i<normal){
                    b = upper.without(a);
                } else {
                    b = upper.subtracted(a);
                }
                seen.add(b);

                if (a.allZero() || b.allZero()) {
                    continue;
                }
                if (!skipAdd_2) {
                    if(scamp5AnalogueConfig.useAdd) {
                        Add_2<G> add = new Add_2<>(a, b, scamp5AnalogueConfig);
                        pairs.add(new GoalPair<>(upper, Arrays.asList(a, b), add));
                    }
                    if (scamp5AnalogueConfig.useAddx) {
                        for (Dir dir : Dir.values()) {
                            G movA = a.translated(-dir.x, -dir.y, 0);
                            G movB = b.translated(-dir.x, -dir.y, 0);
                            Addx<G> addx = new Addx<>(movA, movB, dir, scamp5AnalogueConfig);
                            pairs.add(new GoalPair<>(upper, Arrays.asList(movA, movB), addx));
                        }
                    }
                    if (scamp5AnalogueConfig.useAdd2x) {
                        for (Dir dir1 : Dir.values()) {
                            G aMovA = a.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                            G bMovA = b.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                            Add2x<G> add2xa = new Add2x<>(aMovA, bMovA, dir1, dir1, scamp5AnalogueConfig);
                            pairs.add(new GoalPair<>(upper, Arrays.asList(aMovA, bMovA), add2xa));
                            Dir dir2 = dir1.cw();
                            G aMovB = a.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                            G bMovB = b.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                            Add2x<G> add2xb = new Add2x<>(aMovB, bMovB, dir1, dir2, scamp5AnalogueConfig);
                            pairs.add(new GoalPair<>(upper, Arrays.asList(aMovB, bMovB), add2xb));
                        }
                    }
                }

                G negB = b.negated();
                if(scamp5AnalogueConfig.useSub) {
                    Sub<G> sub = new Sub<>(a, negB, scamp5AnalogueConfig);
                    pairs.add(new GoalPair<>(upper, Arrays.asList(a, negB), sub));
                }
                if (scamp5AnalogueConfig.useSubx) {
                    for (Dir dir : Dir.values()) {
                        G movA = a.translated(-dir.x, -dir.y, 0);
                        Subx<G> subx = new Subx<>(movA, negB, dir, scamp5AnalogueConfig);
                        pairs.add(new GoalPair<>(upper, Arrays.asList(movA, negB), subx));
                    }
                }
                if (scamp5AnalogueConfig.useSub2x) {
                    for (Dir dir1 : Dir.values()) {
                        G aMovA = a.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                        Sub2x<G> sub2xa = new Sub2x<>(aMovA, negB, dir1, dir1, scamp5AnalogueConfig);
                        pairs.add(new GoalPair<>(upper, Arrays.asList(aMovA, negB), sub2xa));
                        Dir dir2 = dir1.cw();
                        G aMovB = a.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                        Sub2x<G> sub2xb = new Sub2x<>(aMovB, negB, dir1, dir2, scamp5AnalogueConfig);
                        pairs.add(new GoalPair<>(upper, Arrays.asList(aMovB, negB), sub2xb));
                    }
                }

                if (scamp5AnalogueConfig.useAdd3) {
                    Set<G> subSeen = new HashSet<>();

                    Collection<G> subSplits = a.allSplits();
                    for (G aa : subSplits) {
                        boolean skipAdd_3 = false;
                        if (subSeen.contains(aa)) {
                            skipAdd_3 = true;
                        }
                        G ab = a.without(aa);
                        subSeen.add(ab);
                        if (aa.allZero() || ab.allZero()) {
                            continue;
                        }
                        if (!skipAdd_3) {
                            Add_3<G> add = new Add_3<>(aa, ab, b, scamp5AnalogueConfig);
                            pairs.add(new GoalPair<>(upper, Arrays.asList(aa, ab, b), add));
                        }
                    }
                }
            }

            return pairs.stream();
        }
    }

    public static class AnalogueAtomDistancePairGen<G extends Kernel3DGoal<G>> extends AtomDistancePairGen<G, Scamp5AnalogueTransformation<G>, Register> {
        final Scamp5AnalogueConfig scamp5AnalogueConfig;

        public AnalogueAtomDistancePairGen(GoalBag<G> goals, Context<G, Scamp5AnalogueTransformation<G>, Register> context, Scamp5AnalogueConfig scamp5AnalogueConfig) {
            super(goals, context, true);
            this.scamp5AnalogueConfig = scamp5AnalogueConfig;
        }

        private AnalogueAtomDistancePairGen(GoalBag<G> goals, Context<G, Scamp5AnalogueTransformation<G>, Register> context, Scamp5AnalogueConfig scamp5AnalogueConfig, Iterator<Tuple<Integer, Integer>> ijGetter) {
            super(goals, context, ijGetter, true);
            this.scamp5AnalogueConfig = scamp5AnalogueConfig;
        }

        @Override
        protected void addAtomDistancePairs(Item item, List<Item> outList) {
            Distance inverse = item.distance.inverse();
            G tmpMov = item.to.translated(inverse.x, inverse.y, inverse.z);
            G tmp = tmpMov;
            if(item.negate){
                tmp = tmpMov.negated();
            }
            if(tmp.same(item.a)){
                if(scamp5AnalogueConfig.useMov2x && item.distance.manhattanXY()>1){
                    //mov2x
                    SimpleTransformation.Direction d1 = item.distance.majorXYDirection();
                    SimpleTransformation.Direction d2 = item.distance.then(d1.opposite()).majorXYDirection();
                    Dir dir1 = Dir.fromDirection(d1).opposite();
                    Dir dir2 = Dir.fromDirection(d2).opposite();
                    Mov2x<G> mov2x = new Mov2x<>(item.a, dir1, dir2, true, scamp5AnalogueConfig);
                    Item newItem = new Item(item, new GoalPair<>(item.a, mov2x.a, mov2x));
                    outList.add(newItem);
                }
                if (scamp5AnalogueConfig.useMovx && item.distance.manhattanXY() > 0){
                    //movx
                    SimpleTransformation.Direction d1 = item.distance.majorXYDirection();
                    Dir dir1 = Dir.fromDirection(d1).opposite();
                    Movx<G> movx = new Movx<>(item.a, dir1, true, scamp5AnalogueConfig);
                    Item newItem = new Item(item, new GoalPair<>(item.a, movx.a, movx));
                    outList.add(newItem);
                } else if(scamp5AnalogueConfig.useNeg && item.negate){
                    Item newItem = new Item(item, new GoalPair<>(item.a, item.to, new Neg<>(item.to, scamp5AnalogueConfig)));
                    outList.add(newItem);
                }
            } else if (!scamp5AnalogueConfig.onlyMov){
                G aWithoutTmp = item.a.without(tmp);

                //Add_2
                if(scamp5AnalogueConfig.useAdd) {
                    G split2 = aWithoutTmp;
                    List<G> lowers = Arrays.asList(tmp, split2);
                    Item newItem = new Item(item, new GoalPair<>(item.a, lowers, new Add_2<>(tmp, split2, scamp5AnalogueConfig)));
                    outList.add(newItem);
                }

                //Sub
                if(scamp5AnalogueConfig.useSub) {
                    G split2 = aWithoutTmp.negated();
                    List<G> lowers = Arrays.asList(tmp, split2);
                    Item newItem = new Item(item, new GoalPair<>(item.a, lowers, new Sub<>(tmp, split2, scamp5AnalogueConfig)));
                    outList.add(newItem);
                }
                //TODO add_3 support?

                //addx
                if(scamp5AnalogueConfig.useAddx && item.distance.manhattanXY()>0){
                    Dir dir1 = Dir.fromDirection(item.distance.majorXYDirection()).opposite();
                    G split1 = aWithoutTmp.translated(-dir1.x, -dir1.y, 0);
                    G split2 = tmp.translated(-dir1.x, -dir1.y, 0);
                    Addx<G> addx = new Addx<>(split1, split2, dir1, scamp5AnalogueConfig);
                    Item newItem = new Item(item, new GoalPair<>(item.a, Arrays.asList(split1, split2), addx));
                    outList.add(newItem);
                }

                //add2x
                if(scamp5AnalogueConfig.useAdd2x && item.distance.manhattanXY()>1){
                    SimpleTransformation.Direction d1 = item.distance.majorXYDirection();
                    SimpleTransformation.Direction d2 = item.distance.then(d1.opposite()).majorXYDirection();
                    Dir dir1 = Dir.fromDirection(d1).opposite();
                    Dir dir2 = Dir.fromDirection(d2).opposite();
                    G split1 = aWithoutTmp.translated(-dir1.x -dir2.x, -dir1.y-dir2.y, 0);
                    G split2 = tmp.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
                    Add2x<G> add2x = new Add2x<>(split1, split2, dir1, dir2, scamp5AnalogueConfig);
                    Item newItem = new Item(item, new GoalPair<>(item.a, Arrays.asList(split1, split2), add2x));
                    
                    outList.add(newItem);
                }

                //Subx
                if(scamp5AnalogueConfig.useSubx && item.distance.manhattanXY()>0){
                    Dir dir1 = Dir.fromDirection(item.distance.majorXYDirection()).opposite();
                    G split1 = tmp.translated(-dir1.x, -dir1.y, 0);
                    G split2 = aWithoutTmp.negated();
                    Subx<G> subx = new Subx<>(split1, split2, dir1, scamp5AnalogueConfig);
                    Item newItem = new Item(item, new GoalPair<>(item.a, Arrays.asList(split1, split2), subx));
                    outList.add(newItem);
                }

                //Sub2x
                if(scamp5AnalogueConfig.useSub2x && item.distance.manhattanXY()>1){
                    SimpleTransformation.Direction d1 = item.distance.majorXYDirection();
                    SimpleTransformation.Direction d2 = item.distance.then(d1.opposite()).majorXYDirection();
                    Dir dir1 = Dir.fromDirection(d1).opposite();
                    Dir dir2 = Dir.fromDirection(d2).opposite();
                    G split1 = tmp.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
                    G split2 = aWithoutTmp.negated();
                    Sub2x<G> sub2x = new Sub2x<>(split1, split2, dir1, dir2, scamp5AnalogueConfig);
                    Item newItem = new Item(item, new GoalPair<>(item.a, Arrays.asList(split1, split2), sub2x));
                    outList.add(newItem);
                }
            }
        }

        @Override
        protected void addAtomDistanceDiagonalPairs(Item item, List<Item> outList) {
            if(scamp5AnalogueConfig.onlyMov){
                return;
            }
            Distance centre = new Distance(item.a.getAveragePos());
            G aWithoutTo = item.a.without(item.to);
            //add_2, sub
            if(!item.negate) {
                if(scamp5AnalogueConfig.useAdd) {
                    G split1 = aWithoutTo;
                    G split2 = item.to;
                   Item newItem = new Item(item, new GoalPair<>(item.a, Arrays.asList(split1, split2), new Add_2<>(split1, split2, scamp5AnalogueConfig)));
                    outList.add(newItem);
                }
            } else {
                if(scamp5AnalogueConfig.useSub) {
                    G split1 = aWithoutTo;
                    G split2 = item.to.negated();
                   Item newItem = new Item(item, new GoalPair<>(item.a, Arrays.asList(split1, split2), new Sub<>(split1, split2, scamp5AnalogueConfig)));
                    outList.add(newItem);
                }
            }

            if (scamp5AnalogueConfig.useAdd3) {
                Distance inverse = item.distance.inverse();
                G tmpMov = item.to.translated(inverse.x, inverse.y, inverse.z);
                G tmp = tmpMov;
                if (item.negate) {
                    tmp = tmpMov.negated();
                }
                tmp = tmp.without(item.to);
                G split1 = aWithoutTo.without(tmp);
                if(!split1.allZero() && !tmp.allZero()) {
                    G split2 = tmp;
                    G split3 = item.to;
                   Item newItem = new Item(item, new GoalPair<>(item.a, Arrays.asList(split1, split2, split3), new Add_3<>(split1, split2, split3, scamp5AnalogueConfig)));
                    outList.add(newItem);
                }
            }
            if(scamp5AnalogueConfig.useAddx && centre.manhattanXY()>0){
                Dir dir1 = Dir.fromDirection(centre.majorXYDirection());
                G split1 = aWithoutTo.translated(-dir1.x, -dir1.y, 0);
                G split2 = item.to.translated(-dir1.x, -dir1.y, 0);
                Addx<G> addx = new Addx<>(split1, split2, dir1, scamp5AnalogueConfig);
                Item newItem = new Item(item, new GoalPair<>(item.a, Arrays.asList(split1, split2), addx));
                outList.add(newItem);
            }

            //add2x
            if(scamp5AnalogueConfig.useAdd2x && centre.manhattanXY()>1){
                SimpleTransformation.Direction d1 = centre.majorXYDirection();
                SimpleTransformation.Direction d2 = centre.then(d1.opposite()).majorXYDirection();
                Dir dir1 = Dir.fromDirection(d1);
                Dir dir2 = Dir.fromDirection(d2);
                G split1 = aWithoutTo.translated(-dir1.x -dir2.x, -dir1.y-dir2.y, 0);
                G split2 = item.to.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
                Add2x<G> add2x = new Add2x<>(split1, split2, dir1, dir2, scamp5AnalogueConfig);
                Item newItem = new Item(item, new GoalPair<>(item.a, Arrays.asList(split1, split2), add2x));
                outList.add(newItem);
            }

            if(scamp5AnalogueConfig.useSubx && item.distance.manhattanXY()>0){
                Dir dir1 = Dir.fromDirection(item.distance.majorXYDirection()).opposite();
                G split1 = aWithoutTo.translated(-dir1.x, -dir1.y, 0);
                G split2 = item.to.negated();
                Item newItem = new Item(item, new GoalPair<>(item.a, Arrays.asList(split1, split2), new Subx<>(split1, split2, dir1, scamp5AnalogueConfig)));
                outList.add(newItem);
            }

            if(scamp5AnalogueConfig.useSub2x && item.distance.manhattanXY()>1){
                SimpleTransformation.Direction d1 = item.distance.majorXYDirection();
                SimpleTransformation.Direction d2 = item.distance.then(d1.opposite()).majorXYDirection();
                Dir dir1 = Dir.fromDirection(d1).opposite();
                Dir dir2 = Dir.fromDirection(d2).opposite();
                G split1 = aWithoutTo.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
                G split2 = item.to.negated();
                Item newItem = new Item(item, new GoalPair<>(item.a, Arrays.asList(split1, split2), new Sub2x<>(split1, split2, dir1, dir2, scamp5AnalogueConfig)));
                outList.add(newItem);
            }
        }

        @SuppressWarnings("WeakerAccess")
        protected void addDirectTransformation(G a, List<Item> outList) {
            Distance centre = new Distance(a.getAveragePos());
            if(scamp5AnalogueConfig.useMovx && centre.manhattanXY()>0){
                SimpleTransformation.Direction d1 = centre.majorXYDirection();
                if(d1!= null) {
                    Dir dir1 = Dir.fromDirection(d1);
                    Movx<G> movx = new Movx<>(a, dir1, true, scamp5AnalogueConfig);
                    Item newItem = new Item(a, movx.a, movx, new Distance(d1, 1), false);
                    outList.add(newItem);
                }
            }
            if(scamp5AnalogueConfig.useMov2x && centre.manhattanXY()>1){
                SimpleTransformation.Direction d1 = centre.majorXYDirection();
                if(d1 != null) {
                    SimpleTransformation.Direction d2 = centre.then(d1.opposite()).majorXYDirection();
                    if (d2 != null) {
                        Dir dir1 = Dir.fromDirection(d1).opposite();
                        Dir dir2 = Dir.fromDirection(d2).opposite();
                        Mov2x<G> mov2x = new Mov2x<>(a, dir1, dir2, true, scamp5AnalogueConfig);
                        Item newItem = new Item(a, mov2x.a, mov2x, new Distance(d1, 1).then(d2), false);
                        outList.add(newItem);
                    }
                }
            }
            if (scamp5AnalogueConfig.useDivq) {
                for (G initialGoal : context.initialGoals) {
                    if (initialGoal.hasSubGoal(a)) {
                        G l = a.added(a);
                        Item newItem = new Item(a, l, new Divq<>(l, scamp5AnalogueConfig));
                        outList.add(newItem);
                    }
                }
            }
        }

    }

    public static class AnalogueAtomDistanceSortedPairGen<G extends Kernel3DGoal<G>> extends AnalogueAtomDistancePairGen<G> {

        private final CostHeuristic<G, Scamp5AnalogueTransformation<G>, Register> heuristic;

        public AnalogueAtomDistanceSortedPairGen(GoalBag<G> goals, Context<G, Scamp5AnalogueTransformation<G>, Register> conf, Scamp5AnalogueConfig scamp5Config, CostHeuristic<G, Scamp5AnalogueTransformation<G>, Register> heuristic) {
            super(goals, conf, scamp5Config, new PlainCombinationIterator(goals.size()));
            this.heuristic = heuristic;
        }

        @Override
        protected void fillCurrentList() {
            if(!currentList.isEmpty()){
                return;
            }
            List<Item> outList = new ArrayList<>();
            while (ijGetter.hasNext()){
                Tuple<Integer, Integer> ij = ijGetter.next();
                G a = goals.get(ij.getA());
                G b = goals.get(ij.getB());
                boolean diagonal = ij.getA().equals(ij.getB());
                List<Item> inList = getAtomDistanceList2D(a, b, diagonal);
                //inList.sort(atomDistanceComparator);
                addPairs(a, diagonal, inList, outList);
            }
            outList.parallelStream().forEach(item -> item.cost = heuristic.getCost(item.pair, goals, this.context));
            outList.removeIf(item -> item.cost < 0);
            outList.sort(Comparator.comparingDouble((Item item) -> item.cost).reversed());
            currentList = outList.stream().map(item -> item.pair).collect(Collectors.toList());

        }
    }

}
