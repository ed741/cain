package uk.co.edstow.cain.scamp5.digital;

import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.atomGoal.pairGen.Distance;
import uk.co.edstow.cain.goals.atomGoal.pairGen.SimpleTransformation;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.pairgen.PairGen;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.scamp5.Scamp5ConfigGetter;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.co.edstow.cain.scamp5.digital.Scamp5DigitalTransformation.*;

public class Scamp5DigitalPairGenFactory<G extends Kernel3DGoal<G>> implements PairGenFactory<G, Scamp5DigitalTransformation<G>, Register> {

    private final Scamp5ConfigGetter<G, Scamp5DigitalTransformation<G>, Register, Scamp5DigitalConfig<G>> scamp5ConfGet;

    public Scamp5DigitalPairGenFactory(Scamp5ConfigGetter<G, Scamp5DigitalTransformation<G>, Register, Scamp5DigitalConfig<G>> confGetter) {
        this.scamp5ConfGet = confGetter;
    }


    @Override
    public List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> solveDirectly(Context<G, Scamp5DigitalTransformation<G>, Register> context, GoalBag<G> goals){
        Scamp5DigitalConfig<G> scamp5DigitalConfig = this.scamp5ConfGet.getScamp5ConfigForDirectSolve(goals, context);
        goals = new GoalBag<>(goals);
        List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> allPairs = new ArrayList<>();
        GoalBag<G> empties = new GoalBag<>();
        for (int i = goals.size() - 1; i >= 0; i--) {
          if(goals.get(i).allZero()){
              empties.add(goals.remove(i));
          }
        }
        if(!scamp5DigitalConfig.onlyMov()) {
            for (int i = 0; i < empties.size(); i++) {
                if (scamp5DigitalConfig.useRes2 && i + 1 < empties.size()) {
                    allPairs.add(new GoalPair<>(Arrays.asList(empties.get(i), empties.get(i + 1)), Collections.emptyList(), new Res_2<>(empties.get(i), empties.get(i + 1), scamp5DigitalConfig)));
                    i++;
                } else if (scamp5DigitalConfig.useRes) {
                    allPairs.add(new GoalPair<>(empties.get(i), Collections.emptyList(), new Res<>(empties.get(i), scamp5DigitalConfig)));
                }
            }
        }
        List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> pairList = PairGenFactory.super.solveDirectly(context, goals);
        if(pairList==null){
            return null;
        }
        allPairs.addAll(pairList);
        return allPairs;
    }

