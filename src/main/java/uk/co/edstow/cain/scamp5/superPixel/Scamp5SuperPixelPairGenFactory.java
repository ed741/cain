package uk.co.edstow.cain.scamp5.superPixel;

import uk.co.edstow.cain.goals.BankedKernel3DGoal;
import uk.co.edstow.cain.goals.atomGoal.Atom;
import uk.co.edstow.cain.goals.atomGoal.pairGen.Distance;
import uk.co.edstow.cain.goals.atomGoal.pairGen.SimpleTransformation;
import uk.co.edstow.cain.nonlinear.LinearPairGenFactory;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.CostHeuristic;
import uk.co.edstow.cain.scamp5.Scamp5ConfigGetter;
import uk.co.edstow.cain.scamp5.digital.Scamp5DigitalTransformation;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.co.edstow.cain.scamp5.superPixel.Scamp5SuperPixelTransformation.*;

public class Scamp5SuperPixelPairGenFactory<G extends BankedKernel3DGoal<G>> implements LinearPairGenFactory<G, Scamp5SuperPixelTransformation<G>> {

    private final Scamp5ConfigGetter<G, Scamp5SuperPixelTransformation<G>, Scamp5SuperPixelConfig<G>> scamp5ConfGet;

    public Scamp5SuperPixelPairGenFactory(Scamp5ConfigGetter<G, Scamp5SuperPixelTransformation<G>, Scamp5SuperPixelConfig<G>> confGetter) {
        this.scamp5ConfGet = confGetter;
    }


    @Override
    public List<GoalPair<G, Scamp5SuperPixelTransformation<G>>> applyAllUnaryOpForwards(List<G> initialGoals, Context<G, Scamp5SuperPixelTransformation<G>> context, GoalBag<G> goals){
        Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig = this.scamp5ConfGet.getScamp5ConfigForDirectSolve(goals, context);
        goals = new GoalBag<>(goals);
        List<GoalPair<G, Scamp5SuperPixelTransformation<G>>> allPairs = new ArrayList<>();
        List<G> empties = new ArrayList<>();
        for (int i = goals.size() - 1; i >= 0; i--) {
          if(goals.get(i).allZero()){
              empties.add(goals.remove(i));
          }
        }
        if(!scamp5SuperPixelConfig.onlyMov()) {
            if(scamp5SuperPixelConfig.useRes) {
                allPairs.add(new GoalPair<>(empties, Collections.emptyList(), new Res<>(empties, scamp5SuperPixelConfig)));
            }
        }
        List<GoalPair<G,Scamp5SuperPixelTransformation<G>>> pairList = LinearPairGenFactory.super.applyAllUnaryOpForwards(initialGoals, context, goals);
        if(pairList==null){
            return null;
        }
        allPairs.addAll(pairList);
        return allPairs;
    }

