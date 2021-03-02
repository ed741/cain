package uk.co.edstow.cain.scamp5.superPixel;

import uk.co.edstow.cain.goals.BankedKernel3DGoal;
import uk.co.edstow.cain.goals.atomGoal.pairGen.Distance;
import uk.co.edstow.cain.goals.atomGoal.pairGen.SimpleTransformation;
import uk.co.edstow.cain.nonlinear.LinearPairGenFactory;
import uk.co.edstow.cain.pairgen.AtomDistancePairGen;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.regAlloc.BRegister;
import uk.co.edstow.cain.scamp5.Scamp5ConfigGetter;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.co.edstow.cain.scamp5.superPixel.Scamp5SuperPixelTransformation.*;

public class Scamp5SuperPixelPairGenFactory<G extends BankedKernel3DGoal<G>> implements LinearPairGenFactory<G, Scamp5SuperPixelTransformation<G>, BRegister> {

    private final Scamp5ConfigGetter<G, Scamp5SuperPixelTransformation<G>, BRegister, Scamp5SuperPixelConfig<G>> scamp5ConfGet;

    public Scamp5SuperPixelPairGenFactory(Scamp5ConfigGetter<G, Scamp5SuperPixelTransformation<G>, BRegister, Scamp5SuperPixelConfig<G>> confGetter) {
        this.scamp5ConfGet = confGetter;
    }


    @Override
    public List<GoalPair<G, Scamp5SuperPixelTransformation<G>, BRegister>> applyAllUnaryOpForwards(List<G> initialGoals, Context<G, Scamp5SuperPixelTransformation<G>, BRegister> context, GoalBag<G> goals){
        Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig = this.scamp5ConfGet.getScamp5ConfigForDirectSolve(goals, context);
        goals = new GoalBag<>(goals);
        List<GoalPair<G, Scamp5SuperPixelTransformation<G>, BRegister>> allPairs = new ArrayList<>();
        List<G> empties = new ArrayList<>();
        for (int i = goals.size() - 1; i >= 0; i--) {
          if(goals.get(i).allZero()){
              empties.add(goals.remove(i));
          }
        }
        if(!scamp5SuperPixelConfig.onlyMov()) {
            if(scamp5SuperPixelConfig.useRes && !empties.isEmpty()) {
                allPairs.add(new GoalPair<>(empties, Collections.emptyList(), new Res<>(empties, scamp5SuperPixelConfig)));
            }
        }
        List<GoalPair<G,Scamp5SuperPixelTransformation<G>, BRegister>> pairList = LinearPairGenFactory.super.applyAllUnaryOpForwards(initialGoals, context, goals);
        if(pairList==null){
            return null;
        }
        allPairs.addAll(pairList);
        return allPairs;
    }

