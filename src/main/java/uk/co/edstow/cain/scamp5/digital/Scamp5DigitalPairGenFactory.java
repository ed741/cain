package uk.co.edstow.cain.scamp5.digital;

import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.atomGoal.Atom;
import uk.co.edstow.cain.goals.atomGoal.pairGen.Distance;
import uk.co.edstow.cain.goals.atomGoal.pairGen.SimpleTransformation;
import uk.co.edstow.cain.nonlinear.LinearPairGenFactory;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.scamp5.Scamp5ConfigGetter;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.co.edstow.cain.scamp5.digital.Scamp5DigitalTransformation.*;

public class Scamp5DigitalPairGenFactory<G extends Kernel3DGoal<G>> implements LinearPairGenFactory<G, Scamp5DigitalTransformation<G>, Register> {

    private final Scamp5ConfigGetter<G, Scamp5DigitalTransformation<G>, Register, Scamp5DigitalConfig<G>> scamp5ConfGet;

    public Scamp5DigitalPairGenFactory(Scamp5ConfigGetter<G, Scamp5DigitalTransformation<G>, Register, Scamp5DigitalConfig<G>> confGetter) {
        this.scamp5ConfGet = confGetter;
    }


    @Override
    public List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> applyAllUnaryOpForwards(List<G> initialGoals, Context<G, Scamp5DigitalTransformation<G>, Register> context, GoalBag<G> goals){
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
        List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> pairList = LinearPairGenFactory.super.applyAllUnaryOpForwards(initialGoals, context, goals);
        if(pairList==null){
            return null;
        }
        allPairs.addAll(pairList);
        return allPairs;
    }