    @Override
    public Collection<Tuple<List<GoalPair<G, Scamp5SuperPixelTransformation<G>>>, G>> applyAllUnaryOpForwards(List<G> initialGoals, Context<G, Scamp5SuperPixelTransformation<G>> context, G goal) {
        Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig = this.scamp5ConfGet.getScamp5ConfigForDirectSolve(new GoalBag<>(goal), context);
        ArrayList<Tuple<List<GoalPair<G,Scamp5SuperPixelTransformation<G>>>, G>> list = new ArrayList<>();

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
                        List<GoalPair<G,Scamp5SuperPixelTransformation<G>>> pairs = new ArrayList<>();
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
    public PairGen<G, Scamp5SuperPixelTransformation<G>> generatePairs(GoalBag<G> goals, Context<G, Scamp5SuperPixelTransformation<G>> context) {
        return this.scamp5ConfGet.getScamp5Strategy(goals, context);
    }

    @Override
    public Scamp5SuperPixelTransformation<G> getDummyTransformation(List<G> upperGoals, List<G> lowerGoals, Context<G, Scamp5SuperPixelTransformation<G>> context) {
        return new Scamp5SuperPixelTransformation.Null<>(lowerGoals.size(), upperGoals.size(), this.scamp5ConfGet.getScamp5ConfigForDirectSolve(new GoalBag<>(lowerGoals), context));
    }

    @Override
    public Collection<Tuple<List<G>, Scamp5SuperPixelTransformation<G>>> generateValueConstantOps(List<G> goal, Context<G, Scamp5SuperPixelTransformation<G>> context) {
        PairGen<G, Scamp5SuperPixelTransformation<G>> gen =  this.scamp5ConfGet.getScamp5Strategy(new GoalBag<G>(goal), context, true);
        return null; //TODO part of non-linear goals

    }


    public static class ExhaustivePairGen<G extends BankedKernel3DGoal<G>, T extends Scamp5SuperPixelConfig<G>> extends uk.co.edstow.cain.pairgen.ExhaustivePairGen<G, Scamp5SuperPixelTransformation<G>> {

        T scamp5SuperPixelConfig;
        public ExhaustivePairGen(GoalBag<G> goals, Context<G, Scamp5SuperPixelTransformation<G>> context, T scamp5SuperPixelConfig, CostHeuristic<G, Scamp5SuperPixelTransformation<G>> huristic) {
            super(goals, context, huristic);
            this.scamp5SuperPixelConfig = scamp5SuperPixelConfig;
        }

        protected Stream<GoalPair<G,Scamp5SuperPixelTransformation<G>>> getUnaryOpStream(G upper) {
            ArrayList<GoalPair<G,Scamp5SuperPixelTransformation<G>>> pairs = new ArrayList<>();
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

        protected Stream<GoalPair<G,Scamp5SuperPixelTransformation<G>>> getNaryOpStream(G upper) {
            if(this.scamp5SuperPixelConfig.onlyMov()){
                return Stream.empty();
            }
            ArrayList<GoalPair<G,Scamp5SuperPixelTransformation<G>>> pairs = new ArrayList<>();
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

    private static class AtomDistanceListItem<G extends BankedKernel3DGoal<G>> {
        GoalPair<G,Scamp5SuperPixelTransformation<G>> pair;
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

    public static class AtomDistancePairGen<G extends BankedKernel3DGoal<G>, T extends Scamp5SuperPixelConfig<G>> implements PairGen<G, Scamp5SuperPixelTransformation<G>> {
        final T scamp5config;
        final Context<G, Scamp5SuperPixelTransformation<G>> context;
        final GoalBag<G> goals;
        final Iterator<Tuple<Integer, Integer>> ijGetter;
        private int count;

        List<GoalPair<G,Scamp5SuperPixelTransformation<G>>> currentList = new ArrayList<>();

        public AtomDistancePairGen(GoalBag<G> goals, Context<G, Scamp5SuperPixelTransformation<G>> context, T scamp5config) {
            this.goals = goals;
            this.scamp5config = scamp5config;
            this.context = context;
            this.ijGetter = new SteppedCombinationIterator(goals.size());
        }

        private AtomDistancePairGen(GoalBag<G> goals, Context<G, Scamp5SuperPixelTransformation<G>> context, T scamp5config, Iterator<Tuple<Integer, Integer>> ijGetter) {
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
            if(scamp5config.useMovbx && centre.manhattanXY()>0){
                SimpleTransformation.Direction d1 = centre.majorXYDirection();
                if(d1!= null) {
                    Dir dir1 = Dir.fromDirection(d1);
                    Movxb<G> movx = new Movxb<>(a, dir1.x, dir1.y, a.getBank(), scamp5config);
                    AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>();
                    newItem.a = a;
                    newItem.distance = new Distance(d1, 1);
                    newItem.pair = new GoalPair<>(a, movx.lower, movx);
                    outList.add(newItem);
                }
            }
        }

        @Override
        public GoalPair<G,Scamp5SuperPixelTransformation<G>> next() {
            count++;
            fillCurrentList();
            return  currentList.isEmpty()? null:currentList.remove(currentList.size()-1);
        }

        @Override
        public int getNumber() {
            return count;
        }
    }

    public static class AtomDistanceSortedPairGen<G extends BankedKernel3DGoal<G>, T extends Scamp5SuperPixelConfig<G>> extends AtomDistancePairGen<G, T> {

        private final CostHeuristic<G, Scamp5SuperPixelTransformation<G>> huristic;

        public AtomDistanceSortedPairGen(GoalBag<G> goals, Context<G, Scamp5SuperPixelTransformation<G>> conf, T scamp5Config, CostHeuristic<G, Scamp5SuperPixelTransformation<G>> huristic) {
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
    private static <G extends BankedKernel3DGoal<G>> void addAtomDistanceDiagonalPairs(AtomDistanceListItem<G> item,
                                                                                 Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig, List<AtomDistanceListItem<G>> outList) {
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
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<G>(item);
                newItem.pair = new GoalPair<>(item.a, Arrays.asList(split1, split2), new Add_2<>(split1, split2, scamp5SuperPixelConfig));
                outList.add(newItem);
            }
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private static <G extends BankedKernel3DGoal<G>> void addAtomDistancePairs(AtomDistanceListItem<G> item,
                                                                         Scamp5SuperPixelConfig<G> scamp5SuperPixelConfig, List<AtomDistanceListItem<G>> outList) {
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
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, movx.lower, movx);
                outList.add(newItem);
            }
        } else if (!scamp5SuperPixelConfig.onlyMov()){
            G aWithoutTmp = item.a.without(tmp);

            //Add_2
            if(scamp5SuperPixelConfig.useAdd) {
                G split2 = aWithoutTmp;
                List<G> lowers = Arrays.asList(tmp, split2);
                AtomDistanceListItem<G> newItem = new AtomDistanceListItem<>(item);
                newItem.pair = new GoalPair<>(item.a, lowers, new Add_2<>(tmp, split2, scamp5SuperPixelConfig));
                outList.add(newItem);
            }
        }
    }

    private static <G extends BankedKernel3DGoal<G>> List<AtomDistanceListItem<G>> getAtomDistanceList(G a, G b, boolean diagonal) {
        Map<Tuple<Distance, Boolean>, BankedKernel3DGoal.BankedKernel3DGoalFactory<G>> distanceMap = new HashMap<>();
        for (Iterator<Tuple<Atom, Integer>> ita = a.uniqueCountIterator(); ita.hasNext(); ) {
            Tuple<Atom, Integer> ta = ita.next();
            Atom atomA = ta.getA();
            for (Iterator<Tuple<Atom, Integer>> itb = b.uniqueCountIterator(); itb.hasNext(); ) {
                Tuple<Atom, Integer> tb = itb.next();
                Atom atomB = tb.getA();

                Distance d = new Distance(atomA, atomB);
                boolean negate = atomA.positive ^ atomB.positive;
                Tuple<Distance, Boolean> key = new Tuple<>(d, negate);
                BankedKernel3DGoal.BankedKernel3DGoalFactory<G> goalFactory = distanceMap.getOrDefault(key, a.newFactory());
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