    @Override
    public Collection<Tuple<List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>>, G>> solveDirectly(Context<G, Scamp5DigitalTransformation<G>, Register> context, G goal) {
        Scamp5DigitalConfig<G> scamp5DigitalConfig = this.scamp5ConfGet.getScamp5ConfigForDirectSolve(new GoalBag<>(goal), context);
        ArrayList<Tuple<List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>>, G>> list = new ArrayList<>();

        if(context.initialGoals.contains(goal)){
            list.add(new Tuple<>(Collections.emptyList(), goal));
            return list;
        }

        //Res
        if(scamp5DigitalConfig.useRes && goal.allZero()){
            Res<G> res = new Res<>(goal, scamp5DigitalConfig);
            list.add(new Tuple<>(Collections.singletonList((new GoalPair<>(goal, Collections.emptyList(), res))), context.initialGoals.get(0)));
            return list;
        }

        //Divide
        if(scamp5DigitalConfig.useDiv) {
            try {
                if (goal.total() == 1d && goal.get(0, 0 ,goal.bounds().getZMax())==1) {
                    int z = goal.bounds().getZMax();
                    G ic = null;
                    for (G i : context.initialGoals) {
                        if (i.get(0,0,z)>0) {
                            ic = i;
                        }
                    }
                    if (ic != null) {
                        List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> pairs = new ArrayList<>();
                        Div<G> div = new Div<>(goal, true, scamp5DigitalConfig);
                        pairs.add(new GoalPair<>(div.applyOpForwards(), Collections.singletonList(div.a), div));
                        while (div.a.totalI() < ic.totalI()) {
                            div = new Div<>(div.a, true, scamp5DigitalConfig);
                            pairs.add(new GoalPair<>(div.applyOpForwards(), Collections.singletonList(div.a), div));
                        }
                        if (div.a.equals(ic)) {
                            list.add(new Tuple<>(pairs, ic));
                        }
                    }
                }

            } catch (TransformationApplicationException e) {
                e.printStackTrace();
                // Should be unreachable
                assert false;
            }
        }

        // AddSelf (mul2)
        if(scamp5DigitalConfig.useAddSelf) {
            AddSelf<G> addSelf = new AddSelf<>(goal, true, scamp5DigitalConfig);
            if(addSelf.isPossible()) {
                list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, addSelf.a, addSelf)), addSelf.a));
            }
        }

        //Move x
        if(scamp5DigitalConfig.useMovx) {
            for (Dir dir : Dir.values()) {
                Movx<G> movx = new Movx<>(goal, dir, true, scamp5DigitalConfig);
                list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, movx.a, movx)), movx.a));
            }
        }

        return list;
    }





    @Override
    public PairGen<G, Scamp5DigitalTransformation<G>, Register> generatePairs(GoalBag<G> goals, Context<G, Scamp5DigitalTransformation<G>, Register> context) {
        return this.scamp5ConfGet.getScamp5Strategy(goals, context);
    }

    @Override
    public Scamp5DigitalTransformation<G> getDummyTransformation(List<G> upperGoals, List<G> lowerGoals, Context<G, Scamp5DigitalTransformation<G>, Register> context) {
        return new Scamp5DigitalTransformation.Null<>(lowerGoals.size(), upperGoals.size(), this.scamp5ConfGet.getScamp5ConfigForDirectSolve(new GoalBag<>(lowerGoals), context));
    }

    public static class ExhaustivePairGen<G extends Kernel3DGoal<G>> extends uk.co.edstow.cain.pairgen.ExhaustivePairGen<G, Scamp5DigitalTransformation<G>, Register> {

        Scamp5DigitalConfig<G> scamp5DigitalConfig;
        public ExhaustivePairGen(GoalBag<G> goals, Context<G, Scamp5DigitalTransformation<G>, Register> context, Scamp5DigitalConfig<G> scamp5DigitalConfig, CostHeuristic<G, Scamp5DigitalTransformation<G>, Register> huristic) {
            super(goals, context, huristic);
            this.scamp5DigitalConfig = scamp5DigitalConfig;
        }

        protected Stream<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> getUnaryOpStream(G upper) {
            ArrayList<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> pairs = new ArrayList<>();
            if(!scamp5DigitalConfig.onlyMov()) {

                if (upper.allZero()) {
                    if(scamp5DigitalConfig.useRes) {
                        Res<G> res = new Res<>(upper, scamp5DigitalConfig);
                        pairs.add(new GoalPair<>(upper, Collections.emptyList(), res));

                    }
                    if(scamp5DigitalConfig.useRes2) {
                        G other = null;
                        for (G g : goals) {
                            if (g.allZero() && !g.equivalent(upper)) {
                                other = g;
                                break;
                            }
                        }
                        if (other != null) {
                            Res_2<G> res_2 = new Res_2<>(upper, other, scamp5DigitalConfig);
                            pairs.add(new GoalPair<>(Arrays.asList(upper, other), Collections.emptyList(), res_2));
                        }
                    }
                }

                //Divide
                if(scamp5DigitalConfig.useDiv) {
                    Div<G> div = new Div<>(upper, true, scamp5DigitalConfig);
                    pairs.add(new GoalPair<>(upper, div.a, div));
                }

                //Add self (mul2)
                if(scamp5DigitalConfig.useAddSelf) {
                    AddSelf<G> add = new AddSelf<>(upper, true, scamp5DigitalConfig);
                    if(add.isPossible()) {
                        pairs.add(new GoalPair<>(upper, add.a, add));
                    }
                }
            }

            //Mov
            if(scamp5DigitalConfig.useMov) {
                Mov<G> mov = new Mov<>(upper, true, scamp5DigitalConfig);
                pairs.add(new GoalPair<>(upper, mov.a, mov));
            }

            //Move x
            if(scamp5DigitalConfig.useMovx) {
                for (Dir dir : Dir.values()) {
                    Movx<G> movx = new Movx<>(upper, dir, true, scamp5DigitalConfig);
                    pairs.add(new GoalPair<>(upper, movx.a, movx));
                }
            }

            return pairs.stream();
        }

        protected Stream<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> getNaryOpStream(G upper) {
            if(this.scamp5DigitalConfig.onlyMov()){
                return Stream.empty();
            }
            ArrayList<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> pairs = new ArrayList<>();
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
                    if(scamp5DigitalConfig.useAdd) {
                        Add_2<G> add = new Add_2<>(a, b, scamp5DigitalConfig);
                        pairs.add(new GoalPair<>(upper, Arrays.asList(a, b), add));
                    }
                }
            }

            return pairs.stream();
        }
    }

    public static class DigitalAtomDistancePairGen<G extends Kernel3DGoal<G>> extends uk.co.edstow.cain.pairgen.AtomDistancePairGen<G, Scamp5DigitalTransformation<G>, Register> {
        final Scamp5DigitalConfig<G> scamp5DigitalConfig;

        public DigitalAtomDistancePairGen(GoalBag<G> goals, Context<G, Scamp5DigitalTransformation<G>, Register> context, Scamp5DigitalConfig<G> scamp5DigitalConfig) {
            super(goals, context);
            this.scamp5DigitalConfig = scamp5DigitalConfig;
        }

        private DigitalAtomDistancePairGen(GoalBag<G> goals, Context<G, Scamp5DigitalTransformation<G>, Register> context, Scamp5DigitalConfig<G> scamp5DigitalConfig, Iterator<Tuple<Integer, Integer>> ijGetter) {
            super(goals, context, ijGetter);
            this.scamp5DigitalConfig = scamp5DigitalConfig;
        }

        @Override
        protected void addAtomDistancePairs(uk.co.edstow.cain.pairgen.AtomDistancePairGen<G, Scamp5DigitalTransformation<G>, Register>.Item item, List<uk.co.edstow.cain.pairgen.AtomDistancePairGen<G, Scamp5DigitalTransformation<G>, Register>.Item> outList) {
            Distance inverse = item.distance.inverse();
            G tmpMov = item.to.translated(inverse.x, inverse.y, inverse.z);
            G tmp = tmpMov;
            if(item.negate){
                tmp = tmpMov.negated();
            }
            if(tmp.same(item.a)){
                if (scamp5DigitalConfig.useMovx && item.distance.manhattanXY() > 0){
                    //movx
                    SimpleTransformation.Direction d1 = item.distance.majorXYDirection();
                    Dir dir1 = Dir.fromDirection(d1).opposite();
                    Movx<G> movx = new Movx<>(item.a, dir1, true, scamp5DigitalConfig);
                    Item newItem = new Item(item, new GoalPair<>(item.a, movx.a, movx));
                    outList.add(newItem);
                }
            } else if (!scamp5DigitalConfig.onlyMov()){
                G aWithoutTmp = item.a.without(tmp);

                //Add_2
                if(scamp5DigitalConfig.useAdd) {
                    G split2 = aWithoutTmp;
                    List<G> lowers = Arrays.asList(tmp, split2);
                    Item newItem = new Item(item, new GoalPair<>(item.a, lowers, new Add_2<>(tmp, split2, scamp5DigitalConfig)));
                    outList.add(newItem);
                }
            }
        }

        @Override
        protected void addAtomDistanceDiagonalPairs(uk.co.edstow.cain.pairgen.AtomDistancePairGen<G, Scamp5DigitalTransformation<G>, Register>.Item item, List<uk.co.edstow.cain.pairgen.AtomDistancePairGen<G, Scamp5DigitalTransformation<G>, Register>.Item> outList) {
            if(scamp5DigitalConfig.onlyMov()){
                return;
            }
            Distance centre = new Distance(item.a.getAveragePos());
            G aWithoutTo = item.a.without(item.to);
            //add_2, sub
            if(!item.negate) {
                if(scamp5DigitalConfig.useAdd) {
                    G split1 = aWithoutTo;
                    G split2 = item.to;
                    Item newItem = new Item(item, new GoalPair<>(item.a, Arrays.asList(split1, split2), new Add_2<>(split1, split2, scamp5DigitalConfig)));
                    outList.add(newItem);
                }
            }
        }

        @SuppressWarnings("WeakerAccess")
        @Override
        protected void addDirectTransformation(G a, List<Item> outList) {
            Distance centre = new Distance(a.getAveragePos());
            if(scamp5DigitalConfig.useMovx && centre.manhattanXY()>0){
                SimpleTransformation.Direction d1 = centre.majorXYDirection();
                if(d1!= null) {
                    Dir dir1 = Dir.fromDirection(d1);
                    Movx<G> movx = new Movx<>(a, dir1, true, scamp5DigitalConfig);
                    Item newItem = new Item(a, movx.a, movx);
                    outList.add(newItem);
                }
            }
            if (scamp5DigitalConfig.useDiv) {
                for (G initialGoal : context.initialGoals) {
                    if (initialGoal.hasSubGoal(a)) {
                        G l = a.added(a);
                        Item newItem = new Item(a, l, new Div<>(l, scamp5DigitalConfig));
                        outList.add(newItem);
                    }
                }
            }
        }
        
    }

    public static class DigitalAtomDistanceSortedPairGen<G extends Kernel3DGoal<G>, T extends Scamp5DigitalConfig<G>> extends DigitalAtomDistancePairGen<G> {

        private final CostHeuristic<G, Scamp5DigitalTransformation<G>, Register> heuristic;

        public DigitalAtomDistanceSortedPairGen(GoalBag<G> goals, Context<G, Scamp5DigitalTransformation<G>, Register> context, T scamp5Config, CostHeuristic<G, Scamp5DigitalTransformation<G>, Register> heuristic) {
            super(goals, context, scamp5Config, new uk.co.edstow.cain.pairgen.AtomDistancePairGen.PlainCombinationIterator(goals.size()));
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
                List<Item> inList = getAtomDistanceList(a, b, diagonal);
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