    @Override
    public Collection<Tuple<List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>>, G>> applyAllUnaryOpForwards(List<G> initialGoals, Context<G, Scamp5DigitalTransformation<G>, Register> context, G goal) {
        Scamp5DigitalConfig<G> scamp5DigitalConfig = this.scamp5ConfGet.getScamp5ConfigForDirectSolve(new GoalBag<>(goal), context);
        ArrayList<Tuple<List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>>, G>> list = new ArrayList<>();

        if(initialGoals.contains(goal)){
            list.add(new Tuple<>(Collections.emptyList(), goal));
            return list;
        }

        //Res
        if(scamp5DigitalConfig.useRes && goal.allZero()){
            Res<G> res = new Res<>(goal, scamp5DigitalConfig);
            list.add(new Tuple<>(Collections.singletonList((new GoalPair<>(goal, Collections.emptyList(), res))), initialGoals.get(0)));
            return list;
        }

        //Divide
        if(scamp5DigitalConfig.useDiv) {
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

    @Override
    public Collection<Tuple<List<G>, Scamp5DigitalTransformation<G>>> generateValueConstantOps(List<G> goal, Context<G, Scamp5DigitalTransformation<G>, Register> context) {
        PairGen<G, Scamp5DigitalTransformation<G>, Register> gen =  this.scamp5ConfGet.getScamp5Strategy(new GoalBag<G>(goal), context, true);
        return null; //TODO part of non-linear goals

    }


    public static class ExhaustivePairGen<G extends Kernel3DGoal<G>> extends uk.co.edstow.cain.pairgen.ExhaustivePairGen<G, Scamp5DigitalTransformation<G>, Register> {

        Scamp5DigitalConfig<G> scamp5DigitalConfig;
        public ExhaustivePairGen(GoalBag<G> goals, Context<G, Scamp5DigitalTransformation<G>, Register> context, Scamp5DigitalConfig<G> scamp5DigitalConfig, CostHeuristic<G, Scamp5DigitalTransformation<G>> huristic) {
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

    private static class AtomDistanceListItem<G extends Kernel3DGoal<G>> {
        GoalPair<G, Scamp5DigitalTransformation<G>, Register> pair;
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

    public static class AtomDistancePairGen<G extends Kernel3DGoal<G>, T extends Scamp5DigitalConfig<G>> implements PairGen<G, Scamp5DigitalTransformation<G>, Register> {
        final T scamp5config;
        final Context<G, Scamp5DigitalTransformation<G>, Register> context;
        final GoalBag<G> goals;
        final Iterator<Tuple<Integer, Integer>> ijGetter;
        private int count;

        List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> currentList = new ArrayList<>();

        public AtomDistancePairGen(GoalBag<G> goals, Context<G, Scamp5DigitalTransformation<G>, Register> context, T scamp5config) {
            this.goals = goals;
            this.scamp5config = scamp5config;
            this.context = context;
            this.ijGetter = new SteppedCombinationIterator(goals.size());
        }

        private AtomDistancePairGen(GoalBag<G> goals, Context<G, Scamp5DigitalTransformation<G>, Register> context, T scamp5config, Iterator<Tuple<Integer, Integer>> ijGetter) {
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
                if(goals.size() < context.registerAllocator.getAvailableRegisters()) {
                    addDirectMov(a, outList);
                }

                for (AtomDistanceListItem<G> item : inList) {
                    addAtomDistanceDiagonalPairs(item, scamp5config, outList);
                }
                if (scamp5config.useDiv) {
                    for (G initialGoal : context.initialGoals) {
                        if (initialGoal.hasSubGoal(a)) {
                            G l = a.added(a);
                            AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>();
                            newItem.a = a;
                            newItem.distance = new Distance(0, 0, 0);
                            newItem.pair = new GoalPair<>(a, l, new Div<>(l, scamp5config));
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
                    Movx<G> movx = new Movx<>(a, dir1, true, scamp5config);
                    AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>();
                    newItem.a = a;
                    newItem.distance = new Distance(d1, 1);
                    newItem.pair = new GoalPair<>(a, movx.a, movx);
                    outList.add(newItem);
                }
            }
        }

        @Override
        public GoalPair<G, Scamp5DigitalTransformation<G>, Register> next() {
            count++;
            fillCurrentList();
            return  currentList.isEmpty()? null:currentList.remove(currentList.size()-1);
        }

        @Override
        public int getNumber() {
            return count;
        }
    }

    public static class AtomDistanceSortedPairGen<G extends Kernel3DGoal<G>, T extends Scamp5DigitalConfig<G>> extends AtomDistancePairGen<G, T> {

        private final CostHeuristic<G, Scamp5DigitalTransformation<G>> huristic;

        public AtomDistanceSortedPairGen(GoalBag<G> goals, Context<G, Scamp5DigitalTransformation<G>, Register> context, T scamp5Config, CostHeuristic<G, Scamp5DigitalTransformation<G>> huristic) {
            super(goals, context, scamp5Config, new PlainCombinationIterator(goals.size()));
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
                                                                                 Scamp5DigitalConfig<G> scamp5DigitalConfig, List<AtomDistanceListItem<G>> outList) {
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
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<G>(item);
                newItem.pair = new GoalPair<>(item.a, Arrays.asList(split1, split2), new Add_2<>(split1, split2, scamp5DigitalConfig));
                outList.add(newItem);
            }
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private static <G extends Kernel3DGoal<G>> void addAtomDistancePairs(AtomDistanceListItem<G> item,
                                                                         Scamp5DigitalConfig<G> scamp5DigitalConfig, List<AtomDistanceListItem<G>> outList) {
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
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, movx.a, movx);
                outList.add(newItem);
            }
        } else if (!scamp5DigitalConfig.onlyMov()){
            G aWithoutTmp = item.a.without(tmp);

            //Add_2
            if(scamp5DigitalConfig.useAdd) {
                G split2 = aWithoutTmp;
                List<G> lowers = Arrays.asList(tmp, split2);
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, lowers, new Add_2<>(tmp, split2, scamp5DigitalConfig));
                outList.add(newItem);
            }
        }
    }

    private static <G extends Kernel3DGoal<G>> List<AtomDistanceListItem<G>> getAtomDistanceList(G a, G b, boolean diagonal) {
        Map<Tuple<Distance, Boolean>, Kernel3DGoal.Kernel3DGoalFactory<G>> distanceMap = new HashMap<>();
        for (Iterator<Tuple<Atom, Integer>> ita = a.uniqueCountIterator(); ita.hasNext(); ) {
            Tuple<Atom, Integer> ta = ita.next();
            Atom atomA = ta.getA();
            for (Iterator<Tuple<Atom, Integer>> itb = b.uniqueCountIterator(); itb.hasNext(); ) {
                Tuple<Atom, Integer> tb = itb.next();
                Atom atomB = tb.getA();

                Distance d = new Distance(atomA, atomB);
                boolean negate = atomA.positive ^ atomB.positive;
                Tuple<Distance, Boolean> key = new Tuple<>(d, negate);
                Kernel3DGoal.Kernel3DGoalFactory<G> goalFactory = distanceMap.getOrDefault(key, a.newFactory());
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

    private class IteratorPairGen implements PairGen{
        private final Iterator<GoalPair> it;
        private int count;

        private IteratorPairGen(Iterator<GoalPair> it) {
            this.it = it;
        }

        @Override
        public GoalPair next() {
            count++;
            return it.hasNext()?it.next():null;
        }

        @Override
        public int getNumber() {
            return count;
        }
    }
}
