package uk.co.edstow.cain.scamp5.digital;

import uk.co.edstow.cain.Transformation;
import uk.co.edstow.cain.atom.Atom;
import uk.co.edstow.cain.atom.AtomGoal;
import uk.co.edstow.cain.atom.pairGen.Distance;
import uk.co.edstow.cain.atom.pairGen.SimpleTransformation;
import uk.co.edstow.cain.nonlinear.LinearPairGenFactory;
import uk.co.edstow.cain.pairgen.Config;
import uk.co.edstow.cain.pairgen.CostHuristic;
import uk.co.edstow.cain.pairgen.ExhaustivePairGen;
import uk.co.edstow.cain.scamp5.Scamp5ConfigGetter;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.co.edstow.cain.scamp5.digital.Scamp5DigitalTransformation.*;

public class Scamp5DigitalPairGenFactory<T extends Scamp5DigitalConfig<AtomGoal>> implements LinearPairGenFactory<AtomGoal> {

    private final Scamp5ConfigGetter<AtomGoal, T> scamp5ConfGet;

    public Scamp5DigitalPairGenFactory(Scamp5ConfigGetter<AtomGoal, T> confGetter) {
        this.scamp5ConfGet = confGetter;
    }


    @Override
    public List<GoalPair<AtomGoal>> applyAllUnaryOpForwards(List<AtomGoal> initialGoals, Config<AtomGoal> config, GoalBag<AtomGoal> goals){
        Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig = this.scamp5ConfGet.getScamp5ConfigForDirectSolve(goals, config);
        goals = new GoalBag<>(goals);
        List<GoalPair<AtomGoal>> allPairs = new ArrayList<>();
        GoalBag<AtomGoal> empties = new GoalBag<>();
        for (int i = goals.size() - 1; i >= 0; i--) {
          if(goals.get(i).isEmpty()){
              empties.add(goals.remove(i));
          }
        }
        if(!scamp5DigitalConfig.onlyMov()) {
            for (int i = 0; i < empties.size(); i++) {
                if (scamp5DigitalConfig.useRes2 && i + 1 < empties.size()) {
                    allPairs.add(new GoalPair<>(Arrays.asList(empties.get(i), empties.get(i + 1)), Collections.emptyList(), new Res_2(empties.get(i), empties.get(i + 1), scamp5DigitalConfig)));
                    i++;
                } else if (scamp5DigitalConfig.useRes) {
                    allPairs.add(new GoalPair<>(empties.get(i), Collections.emptyList(), new Res(empties.get(i), scamp5DigitalConfig)));
                }
            }
        }
        List<GoalPair<AtomGoal>> pairList = LinearPairGenFactory.super.applyAllUnaryOpForwards(initialGoals, config, goals);
        if(pairList==null){
            return null;
        }
        allPairs.addAll(pairList);
        return allPairs;
    }