    @Override
    public Collection<Tuple<List<GoalPair<G, Scamp5SuperPixelTransformation<G>, BRegister>>, G>> applyAllUnaryOpForwards(List<G> initialGoals, Context<G, Scamp5SuperPixelTransformation<G>, BRegister> context, G goal) {
        Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig = this.scamp5ConfGet.getScamp5ConfigForDirectSolve(new GoalBag<>(goal), context);
        ArrayList<Tuple<List<GoalPair<G,Scamp5SuperPixelTransformation<G>, BRegister>>, G>> list = new ArrayList<>();

        if(initialGoals.contains(goal)){
            list.add(new Tuple<>(Collections.emptyList(), goal));
            return list;
        }

        //Res
        if(scamp5SuperPixelConfig.useRes && goal.allZero()){
            Res<G> res = new Res<>(Collections.singletonList(goal), scamp5SuperPixelConfig);
            list.add(new Tuple<>(Collections.singletonList((new GoalPair<>(goal, Collections.emptyList(), res))), initialGoals.get(0)));
            return list;
        }

        //Divide
        if(scamp5SuperPixelConfig.useDiv) {
            try {
                if (goal.total() == 1d && goal.get(0, 0 ,goal.bounds().getZMax())==1) {
                    int z = goal.bounds().getZMax();
                    G ic = null;
                    for (G i : initialGoals) {
                        if (i.get(0,0,z)>0) {
                            ic = i;
                        }
                    }
                    if (ic != null) {
                        List<GoalPair<G,Scamp5SuperPixelTransformation<G>, BRegister>> pairs = new ArrayList<>();
                        Div<G> div = new Div<>(goal, true, scamp5SuperPixelConfig);
                        pairs.add(new GoalPair<>(div.applyForwards(), Collections.singletonList(div.a), div));
                        while (div.a.totalI() < ic.totalI()) {
                            div = new Div<>(div.a, true, scamp5SuperPixelConfig);
                            pairs.add(new GoalPair<>(div.applyForwards(), Collections.singletonList(div.a), div));
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
        if(scamp5SuperPixelConfig.useAddSelf) {
            AddSelf<G> addSelf = new AddSelf<>(goal, true, scamp5SuperPixelConfig);
            if(addSelf.isPossible()) {
                list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, addSelf.a, addSelf)), addSelf.a));
            }
        }

        //Move x
        if(scamp5SuperPixelConfig.useMovbx) {
            for (Dir dir : Dir.values()) {
                for (int i = 0; i < scamp5SuperPixelConfig.banks; i++) {
                    Movxb<G> movx = new Movxb<>(goal, dir.x, dir.y, i, scamp5SuperPixelConfig);
                    list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, movx.lower, movx)), movx.lower));
                }
            }
        }

        return list;
    }





    @Override
    public PairGen<G, Scamp5SuperPixelTransformation<G>,BRegister> generatePairs(GoalBag<G> goals, Context<G, Scamp5SuperPixelTransformation<G>, BRegister> context) {
        return this.scamp5ConfGet.getScamp5Strategy(goals, context);
    }

    @Override
    public Scamp5SuperPixelTransformation<G> getDummyTransformation(List<G> upperGoals, List<G> lowerGoals, Context<G, Scamp5SuperPixelTransformation<G>, BRegister> context) {
        return new Scamp5SuperPixelTransformation.Null<>(lowerGoals.size(), upperGoals.size(), this.scamp5ConfGet.getScamp5ConfigForDirectSolve(new GoalBag<>(lowerGoals), context));
    }

    @Override
    public Collection<Tuple<List<G>, Scamp5SuperPixelTransformation<G>>> generateValueConstantOps(List<G> goal, Context<G, Scamp5SuperPixelTransformation<G>, BRegister> context) {
        PairGen<G, Scamp5SuperPixelTransformation<G>, BRegister> gen =  this.scamp5ConfGet.getScamp5Strategy(new GoalBag<G>(goal), context, true);
        return null; //TODO part of non-linear goals

    }


    public static class ExhaustivePairGen<G extends BankedKernel3DGoal<G>> extends uk.co.edstow.cain.pairgen.ExhaustivePairGen<G, Scamp5SuperPixelTransformation<G>, BRegister> {

        Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig;
        public ExhaustivePairGen(GoalBag<G> goals, Context<G, Scamp5SuperPixelTransformation<G>, BRegister> context, Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig, CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> heuristic) {
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

            //Mov
            if(scamp5SuperPixelConfig.useMovbx) {
                Movxb<G> mov = new Movxb<>(upper, upper.copy(), scamp5SuperPixelConfig);
                pairs.add(new GoalPair<>(upper, mov.lower, mov));
            }

            //Move x
            if(scamp5SuperPixelConfig.useMovbx) {
                for (int i = 0; i < scamp5SuperPixelConfig.banks; i++) {
                    for (int x = -3; x < 4; x++) {
                        for (int y = -3; y < 4; y++) {
                            Movxb<G> movx = new Movxb<>(upper, x, y, i, scamp5SuperPixelConfig);
                            pairs.add(new GoalPair<>(upper, movx.lower, movx));
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
            }

            return pairs.stream();
        }
    }


    public static class SuperPixelAtomDistancePairGen<G extends BankedKernel3DGoal<G>> extends AtomDistancePairGen<G, Scamp5SuperPixelTransformation<G>, BRegister> {
        final Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig;



        public SuperPixelAtomDistancePairGen(GoalBag<G> goals, Context<G, Scamp5SuperPixelTransformation<G>, BRegister> context, Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig) {
            super(goals, context);
            this.scamp5SuperPixelConfig = scamp5SuperPixelConfig;
        }

        private SuperPixelAtomDistancePairGen(GoalBag<G> goals, Context<G, Scamp5SuperPixelTransformation<G>, BRegister> context, Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig, Iterator<Tuple<Integer, Integer>> ijGetter) {
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
                    Dir dir1 = Dir.fromDirection(d1).opposite();
                    Movxb<G> movx = new Movxb<>(item.a, dir1.x, dir1.y, item.a.getBank(), scamp5SuperPixelConfig);
                    Item newItem = new Item(item, new GoalPair<>(item.a, movx.lower, movx));
                    outList.add(newItem);
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
            }
        }

        @Override
        protected void addDirectTransformation(G a, List<AtomDistancePairGen<G, Scamp5SuperPixelTransformation<G>, BRegister>.Item> outList) {
            Distance centre = new Distance(a.getAveragePos());
            if(scamp5SuperPixelConfig.useMovbx && centre.manhattanXY()>0){
                SimpleTransformation.Direction d1 = centre.majorXYDirection();
                if(d1!= null) {
                    Dir dir1 = Dir.fromDirection(d1);
                    Movxb<G> movx = new Movxb<>(a, dir1.x, dir1.y, a.getBank(), scamp5SuperPixelConfig);
                    Item newItem = new Item(a, movx.lower, movx);
                    outList.add(newItem);
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

        public SuperPixelAtomDistanceSortedPairGen(GoalBag<G> goals, Context<G, Scamp5SuperPixelTransformation<G>, BRegister> conf, Scamp5SuperPixelConfig<G> scamp5Config, CostHeuristic<G, Scamp5SuperPixelTransformation<G>, BRegister> huristic) {
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
