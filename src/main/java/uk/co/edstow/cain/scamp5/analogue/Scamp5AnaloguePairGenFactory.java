package uk.co.edstow.cain.scamp5.analogue;

import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.atomGoal.pairGen.Distance;
import uk.co.edstow.cain.goals.atomGoal.pairGen.SimpleTransformation;
import uk.co.edstow.cain.nonlinear.LinearPairGenFactory;
import uk.co.edstow.cain.pairgen.*;
import uk.co.edstow.cain.goals.atomGoal.Atom;
import uk.co.edstow.cain.Transformation;
import uk.co.edstow.cain.scamp5.Scamp5ConfigGetter;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.co.edstow.cain.scamp5.analogue.Scamp5AnalogueTransformation.*;

public class Scamp5AnaloguePairGenFactory<G extends Kernel3DGoal<G>> implements LinearPairGenFactory<G> {

    private final Scamp5ConfigGetter<G, Scamp5AnalogueConfig<G>> scamp5ConfGet;

    public Scamp5AnaloguePairGenFactory(Scamp5ConfigGetter<G, Scamp5AnalogueConfig<G>> confGetter) {
        this.scamp5ConfGet = confGetter;
    }


    @Override
    public List<GoalPair<G>> applyAllUnaryOpForwards(List<G> initialGoals, Context<G> context, GoalBag<G> goals){
        Scamp5AnalogueConfig<G> scamp5AnalogueConfig = this.scamp5ConfGet.getScamp5ConfigForDirectSolve(goals, context);
        goals = new GoalBag<>(goals);
        List<GoalPair<G>> allPairs = new ArrayList<>();
        GoalBag<G> empties = new GoalBag<>();
        for (int i = goals.size() - 1; i >= 0; i--) {
          if(goals.get(i).allZero()){
              empties.add(goals.remove(i));
          }
        }
        if(!scamp5AnalogueConfig.onlyMov()) {
            for (int i = 0; i < empties.size(); i++) {
                if (scamp5AnalogueConfig.useRes2 && i + 1 < empties.size()) {
                    allPairs.add(new GoalPair<>(Arrays.asList(empties.get(i), empties.get(i + 1)), Collections.emptyList(), new Res_2<G>(empties.get(i), empties.get(i + 1))));
                    i++;
                } else if (scamp5AnalogueConfig.useRes) {
                    allPairs.add(new GoalPair<>(empties.get(i), Collections.emptyList(), new Res<G>(empties.get(i))));
                }
            }
        }
        List<GoalPair<G>> pairList = LinearPairGenFactory.super.applyAllUnaryOpForwards(initialGoals, context, goals);
        if(pairList==null){
            return null;
        }
        allPairs.addAll(pairList);
        return allPairs;
    }