    @Override
    public Collection<Tuple<List<GoalPair<AtomGoal>>, AtomGoal>> applyAllUnaryOpForwards(List<AtomGoal> initialGoals, Config<AtomGoal> config, AtomGoal goal) {
        Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig = this.scamp5ConfGet.getScamp5ConfigForDirectSolve(new GoalBag<>(goal), config);
        ArrayList<Tuple<List<GoalPair<AtomGoal>>, AtomGoal>> list = new ArrayList<>();

        if(initialGoals.contains(goal)){
            list.add(new Tuple<>(Collections.emptyList(), goal));
            return list;
        }

        //Res
        if(scamp5DigitalConfig.useRes && goal.isEmpty()){
            Res res = new Res(goal, scamp5DigitalConfig);
            list.add(new Tuple<>(Collections.singletonList((new GoalPair<>(goal, Collections.emptyList(), res))), initialGoals.get(0)));
            return list;
        }

        //Divide
        if(scamp5DigitalConfig.useDiv) {
            try {
                if (goal.countUnique() == 1 && goal.get(0).x == 0 && goal.get(0).y == 0 && goal.get(0).positive) {
                    int z = goal.get(0).z;
                    AtomGoal ic = null;
                    for (AtomGoal i : initialGoals) {
                        if (i.contains(goal.get(0))) {
                            ic = i;
                        }
                    }
                    if (ic != null) {
                        List<GoalPair<AtomGoal>> pairs = new ArrayList<>();
                        Div div = new Div(goal, true, scamp5DigitalConfig);
                        pairs.add(new GoalPair<>(div.applyOpForwards(), Collections.singletonList(div.a), div));
                        while (div.a.atomCount() < ic.atomCount()) {
                            div = new Div(div.a, true, scamp5DigitalConfig);
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
            boolean divisable = true;
            AtomGoal.Factory factory = new AtomGoal.Factory();
            for (Iterator<Tuple<Atom, Integer>> it = goal.uniqueCountIterator(); it.hasNext(); ) {
                Tuple<Atom, Integer> t = it.next();
                divisable &= (t.getB()%2)==0;
                factory.add(t.getA(), t.getB()/2);
            }
            if(divisable) {
                AtomGoal lower = factory.get();
                if(initialGoals.contains(lower)) {
                    AddSelf addSelf = new AddSelf(lower, goal, scamp5DigitalConfig);
                    list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, addSelf.a, addSelf)), addSelf.a));
                }
            }
        }

        //Move x
        if(scamp5DigitalConfig.useMovx) {
            for (Dir dir : Dir.values()) {
                Movx movx = new Movx(goal, dir, true, scamp5DigitalConfig);
                if (initialGoals.contains(movx.a)) {
                    list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, movx.a, movx)), movx.a));
                }
            }
        }

        return list;
    }





    @Override
    public PairGen<AtomGoal> generatePairs(GoalBag<AtomGoal> goals, Config<AtomGoal> config) {
        return this.scamp5ConfGet.getScamp5Strategy(goals, config);
    }

    @Override
    public Collection<Tuple<List<AtomGoal>, Transformation>> generateValueConstantOps(List<AtomGoal> goal, Config<AtomGoal> config) {
        PairGen<AtomGoal> gen =  this.scamp5ConfGet.getScamp5Strategy(new GoalBag<AtomGoal>(goal), config, true);
        return null; //TODO part of non-linear goals

    }


    public static class Scamp5ExhaustivePairGen<T extends Scamp5DigitalConfig<AtomGoal>> extends ExhaustivePairGen<AtomGoal>{

        Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig;
        public Scamp5ExhaustivePairGen(GoalBag<AtomGoal> goals, Config<AtomGoal> conf, Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig, CostHuristic<AtomGoal> huristic) {
            super(goals, conf, huristic);
            this.scamp5DigitalConfig = scamp5DigitalConfig;
        }

        protected Stream<GoalPair<AtomGoal>> getUnaryOpStream(AtomGoal upper) {
            ArrayList<GoalPair<AtomGoal>> pairs = new ArrayList<>();
            if(!scamp5DigitalConfig.onlyMov()) {

                if (upper.isEmpty()) {
                    if(scamp5DigitalConfig.useRes) {
                        Res res = new Res(upper, scamp5DigitalConfig);
                        pairs.add(new GoalPair<>(upper, Collections.emptyList(), res));

                    }
                    if(scamp5DigitalConfig.useRes2) {
                        AtomGoal other = null;
                        for (AtomGoal g : goals) {
                            if (g.isEmpty() && !g.equivalent(upper)) {
                                other = g;
                                break;
                            }
                        }
                        if (other != null) {
                            Res_2 res_2 = new Res_2(upper, other, scamp5DigitalConfig);
                            pairs.add(new GoalPair<>(Arrays.asList(upper, other), Collections.emptyList(), res_2));
                        }
                    }
                }

                //Divide
                if(scamp5DigitalConfig.useDiv) {
                    Div div = new Div(upper, true, scamp5DigitalConfig);
                    pairs.add(new GoalPair<>(upper, div.a, div));
                }

                //Add self (mul2)
                if(scamp5DigitalConfig.useAddSelf) {
                    AddSelf add = new AddSelf(upper, true, scamp5DigitalConfig);
                    if(add.isPossible()) {
                        pairs.add(new GoalPair<>(upper, add.a, add));
                    }
                }
            }

            //Mov
            if(scamp5DigitalConfig.useMov) {
                Mov mov = new Mov(upper, true, scamp5DigitalConfig);
                pairs.add(new GoalPair<>(upper, mov.a, mov));
            }

            //Move x
            if(scamp5DigitalConfig.useMovx) {
                for (Dir dir : Dir.values()) {
                    Movx movx = new Movx(upper, dir, true, scamp5DigitalConfig);
                    pairs.add(new GoalPair<>(upper, movx.a, movx));
                }
            }

            return pairs.stream();
        }

        protected Stream<GoalPair<AtomGoal>> getNaryOpStream(AtomGoal upper) {
            if(this.scamp5DigitalConfig.onlyMov()){
                return Stream.empty();
            }
            ArrayList<GoalPair<AtomGoal>> pairs = new ArrayList<>();
            List<AtomGoal> splits = upper.allSplits();
            int normal = splits.size();

            Set<AtomGoal> seen = new HashSet<>();
            for (int i = 0; i < splits.size(); i++) {
                AtomGoal a = splits.get(i);
                boolean skipAdd_2 = false;
                if (seen.contains(a)) {
                    skipAdd_2 = true;
                }
                AtomGoal b;
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
                    if(scamp5DigitalConfig.useAdd) {
                        Add_2 add = new Add_2(a, b, scamp5DigitalConfig);
                        pairs.add(new GoalPair<>(upper, Arrays.asList(a, b), add));
                    }
                }
            }

            return pairs.stream();
        }
    }

    private static class AtomDistanceListItem {
        GoalPair<AtomGoal> pair;
        double cost;
        AtomGoal a;
        AtomGoal b;
        Distance distance;
        boolean negate;
        AtomGoal to;

        AtomDistanceListItem() {
        }
        
        AtomDistanceListItem(AtomDistanceListItem item) {
            this.pair = item.pair;
            this.cost = item.cost;
            this.a = item.a;
            this.b = item.b;
            this.distance = item.distance;
            this.negate = item.negate;
            this.to = item.to;

        }
    }

    private static class AtomDistancePairGen<T extends Scamp5DigitalConfig<AtomGoal>> implements PairGen<AtomGoal> {
        final T scamp5config;
        final Config<AtomGoal> config;
        final GoalBag<AtomGoal> goals;
        final Iterator<Tuple<Integer, Integer>> ijGetter;
        private int count;

        List<GoalPair<AtomGoal>> currentList = new ArrayList<>();

        private AtomDistancePairGen(GoalBag<AtomGoal> goals, Config<AtomGoal> config, T scamp5config) {
            this.goals = goals;
            this.scamp5config = scamp5config;
            this.config = config;
            this.ijGetter = new SteppedCombinationIterator(goals.size());
        }

        private AtomDistancePairGen(GoalBag<AtomGoal> goals, Config<AtomGoal> config, T scamp5config, Iterator<Tuple<Integer, Integer>> ijGetter) {
            this.goals = goals;
            this.scamp5config = scamp5config;
            this.config = config;
            this.ijGetter = ijGetter;
        }

        @SuppressWarnings("WeakerAccess")
        protected void fillCurrentList(){
            while (currentList.isEmpty()){
                if(!ijGetter.hasNext()){
                    return;
                }
                Tuple<Integer, Integer> ij = ijGetter.next();
                AtomGoal a = goals.get(ij.getA());
                AtomGoal b = goals.get(ij.getB());

                boolean diagonal = ij.getA().equals(ij.getB());
                List<AtomDistanceListItem> inList = getAtomDistanceList(a, b, diagonal);
                List<AtomDistanceListItem> outList = new ArrayList<>();
                inList.sort(atomDistanceComparator);
                addPairs(a, diagonal, inList, outList);
                outList.forEach(item -> currentList.add(item.pair));

            }
        }
        @SuppressWarnings("WeakerAccess")
        protected void addPairs(AtomGoal a, boolean diagonal, List<AtomDistanceListItem> inList, List<AtomDistanceListItem> outList) {
            if(!diagonal) {
                for (AtomDistanceListItem item : inList) {
                    addAtomDistancePairs(item, scamp5config, outList);
                }
            } else {
                // diagonal == True
                if(goals.size() < config.totalAvailableRegisters) {
                    addDirectMov(a, outList);
                }

                for (AtomDistanceListItem item : inList) {
                    addAtomDistanceDiagonalPairs(item, scamp5config, outList);
                }
                if (scamp5config.useDiv) {
                    for (AtomGoal initialGoal : config.initialGoals) {
                        if (initialGoal.hasSubGoal(a)) {
                            AtomGoal l = new AtomGoal.Factory(a).addAll(a).get();
                            AtomDistanceListItem newItem = new AtomDistanceListItem();
                            newItem.a = a;
                            newItem.distance = new Distance(0, 0, 0);
                            newItem.pair = new GoalPair<>(a, l, new Div(l, scamp5config));
                            outList.add(newItem);
                        }
                    }
                }
            }
        }

        @SuppressWarnings("WeakerAccess")
        protected void addDirectMov(AtomGoal a, List<AtomDistanceListItem> outList) {
            Distance centre = new Distance(a.getAveragePos());
            if(scamp5config.useMovx && centre.manhattanXY()>0){
                SimpleTransformation.Direction d1 = centre.majorXYDirection();
                if(d1!= null) {
                    Dir dir1 = Dir.fromDirection(d1);
                    Movx movx = new Movx(a, dir1, true, scamp5config);
                    AtomDistanceListItem newItem = new AtomDistanceListItem();
                    newItem.a = a;
                    newItem.distance = new Distance(d1, 1);
                    newItem.pair = new GoalPair<>(a, movx.a, movx);
                    outList.add(newItem);
                }
            }
        }

        @Override
        public GoalPair<AtomGoal> next() {
            count++;
            fillCurrentList();
            return  currentList.isEmpty()? null:currentList.remove(currentList.size()-1);
        }

        @Override
        public int getNumber() {
            return count;
        }
    }

    public static class AtomDistanceSortedPairGen<T extends Scamp5DigitalConfig<AtomGoal>> extends AtomDistancePairGen<T> {

        private final CostHuristic<AtomGoal> huristic;

        public AtomDistanceSortedPairGen(GoalBag<AtomGoal> goals, Config<AtomGoal> conf, T scamp5Config, CostHuristic<AtomGoal> huristic) {
            super(goals, conf, scamp5Config, new PlainCombinationIterator(goals.size()));
            this.huristic = huristic;
        }

        @Override
        protected void fillCurrentList() {
            if(!currentList.isEmpty()){
                return;
            }
            List<AtomDistanceListItem> outList = new ArrayList<>();
            while (ijGetter.hasNext()){
                Tuple<Integer, Integer> ij = ijGetter.next();
                AtomGoal a = goals.get(ij.getA());
                AtomGoal b = goals.get(ij.getB());
                boolean diagonal = ij.getA().equals(ij.getB());
                List<AtomDistanceListItem> inList = getAtomDistanceList(a, b, diagonal);
                //inList.sort(atomDistanceComparator);
                addPairs(a, diagonal, inList, outList);
            }
            outList.parallelStream().forEach(item -> item.cost = huristic.getCost(item.pair, goals, this.config));
            outList.removeIf(item -> item.cost < 0);
            outList.sort(Comparator.comparingDouble((AtomDistanceListItem item) -> item.cost).reversed());
            currentList = outList.stream().map(item -> item.pair).collect(Collectors.toList());
        }
    }

    private static final Comparator<AtomDistanceListItem> atomDistanceComparator = Comparator.comparingInt((AtomDistanceListItem i) -> i.to.size()).thenComparingInt(i -> -i.distance.manhattanXY());


    @SuppressWarnings("UnnecessaryLocalVariable")
    private static void addAtomDistanceDiagonalPairs(AtomDistanceListItem item,
                                                                         Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig, List<AtomDistanceListItem> outList) {
        if(scamp5DigitalConfig.onlyMov()){
            return;
        }
        Distance centre = new Distance(item.a.getAveragePos());
        AtomGoal aWithoutTo = item.a.without(item.to);
        //add_2, sub
        if(!item.negate) {
            if(scamp5DigitalConfig.useAdd) {
                AtomGoal split1 = aWithoutTo;
                AtomGoal split2 = item.to;
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new GoalPair<>(item.a, Arrays.asList(split1, split2), new Add_2(split1, split2, scamp5DigitalConfig));
                outList.add(newItem);
            }
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private static void addAtomDistancePairs(AtomDistanceListItem item,
                                             Scamp5DigitalConfig<AtomGoal> scamp5DigitalConfig, List<AtomDistanceListItem> outList) {
        AtomGoal tmpMov = item.distance.inverse().translate(item.to);
        AtomGoal tmp = tmpMov;
        if(item.negate){
            tmp = tmpMov.negative();
        }
        if(tmp.same(item.a)){
            if (scamp5DigitalConfig.useMovx && item.distance.manhattanXY() > 0){
                //movx
                SimpleTransformation.Direction d1 = item.distance.majorXYDirection();
                Dir dir1 = Dir.fromDirection(d1).opposite();
                Movx movx = new Movx(item.a, dir1, true, scamp5DigitalConfig);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new GoalPair<>(item.a, movx.a, movx);
                outList.add(newItem);
            }
        } else if (!scamp5DigitalConfig.onlyMov()){
            AtomGoal aWithoutTmp = item.a.without(tmp);

            //Add_2
            if(scamp5DigitalConfig.useAdd) {
                AtomGoal split2 = aWithoutTmp;
                List<AtomGoal> lowers = Arrays.asList(tmp, split2);
                AtomDistanceListItem newItem = new AtomDistanceListItem(item);
                newItem.pair = new GoalPair<>(item.a, lowers, new Add_2(tmp, split2, scamp5DigitalConfig));
                outList.add(newItem);
            }
        }
    }

    private static List<AtomDistanceListItem> getAtomDistanceList(AtomGoal a, AtomGoal b, boolean diagonal) {
        Map<Tuple<Distance, Boolean>, AtomGoal.Factory> distanceMap = new HashMap<>();
        for (Iterator<Tuple<Atom, Integer>> ita = a.uniqueCountIterator(); ita.hasNext(); ) {
            Tuple<Atom, Integer> ta = ita.next();
            Atom atomA = ta.getA();
            for (Iterator<Tuple<Atom, Integer>> itb = b.uniqueCountIterator(); itb.hasNext(); ) {
                Tuple<Atom, Integer> tb = itb.next();
                Atom atomB = tb.getA();

                Distance d = new Distance(atomA, atomB);
                boolean negate = atomA.positive ^ atomB.positive;
                Tuple<Distance, Boolean> key = new Tuple<>(d, negate);
                AtomGoal.Factory goalFactory = distanceMap.getOrDefault(key, new AtomGoal.Factory());
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
