package uk.co.edstow.cain.scamp5.superPixel;

import uk.co.edstow.cain.goals.BankedKernel3DGoal;
import uk.co.edstow.cain.goals.atomGoal.pairGen.Distance;
import uk.co.edstow.cain.goals.atomGoal.pairGen.SimpleTransformation;
import uk.co.edstow.cain.pairgen.*;
import uk.co.edstow.cain.regAlloc.BRegister;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.co.edstow.cain.scamp5.superPixel.Scamp5SuperPixelTransformation.*;

public class Scamp5SuperPixelPairGens {

    public static class ExhaustivePairGen<G extends BankedKernel3DGoal<G>> extends uk.co.edstow.cain.pairgen.ExhaustivePairGen<G, Scamp5SuperPixelTransformation<G>, BRegister> {

        Scamp5SuperPixelConfig scamp5SuperPixelConfig;
        public ExhaustivePairGen(GoalBag<G> goals, Context<G, Scamp5SuperPixelTransformation<G>, BRegister> context, Scamp5SuperPixelConfig scamp5SuperPixelConfig, CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> heuristic) {
            super(goals, context, heuristic);
            this.scamp5SuperPixelConfig = scamp5SuperPixelConfig;
        }

        protected Stream<GoalPair<G,Scamp5SuperPixelTransformation<G>, BRegister>> getUnaryOpStream(G upper) {
            ArrayList<GoalPair<G,Scamp5SuperPixelTransformation<G>, BRegister>> pairs = new ArrayList<>();
            if(!scamp5SuperPixelConfig.onlyMov()) {

                if (upper.allZero()) {
                    if(scamp5SuperPixelConfig.useRes) {
                        Res<G> res = new Res<>(Collections.singletonList(upper), scamp5SuperPixelConfig);
                        pairs.add(new GoalPair<>(upper, Collections.emptyList(), res));

                    }
                }

                //Divide
                if(scamp5SuperPixelConfig.useDiv) {
                    Div<G> div = new Div<>(upper, true, scamp5SuperPixelConfig);
                    pairs.add(new GoalPair<>(upper, div.a, div));
                }

                //Add self (mul2)
                if(scamp5SuperPixelConfig.useAddSelf) {
                    AddSelf<G> add = new AddSelf<>(upper, true, scamp5SuperPixelConfig);
                    if(add.isPossible()) {
                        pairs.add(new GoalPair<>(upper, add.a, add));
                    }
                }
            }

            //Move x
            if(scamp5SuperPixelConfig.useMovbx) {
                for (int i = 0; i < scamp5SuperPixelConfig.banks; i++) {
                    if (scamp5SuperPixelConfig.isBankSameShape(upper.getBank(), i)) {
                        for (int x = -3; x < 4; x++) {
                            for (int y = -3; y < 4; y++) {
                                Movxb<G> movx = new Movxb<>(upper, x, y, i, scamp5SuperPixelConfig);
                                pairs.add(new GoalPair<>(upper, movx.lower, movx));
                            }
                        }
                    }
                }
            }

            return pairs.stream();
        }

        protected Stream<GoalPair<G,Scamp5SuperPixelTransformation<G>, BRegister>> getNaryOpStream(G upper) {
            if(this.scamp5SuperPixelConfig.onlyMov()){
                return Stream.empty();
            }
            ArrayList<GoalPair<G,Scamp5SuperPixelTransformation<G>, BRegister>> pairs = new ArrayList<>();
            List<G> splits = upper.allSplits();
            int normal = splits.size();

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
                    if(scamp5SuperPixelConfig.useAdd) {
                        Add_2<G> add = new Add_2<>(a, b, scamp5SuperPixelConfig);
                        pairs.add(new GoalPair<>(upper, Arrays.asList(a, b), add));
                    }
                }
                G negB = b.negated();
                if(scamp5SuperPixelConfig.useSub) {
                    Sub_2<G> sub = new Sub_2<>(a, negB, scamp5SuperPixelConfig);
                    pairs.add(new GoalPair<>(upper, Arrays.asList(a, negB), sub));
                }
            }