    @Override
    public Collection<Tuple<List<GoalPair<G>>, G>> applyAllUnaryOpForwards(List<G> initialGoals, Context<G> context, G goal) {
        Scamp5AnalogueConfig<G> scamp5AnalogueConfig = this.scamp5ConfGet.getScamp5ConfigForDirectSolve(new GoalBag<>(goal), context);
        ArrayList<Tuple<List<GoalPair<G>>, G>> list = new ArrayList<>();

        if(initialGoals.contains(goal)){
            list.add(new Tuple<>(Collections.emptyList(), goal));
            return list;
        }

        //Res
        if(scamp5AnalogueConfig.useRes && goal.allZero()){
            Res<G> res = new Res<>(goal);
            list.add(new Tuple<>(Collections.singletonList((new GoalPair<>(goal, Collections.emptyList(), res))), initialGoals.get(0)));
            return list;
        }

        //Negate
        if(scamp5AnalogueConfig.useNeg) {
            Neg<G> neg = new Neg<>(goal, true);
            if (initialGoals.contains(neg.a)) {
                list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(neg.applyForwards(), neg.a, neg)), neg.a));
            }
        }

        //Divide
        if(scamp5AnalogueConfig.useDiv3 || scamp5AnalogueConfig.useDiv4) {
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
                        List<GoalPair<G>> pairs = new ArrayList<>();
                        Div<G> div = new Div<>(goal, true, scamp5AnalogueConfig.useDiv3);
                        pairs.add(new GoalPair<>(div.applyOpForwards(), Collections.singletonList(div.a), div));
                        while (div.a.totalI() < ic.totalI()) {
                            div = new Div<>(div.a, true, scamp5AnalogueConfig.useDiv3);
                            pairs.add(new GoalPair<>(div.applyOpForwards(), Collections.singletonList(div.a), div));
                        }
                        if (div.a.equals(ic)) {
                            list.add(new Tuple<>(pairs, ic));
                        }
                    }
                }

            } catch (Transformation.TransformationApplicationException e) {
                e.printStackTrace();
                // Should be unreachable
                assert false;
            }
        } else if(scamp5AnalogueConfig.useDivq) {
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
                        List<GoalPair<G>> pairs = new ArrayList<>();
                        Divq<G> div = new Divq<>(goal, true);
                        pairs.add(new GoalPair<>(div.applyOpForwards(), Collections.singletonList(div.a), div));
                        while (div.a.totalI() < ic.totalI()) {
                            div = new Divq<>(div.a, true);
                            pairs.add(new GoalPair<>(div.applyOpForwards(), Collections.singletonList(div.a), div));
                        }
                        if (div.a.equals(ic)) {
                            list.add(new Tuple<>(pairs, ic));
                        }
                    }
                }

            } catch (Transformation.TransformationApplicationException e) {
                e.printStackTrace();
                // Should be unreachable
                assert false;
            }
        }

        //Move x
        if(scamp5AnalogueConfig.useMovx) {
            for (Dir dir : Dir.values()) {
                Movx<G> movx = new Movx<>(goal, dir, true);
                if (initialGoals.contains(movx.a)) {
                    list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, movx.a, movx)), movx.a));
                }
            }
        }

        //Move 2x
        if(scamp5AnalogueConfig.useMov2x) {
            for (Dir dir1 : Dir.values()) {
                Mov2x<G> mov2xa = new Mov2x<>(goal, dir1, dir1, true);
                if(initialGoals.contains(mov2xa.a)) {
                    list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, mov2xa.a, mov2xa)), mov2xa.a));
                }
                Mov2x<G> mov2xb = new Mov2x<>(goal, dir1, dir1.cw(), true);
                if(initialGoals.contains(mov2xb.a)) {
                    list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, mov2xb.a, mov2xb)), mov2xb.a));
                }
            }
        }

        return list;
    }





    @Override
    public PairGen<G> generatePairs(GoalBag<G> goals, Context<G> context) {
        return this.scamp5ConfGet.getScamp5Strategy(goals, context);
    }

    @Override
    public Collection<Tuple<List<G>, Transformation>> generateValueConstantOps(List<G> goal, Context<G> context) {
        PairGen<G> gen =  this.scamp5ConfGet.getScamp5Strategy(new GoalBag<G>(goal), context, true);
        return null; //TODO part of non-linear goals

    }


    public static class ExhaustivePairGen<G extends Kernel3DGoal<G>, T extends Scamp5AnalogueConfig<G>> extends uk.co.edstow.cain.pairgen.ExhaustivePairGen<G>{

        Scamp5AnalogueConfig<G> scamp5AnalogueConfig;
        public ExhaustivePairGen(GoalBag<G> goals, Context<G> conf, Scamp5AnalogueConfig<G> scamp5AnalogueConfig, CostHeuristic<G> huristic) {
            super(goals, conf, huristic);
            this.scamp5AnalogueConfig = scamp5AnalogueConfig;
        }

        protected Stream<GoalPair<G>> getUnaryOpStream(G upper) {
            ArrayList<GoalPair<G>> pairs = new ArrayList<>();
            if(!scamp5AnalogueConfig.onlyMov()) {
                //Negate
                if(scamp5AnalogueConfig.useNeg) {
                    Neg<G> neg = new Neg<>(upper, true);
                    pairs.add(new GoalPair<>(upper, neg.a, neg));
                }

                if (upper.allZero()) {
                    if(scamp5AnalogueConfig.useRes) {
                        Res<G> res = new Res<>(upper);
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
                            Res_2<G> res_2 = new Res_2<>(upper, other);
                            pairs.add(new GoalPair<>(Arrays.asList(upper, other), Collections.emptyList(), res_2));
                        }
                    }
                }


                //Divide
                if(scamp5AnalogueConfig.useDiv3) {
                    Div<G> divc = new Div<>(upper, true, true);
                    pairs.add(new GoalPair<>(upper, divc.a, divc));
                }
                if(scamp5AnalogueConfig.useDiv4) {
                    Div<G> div = new Div<>(upper, true, false);
                    pairs.add(new GoalPair<>(upper, div.a, div));
                }
                //Divide
                if(scamp5AnalogueConfig.useDivq) {
                    Divq<G> divq = new Divq<G>(upper, true);
                    pairs.add(new GoalPair<>(upper, divq.a, divq));
                }
            }

            //Mov
            if(scamp5AnalogueConfig.useMov) {
                Mov<G> mov = new Mov<>(upper, true);
                pairs.add(new GoalPair<>(upper, mov.a, mov));
            }

            //Move x
            if(scamp5AnalogueConfig.useMovx) {
                for (Dir dir : Dir.values()) {
                    Movx<G> movx = new Movx<>(upper, dir, true);
                    pairs.add(new GoalPair<>(upper, movx.a, movx));
                }
            }

            if(scamp5AnalogueConfig.useMov2x) {
                for (Dir dir1 : Dir.values()) {
                    Mov2x<G> mov2xa = new Mov2x<>(upper, dir1, dir1, true);
                    pairs.add(new GoalPair<>(upper, mov2xa.a, mov2xa));
                    Mov2x<G> mov2xb = new Mov2x<>(upper, dir1, dir1.cw(), true);
                    pairs.add(new GoalPair<>(upper, mov2xb.a, mov2xb));
                }
            }

            return pairs.stream();
        }

        protected Stream<GoalPair<G>> getNaryOpStream(G upper) {
            if(this.scamp5AnalogueConfig.onlyMov()){
                return Stream.empty();
            }
            ArrayList<GoalPair<G>> pairs = new ArrayList<>();
            List<G> splits = upper.allSplits();
            splits.remove(0);
            int normal = splits.size();
            if(scamp5AnalogueConfig.subPowerOf2){
                Kernel3DGoal.Goal3DAtomLikeFactory<G> sub1 = upper.newFactory();
                Kernel3DGoal.Goal3DAtomLikeFactory<G> sub2 = upper.newFactory();
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
                        Add_2<G> add = new Add_2<>(a, b);
                        pairs.add(new GoalPair<>(upper, Arrays.asList(a, b), add));
                    }
                    if (scamp5AnalogueConfig.useAddx) {
                        for (Dir dir : Dir.values()) {
                            G movA = a.translated(-dir.x, -dir.y, 0);
                            G movB = b.translated(-dir.x, -dir.y, 0);
                            Addx<G> addx = new Addx<>(movA, movB, dir);
                            pairs.add(new GoalPair<>(upper, Arrays.asList(movA, movB), addx));
                        }
                    }
                    if (scamp5AnalogueConfig.useAdd2x) {
                        for (Dir dir1 : Dir.values()) {
                            G aMovA = a.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                            G bMovA = b.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                            Add2x<G> add2xa = new Add2x<>(aMovA, bMovA, dir1, dir1);
                            pairs.add(new GoalPair<>(upper, Arrays.asList(aMovA, bMovA), add2xa));
                            Dir dir2 = dir1.cw();
                            G aMovB = a.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                            G bMovB = b.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                            Add2x<G> add2xb = new Add2x<>(aMovB, bMovB, dir1, dir2);
                            pairs.add(new GoalPair<>(upper, Arrays.asList(aMovB, bMovB), add2xb));
                        }
                    }
                }

                G negB = b.negated();
                if(scamp5AnalogueConfig.useSub) {
                    Sub<G> sub = new Sub<>(a, negB);
                    pairs.add(new GoalPair<>(upper, Arrays.asList(a, negB), sub));
                }
                if (scamp5AnalogueConfig.useSubx) {
                    for (Dir dir : Dir.values()) {
                        G movA = a.translated(-dir.x, -dir.y, 0);
                        Subx<G> subx = new Subx<>(movA, negB, dir);
                        pairs.add(new GoalPair<>(upper, Arrays.asList(movA, negB), subx));
                    }
                }
                if (scamp5AnalogueConfig.useSub2x) {
                    for (Dir dir1 : Dir.values()) {
                        G aMovA = a.translated(-dir1.x - dir1.x, -dir1.y - dir1.y, 0);
                        Sub2x<G> sub2xa = new Sub2x<>(aMovA, negB, dir1, dir1);
                        pairs.add(new GoalPair<>(upper, Arrays.asList(aMovA, negB), sub2xa));
                        Dir dir2 = dir1.cw();
                        G aMovB = a.translated(-dir1.x - dir2.x, -dir1.y - dir2.y, 0);
                        Sub2x<G> sub2xb = new Sub2x<>(aMovB, negB, dir1, dir2);
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
                            Add_3<G> add = new Add_3<>(aa, ab, b);
                            pairs.add(new GoalPair<>(upper, Arrays.asList(aa, ab, b), add));
                        }
                    }
                }
            }

            return pairs.stream();
        }
    }

    private static class AtomDistanceListItem<G extends Kernel3DGoal<G>> {
        GoalPair<G> pair;
        double cost;
        G a;
        G b;
        Distance distance;
        boolean negate;
        G to;

        AtomDistanceListItem() {
        }
        
        AtomDistanceListItem(AtomDistanceListItem<G> item) {
            this.pair = item.pair;
            this.cost = item.cost;
            this.a = item.a;
            this.b = item.b;
            this.distance = item.distance;
            this.negate = item.negate;
            this.to = item.to;

        }
    }

    public static class AtomDistancePairGen<G extends Kernel3DGoal<G>, T extends Scamp5AnalogueConfig<G>> implements PairGen<G> {
        final T scamp5config;
        final Context<G> context;
        final GoalBag<G> goals;
        final Iterator<Tuple<Integer, Integer>> ijGetter;
        private int count;

        List<GoalPair<G>> currentList = new ArrayList<>();

        public AtomDistancePairGen(GoalBag<G> goals, Context<G> context, T scamp5config) {
            this.goals = goals;
            this.scamp5config = scamp5config;
            this.context = context;
            this.ijGetter = new SteppedCombinationIterator(goals.size());
        }

        private AtomDistancePairGen(GoalBag<G> goals, Context<G> context, T scamp5config, Iterator<Tuple<Integer, Integer>> ijGetter) {
            this.goals = goals;
            this.scamp5config = scamp5config;
            this.context = context;
            this.ijGetter = ijGetter;
        }

        @SuppressWarnings("WeakerAccess")
        protected void fillCurrentList(){
            while (currentList.isEmpty()){
                if(!ijGetter.hasNext()){
                    return;
                }
                Tuple<Integer, Integer> ij = ijGetter.next();
                G a = goals.get(ij.getA());
                G b = goals.get(ij.getB());

                boolean diagonal = ij.getA().equals(ij.getB());
                List<AtomDistanceListItem<G>> inList = getAtomDistanceList(a, b, diagonal);
                List<AtomDistanceListItem<G>> outList = new ArrayList<>();
                inList.sort(atomDistanceComparator);
                addPairs(a, diagonal, inList, outList);
                outList.forEach(item -> currentList.add(item.pair));

            }
        }
        @SuppressWarnings("WeakerAccess")
        protected void addPairs(G a, boolean diagonal, List<AtomDistanceListItem<G>> inList, List<AtomDistanceListItem<G>> outList) {
            if(!diagonal) {
                for (AtomDistanceListItem<G> item : inList) {
                    addAtomDistancePairs(item, scamp5config, outList);
                }
            } else {
                // diagonal == True
                if(goals.size() < context.totalAvailableRegisters) {
                    addDirectMov(a, outList);
                }

                for (AtomDistanceListItem<G> item : inList) {
                    addAtomDistanceDiagonalPairs(item, scamp5config, outList);
                }
                if (scamp5config.useDivq) {
                    for (G initialGoal : context.initialGoals) {
                        if (initialGoal.hasSubGoal(a)) {
                            G l = a.added(a);
                            AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>();
                            newItem.a = a;
                            newItem.distance = new Distance(0, 0, 0);
                            newItem.pair = new GoalPair<>(a, l, new Divq<>(l));
                            outList.add(newItem);
                        }
                    }
                }
            }
        }

        @SuppressWarnings("WeakerAccess")
        protected void addDirectMov(G a, List<AtomDistanceListItem<G>> outList) {
            Distance centre = new Distance(a.getAveragePos());
            if(scamp5config.useMovx && centre.manhattanXY()>0){
                SimpleTransformation.Direction d1 = centre.majorXYDirection();
                if(d1!= null) {
                    Dir dir1 = Dir.fromDirection(d1);
                    Movx<G> movx = new Movx<>(a, dir1, true);
                    AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>();
                    newItem.a = a;
                    newItem.distance = new Distance(d1, 1);
                    newItem.pair = new GoalPair<>(a, movx.a, movx);
                    outList.add(newItem);
                }
            }
            if(scamp5config.useMov2x && centre.manhattanXY()>1){
                SimpleTransformation.Direction d1 = centre.majorXYDirection();
                if(d1 != null) {
                    SimpleTransformation.Direction d2 = centre.then(d1.opposite()).majorXYDirection();
                    if (d2 != null) {
                        Dir dir1 = Dir.fromDirection(d1).opposite();
                        Dir dir2 = Dir.fromDirection(d2).opposite();
                        Mov2x<G> mov2x = new Mov2x<>(a, dir1, dir2, true);
                        AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>();
                        newItem.a = a;
                        newItem.distance = new Distance(d1, 1).then(d2);
                        newItem.pair = new GoalPair<>(a, mov2x.a, mov2x);
                        outList.add(newItem);
                    }
                }
            }
        }

        @Override
        public GoalPair<G> next() {
            count++;
            fillCurrentList();
            return  currentList.isEmpty()? null:currentList.remove(currentList.size()-1);
        }

        @Override
        public int getNumber() {
            return count;
        }
    }

    public static class AtomDistanceSortedPairGen<G extends Kernel3DGoal<G>, T extends Scamp5AnalogueConfig<G>> extends AtomDistancePairGen<G, T> {

        private final CostHeuristic<G> huristic;

        public AtomDistanceSortedPairGen(GoalBag<G> goals, Context<G> conf, T scamp5Config, CostHeuristic<G> huristic) {
            super(goals, conf, scamp5Config, new PlainCombinationIterator(goals.size()));
            this.huristic = huristic;
        }

        @Override
        protected void fillCurrentList() {
            if(!currentList.isEmpty()){
                return;
            }
            List<AtomDistanceListItem<G>> outList = new ArrayList<>();
            while (ijGetter.hasNext()){
                Tuple<Integer, Integer> ij = ijGetter.next();
                G a = goals.get(ij.getA());
                G b = goals.get(ij.getB());
                boolean diagonal = ij.getA().equals(ij.getB());
                List<AtomDistanceListItem<G>> inList = getAtomDistanceList(a, b, diagonal);
                //inList.sort(atomDistanceComparator);
                addPairs(a, diagonal, inList, outList);
            }
            outList.parallelStream().forEach(item -> item.cost = huristic.getCost(item.pair, goals, this.context));
            outList.removeIf(item -> item.cost < 0);
            outList.sort(Comparator.comparingDouble((AtomDistanceListItem<G> item) -> item.cost).reversed());
            currentList = outList.stream().map(item -> item.pair).collect(Collectors.toList());
        }
    }

    private static final Comparator<AtomDistanceListItem<?>> atomDistanceComparator = Comparator.comparingInt((AtomDistanceListItem<?> i) -> i.to.totalI()).thenComparingInt(i -> -i.distance.manhattanXY());


    @SuppressWarnings("UnnecessaryLocalVariable")
    private static <G extends Kernel3DGoal<G>> void addAtomDistanceDiagonalPairs(AtomDistanceListItem<G> item,
                                                                                 Scamp5AnalogueConfig<G> scamp5AnalogueConfig, List<AtomDistanceListItem<G>> outList) {
        if(scamp5AnalogueConfig.onlyMov()){
            return;
        }
        Distance centre = new Distance(item.a.getAveragePos());
        G aWithoutTo = item.a.without(item.to);
        //add_2, sub
        if(!item.negate) {
            if(scamp5AnalogueConfig.useAdd) {
                G split1 = aWithoutTo;
                G split2 = item.to;
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, Arrays.asList(split1, split2), new Add_2<>(split1, split2));
                outList.add(newItem);
            }
        } else {
            if(scamp5AnalogueConfig.useSub) {
                G split1 = aWithoutTo;
                G split2 = item.to.negated();
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, Arrays.asList(split1, split2), new Sub<>(split1, split2));
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
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, Arrays.asList(split1, split2, split3), new Add_3<>(split1, split2, split3));
                outList.add(newItem);
            }
        }
        if(scamp5AnalogueConfig.useAddx && centre.manhattanXY()>0){
            Dir dir1 = Dir.fromDirection(centre.majorXYDirection());
            G split1 = aWithoutTo.translated(-dir1.x, -dir1.y, 0);
            G split2 = item.to.translated(-dir1.x, -dir1.y, 0);
            Addx<G> addx = new Addx<>(split1, split2, dir1);
            AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
            newItem.pair = new GoalPair<>(item.a, Arrays.asList(split1, split2), addx);
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
            Add2x<G> add2x = new Add2x<>(split1, split2, dir1, dir2);
            AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
            newItem.pair = new GoalPair<>(item.a, Arrays.asList(split1, split2), add2x);
            outList.add(newItem);
        }

        if(scamp5AnalogueConfig.useSubx && item.distance.manhattanXY()>0){
            Dir dir1 = Dir.fromDirection(item.distance.majorXYDirection()).opposite();
            G split1 = aWithoutTo.translated(-dir1.x, -dir1.y, 0);
            G split2 = item.to.negated();
            AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
            newItem.pair = new GoalPair<>(item.a, Arrays.asList(split1, split2), new Subx<>(split1, split2, dir1));
            outList.add(newItem);
        }

        if(scamp5AnalogueConfig.useSub2x && item.distance.manhattanXY()>1){
            SimpleTransformation.Direction d1 = item.distance.majorXYDirection();
            SimpleTransformation.Direction d2 = item.distance.then(d1.opposite()).majorXYDirection();
            Dir dir1 = Dir.fromDirection(d1).opposite();
            Dir dir2 = Dir.fromDirection(d2).opposite();
            G split1 = aWithoutTo.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
            G split2 = item.to.negated();
            AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
            newItem.pair = new GoalPair<>(item.a, Arrays.asList(split1, split2), new Sub2x<>(split1, split2, dir1, dir2));
            outList.add(newItem);
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private static <G extends Kernel3DGoal<G>> void addAtomDistancePairs(AtomDistanceListItem<G> item,
                                                                         Scamp5AnalogueConfig<G> scamp5AnalogueConfig, List<AtomDistanceListItem<G>> outList) {
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
                Mov2x<G> mov2x = new Mov2x<>(item.a, dir1, dir2, true);
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, mov2x.a, mov2x);
                outList.add(newItem);
            }
            if (scamp5AnalogueConfig.useMovx && item.distance.manhattanXY() > 0){
                //movx
                SimpleTransformation.Direction d1 = item.distance.majorXYDirection();
                Dir dir1 = Dir.fromDirection(d1).opposite();
                Movx<G> movx = new Movx<>(item.a, dir1, true);
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, movx.a, movx);
                outList.add(newItem);
            } else if(scamp5AnalogueConfig.useNeg && item.negate){
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, item.to, new Neg<>(item.to));
                outList.add(newItem);
            }
        } else if (!scamp5AnalogueConfig.onlyMov()){
            G aWithoutTmp = item.a.without(tmp);

            //Add_2
            if(scamp5AnalogueConfig.useAdd) {
                G split2 = aWithoutTmp;
                List<G> lowers = Arrays.asList(tmp, split2);
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, lowers, new Add_2<>(tmp, split2));
                outList.add(newItem);
            }

            //Sub
            if(scamp5AnalogueConfig.useSub) {
                G split2 = aWithoutTmp.negated();
                List<G> lowers = Arrays.asList(tmp, split2);
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, lowers, new Sub<>(tmp, split2));
                outList.add(newItem);
            }
            //TODO add_3 support?

            //addx
            if(scamp5AnalogueConfig.useAddx && item.distance.manhattanXY()>0){
                Dir dir1 = Dir.fromDirection(item.distance.majorXYDirection()).opposite();
                G split1 = aWithoutTmp.translated(-dir1.x, -dir1.y, 0);
                G split2 = tmp.translated(-dir1.x, -dir1.y, 0);
                Addx<G> addx = new Addx<>(split1, split2, dir1);
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, Arrays.asList(split1, split2), addx);
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
                Add2x<G> add2x = new Add2x<>(split1, split2, dir1, dir2);
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, Arrays.asList(split1, split2), add2x);
                outList.add(newItem);
            }

            //Subx
            if(scamp5AnalogueConfig.useSubx && item.distance.manhattanXY()>0){
                Dir dir1 = Dir.fromDirection(item.distance.majorXYDirection()).opposite();
                G split1 = tmp.translated(-dir1.x, -dir1.y, 0);
                G split2 = aWithoutTmp.negated();
                Subx<G> subx = new Subx<>(split1, split2, dir1);
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, Arrays.asList(split1, split2), subx);
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
                Sub2x<G> sub2x = new Sub2x<>(split1, split2, dir1, dir2);
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, Arrays.asList(split1, split2), sub2x);
                outList.add(newItem);
            }
        }
    }

    private static <G extends Kernel3DGoal<G>> List<AtomDistanceListItem<G>> getAtomDistanceList(G a, G b, boolean diagonal) {
        Map<Tuple<Distance, Boolean>, Kernel3DGoal.Goal3DAtomLikeFactory<G>> distanceMap = new HashMap<>();
        for (Iterator<Tuple<Atom, Integer>> ita = a.uniqueCountIterator(); ita.hasNext(); ) {
            Tuple<Atom, Integer> ta = ita.next();
            Atom atomA = ta.getA();
            for (Iterator<Tuple<Atom, Integer>> itb = b.uniqueCountIterator(); itb.hasNext(); ) {
                Tuple<Atom, Integer> tb = itb.next();
                Atom atomB = tb.getA();

                Distance d = new Distance(atomA, atomB);
                boolean negate = atomA.positive ^ atomB.positive;
                Tuple<Distance, Boolean> key = new Tuple<>(d, negate);
                Kernel3DGoal.Goal3DAtomLikeFactory<G> goalFactory = distanceMap.getOrDefault(key, a.newFactory());
                int count = Math.min(ta.getB(), tb.getB());
                if (diagonal && d.isZero()){
                    count /= 2;
                }
                for (int i = 0; i < count; i++) {
                    goalFactory.add(atomB.x, atomB.y, atomB.z, atomB.positive?1:-1);
                }
                distanceMap.put(key, goalFactory);

            }
        }
        List<AtomDistanceListItem<G>> list = new ArrayList<>(distanceMap.size());
        distanceMap.forEach((key, value) -> {
            AtomDistanceListItem<G> i = new AtomDistanceListItem<>();
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
        list.removeIf(t->t.to.totalI()==0);
        return list;
    }

    private static class SteppedCombinationIterator implements Iterator<Tuple<Integer, Integer>> {
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
    private static class PlainCombinationIterator implements Iterator<Tuple<Integer, Integer>> {
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

    private class IteratorPairGen implements PairGen<G>{
        private final Iterator<GoalPair<G>> it;
        private int count;

        private IteratorPairGen(Iterator<GoalPair<G>> it) {
            this.it = it;
        }

        @Override
        public GoalPair<G> next() {
            count++;
            return it.hasNext()?it.next():null;
        }

        @Override
        public int getNumber() {
            return count;
        }
    }
}