            return pairs.stream();
        }
    }


    public static class SuperPixelAtomDistancePairGen<G extends BankedKernel3DGoal<G>> extends AtomDistancePairGen<G, Scamp5SuperPixelTransformation<G>, BRegister> {
        final Scamp5SuperPixelConfig scamp5SuperPixelConfig;



        public SuperPixelAtomDistancePairGen(GoalBag<G> goals, Context<G, Scamp5SuperPixelTransformation<G>, BRegister> context, Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
            super(goals, context);
            this.scamp5SuperPixelConfig = scamp5SuperPixelConfig;
        }

        private SuperPixelAtomDistancePairGen(GoalBag<G> goals, Context<G, Scamp5SuperPixelTransformation<G>, BRegister> context, Scamp5SuperPixelConfig scamp5SuperPixelConfig, Iterator<Tuple<Integer, Integer>> ijGetter) {
            super(goals, context, ijGetter);
            this.scamp5SuperPixelConfig = scamp5SuperPixelConfig;
        }
        

        @Override
        protected void addAtomDistancePairs(AtomDistancePairGen<G, Scamp5SuperPixelTransformation<G>, BRegister>.Item item, List<AtomDistancePairGen<G, Scamp5SuperPixelTransformation<G>, BRegister>.Item> outList) {
            Distance inverse = item.distance.inverse();
            G tmpMov = item.to.translated(inverse.x, inverse.y, inverse.z);
            G tmp = tmpMov;
            if(item.negate){
                tmp = tmpMov.negated();
            }
            if(tmp.same(item.a)){
                if (scamp5SuperPixelConfig.useMovbx && item.distance.manhattanXY() > 0){
                    //movx
                    SimpleTransformation.Direction d1 = item.distance.majorXYDirection();
                    Dir dir1 = Dir.fromDirection(d1);
                    for (int bank = 0; bank < scamp5SuperPixelConfig.banks; bank++) {
                        if (scamp5SuperPixelConfig.isBankSameShape(item.a.getBank(), bank)) {
                            Movxb<G> movx = new Movxb<>(item.a, dir1.x, dir1.y, bank, scamp5SuperPixelConfig);
                            Item newItem = new Item(item, new GoalPair<>(item.a, movx.lower, movx));
                            outList.add(newItem);
                        }
                    }
                }
            } else if (!scamp5SuperPixelConfig.onlyMov()){
                G aWithoutTmp = item.a.without(tmp);

                //Add_2
                if(scamp5SuperPixelConfig.useAdd) {
                    G split2 = aWithoutTmp;
                    List<G> lowers = Arrays.asList(tmp, split2);
                    Item newItem = new Item(item, new GoalPair<>(item.a, lowers, new Add_2<>(tmp, split2, scamp5SuperPixelConfig)));
                    outList.add(newItem);
                }
            }
        }

        @Override
        protected void addAtomDistanceDiagonalPairs(AtomDistancePairGen<G, Scamp5SuperPixelTransformation<G>, BRegister>.Item item, List<AtomDistancePairGen<G, Scamp5SuperPixelTransformation<G>, BRegister>.Item> outList) {
            if(scamp5SuperPixelConfig.onlyMov()){
                return;
            }
            Distance centre = new Distance(item.a.getAveragePos());
            G aWithoutTo = item.a.without(item.to);
            //add_2, sub
            if(!item.negate) {
                if(scamp5SuperPixelConfig.useAdd) {
                    G split1 = aWithoutTo;
                    G split2 = item.to;
                    Item newItem = new Item(item, new GoalPair<>(item.a, Arrays.asList(split1, split2), new Add_2<>(split1, split2, scamp5SuperPixelConfig)));
                    outList.add(newItem);
                }
            } else {
                if(scamp5SuperPixelConfig.useSub) {
                    G split1 = aWithoutTo;
                    G split2 = item.to.negated();
                    Item newItem = new Item(item, new GoalPair<>(item.a, Arrays.asList(split1, split2), new Sub_2<>(split1, split2, scamp5SuperPixelConfig)));
                    outList.add(newItem);
                }
            }
        }

        @Override
        protected void addDirectTransformation(G a, List<AtomDistancePairGen<G, Scamp5SuperPixelTransformation<G>, BRegister>.Item> outList) {
            Distance centre = new Distance(a.getAveragePos());
            if(scamp5SuperPixelConfig.useMovbx && centre.manhattanXY()>0){
                SimpleTransformation.Direction d1 = centre.majorXYDirection();
                if(d1!= null) {
                    Dir dir1 = Dir.fromDirection(d1).opposite();
                    for (int bank = 0; bank < scamp5SuperPixelConfig.banks; bank++) {
                        if (scamp5SuperPixelConfig.isBankSameShape(a.getBank(), bank)) {
                            Movxb<G> movx = new Movxb<>(a, dir1.x, dir1.y, bank, scamp5SuperPixelConfig);
                            Item newItem = new Item(a, movx.lower, movx);
                            outList.add(newItem);
                        }
                    }
                }
            }

            if (scamp5SuperPixelConfig.useDiv) {
                for (G initialGoal : context.initialGoals) {
                    if (initialGoal.hasSubGoal(a)) {
                        G l = a.added(a);
                        Item newItem = new Item(a, l, new Div<>(l, scamp5SuperPixelConfig));
                        outList.add(newItem);
                    }
                }
            }
        }
    }

    public static class SuperPixelAtomDistanceSortedPairGen<G extends BankedKernel3DGoal<G>> extends SuperPixelAtomDistancePairGen<G> {

        private final CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> huristic;

        public SuperPixelAtomDistanceSortedPairGen(GoalBag<G> goals, Context<G, Scamp5SuperPixelTransformation<G>, BRegister> conf, Scamp5SuperPixelConfig scamp5Config, CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> huristic) {
            super(goals, conf, scamp5Config, new PlainCombinationIterator(goals.size()));
            this.huristic = huristic;
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
                List<Item> inList = getAtomDistanceList(a, b, diagonal);
                //inList.sort(atomDistanceComparator);
                addPairs(a, diagonal, inList, outList);
            }
            outList.parallelStream().forEach(item -> item.cost = huristic.getCost(item.pair, goals, this.context));
            outList.removeIf(item -> item.cost < 0);
            outList.sort(Comparator.comparingDouble((Item item) -> item.cost).reversed());
            currentList = outList.stream().map(item -> item.pair).collect(Collectors.toList());
        }
    }
}
