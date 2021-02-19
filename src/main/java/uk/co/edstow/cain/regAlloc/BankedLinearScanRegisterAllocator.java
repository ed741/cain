package uk.co.edstow.cain.regAlloc;

import uk.co.edstow.cain.Transformation;
import uk.co.edstow.cain.goals.BankedGoal;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.Plan;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;

public class BankedLinearScanRegisterAllocator<G extends BankedGoal<G> & Goal<G>, T extends Transformation> implements RegisterAllocator<G, T> {
    private final List<BRegister> registers;
    private final int banks;
    private final List<List<BRegister>> bankedRegisters;
    private final List<BRegister> init;
    private final List<G> initialGoals;

    public BankedLinearScanRegisterAllocator(int banks, List<BRegister> init, List<G> initialGoals, List<BRegister> r) {
        this.init = init;
        this.initialGoals = initialGoals;
        this.registers = r;
        this.banks = banks;
        List<List<BRegister>> bankedRegistersLists = new ArrayList<>(banks);
        for (int i = 0; i < banks; i++) {
            bankedRegistersLists.add(new ArrayList<>());
        }
        this.bankedRegisters = Collections.unmodifiableList(bankedRegistersLists);
        for (BRegister register : this.registers) {
            this.bankedRegisters.get(register.bank).add(register);
        }

    }

    @Override
    public int getAvailableRegisters() {
        return registers.size();
    }

    @Override
    public List<? extends RegisterAllocator.Register> getAvailableRegistersArray() {
        return registers;
    }

    @Override
    public List<? extends RegisterAllocator.Register> getInitRegisters() {
        return init;
    }

    @Override
    public boolean checkValid(GoalBag<G> current, T lastTransformation) {
        return current.size() +lastTransformation.ExtraRegisterCount() <= registers.size();
    }

    @Override
    public boolean checkPossible(Plan<G, T> p) {
        return solve(p) != null;
    }

    @Override
    public Mapping<G> solve(Plan<G, T> plan){
        List<? extends Plan.Step<G, ?>> all_r = plan.getAll();
        List<Set<Integer>> requiresInit = new ArrayList<>(init.size());
        for (BRegister i : init) {
            requiresInit.add(new HashSet<>());
        }

        List<G> initialTrueGoals = new ArrayList<>(init.size());

        for (int j = 0; j < init.size(); j++) {
            inits:
            for (int i = all_r.size() - 1; i >= 0; i--) {
                Plan.Step<G,?> step = all_r.get(i);
                for (int l = 0; l < step.getLowers().size(); l++) {
                    G g = step.getLowerTrueGoal(l);
                    if(g.same(initialGoals.get(j))){
                        initialTrueGoals.add(g);
                        break inits;
                    }
                }
            }
        }



        List<List<Integer>> liveness = new ArrayList<>();


        List<BRegister> availableRegisters = new ArrayList<>(registers);
        Set<Tuple<Integer, Integer>> live = new HashSet<>();
        Mapping<G> map = new Mapping<>(init);
        Map<Tuple<Integer, Integer>, BRegister> lineMap = new HashMap<>();

        for (int i = 0; i < all_r.size() + initialGoals.size(); i++) {
            List<Integer> l = new ArrayList<>();
            if(i < all_r.size()) {
                for (G U : all_r.get(i).getUppers()) {
                    l.add(i);
                }
            }else{
                l.add(i);
            }
            liveness.add(l);
        }

        for (int i = 0; i < all_r.size(); i++) {
            Plan.Step<G,?> step = all_r.get(i);

            List<G> lowers = step.getLowers();
            for (int lowerIdx = 0; lowerIdx < lowers.size(); lowerIdx++) {
                G trueGoal = step.getLowerTrueGoal(lowerIdx);
                int j = i + 1;
                int k = -1;
                jloop:
                while (j < all_r.size()) {
                    List<G> uppers = all_r.get(j).getUppers();
                    for (int upperIdx = 0; upperIdx < uppers.size(); upperIdx++) {
                        G upper = uppers.get(upperIdx);
                        if (upper.equivalent(trueGoal)) {
                            k = upperIdx;
                            break jloop;
                        }
                    }

                    j++;
                }
                int liveUntil = i;
                for (int upperIdx = 0; upperIdx < step.getUppers().size(); upperIdx++) {
                    if(step.getTransformation().inputRegisterOutputInterference(upperIdx)[lowerIdx]){
                        liveUntil--;
                        break;
                    }
                }

                if (j >= all_r.size()) {
                    if(!initialGoals.contains(trueGoal)){
                        assert false;
                        return null;
                    }
                    int offset = initialGoals.indexOf(trueGoal);
                    requiresInit.get(offset).add(liveUntil);
                    j += offset;
                    k = 0;
                }
                liveness.get(j).set(k, Math.min(liveness.get(j).get(k), i));
                if(i==0){
                    live.add(new Tuple<>(j, k));
                    availableRegisters.remove(registers.get(lowerIdx));
                    lineMap.put(new Tuple<>(j, k), registers.get(lowerIdx));
                    map.put(trueGoal, registers.get(lowerIdx));
                }
            }
        }
        int[] initLastUsed = new int[init.size()];
        for (int i = 0; i < init.size(); i++) {
            if(requiresInit.get(i).isEmpty()) {
            initLastUsed[i] = Integer.MAX_VALUE;
            }else{
                initLastUsed[i] = Collections.min(requiresInit.get(i));
            }
        }

//        for (int i = 0; i < liveness.size(); i++) {
//            System.out.println(i + ":: " + liveness.get(i));
//        }


        for (int i = 0; i < liveness.size(); i++) {
            List<BRegister> trash = new ArrayList<>(availableRegisters);
//            System.out.println("Av " + availableRegisters);
            for (int u = 0; u < liveness.get(i).size(); u++) {
                if (live.contains(new Tuple<>(i, u))) {
                    live.remove(new Tuple<>(i, u));
                    availableRegisters.add(0, lineMap.get(new Tuple<>(i, u)));
                }
            }
            List<Tuple<Integer, Integer>> needAllocatingInit = new ArrayList<>();
            List<Tuple<Integer, Integer>> needAllocatingConstrained = new ArrayList<>();
            List<Tuple<Integer, Integer>> needAllocatingOther = new ArrayList<>();

            // iterate all uppers from the begining of the program up to i
            for (int j = liveness.size() - 1; j > i; j--) {
                for (int k = 0; k < liveness.get(j).size(); k++) {
                    // if the upper (jk) is live until i
                    if (liveness.get(j).get(k) == i) {
                        // and isn't already mapped (check for preallocation to enure outputs are in correct registers only)
                        if (!lineMap.containsKey(new Tuple<>(j, k))) {

                            if(j>= all_r.size()){// Upper is an Init so must be allocated first
                                needAllocatingInit.add(new Tuple<>(j, k));
                            }else {
                                //check if upper is used in a way that has constraints, should be allocated before the rest
                                boolean addFirst = false;

                                for (int l = 0; !addFirst && l < all_r.get(i).getLowers().size(); l++) {
                                    G trueLower = all_r.get(i).getLowerTrueGoal(l);
                                    if (j < all_r.size() && trueLower.equivalent(all_r.get(j).getUppers().get(k))) {
                                        for (int u = 0; !addFirst && u < all_r.get(i).getUppers().size(); u++) {
                                            if (all_r.get(i).getTransformation().inputRegisterOutputInterference(u)[l]) {
                                                addFirst = true;
                                            }
                                        }
                                    }

                                }
                                if(addFirst){
                                    needAllocatingConstrained.add(new Tuple<>(j, k));
                                } else {
                                    needAllocatingOther.add(new Tuple<>(j, k));
                                }
                            }
                        }
                    }
                }
            }
            List<Tuple<Integer, Integer>> needAllocating = new ArrayList<>();
            needAllocating.addAll(needAllocatingInit);
            needAllocating.addAll(needAllocatingConstrained);
            needAllocating.addAll(needAllocatingOther);
            for (Tuple<Integer, Integer> jk : needAllocating) {
                live.add(jk);
                BRegister r = null;
                if (availableRegisters.isEmpty()) {
                    return null;
                }
                if (jk.getA() >= all_r.size()) {
                    int initIdx = jk.getA() - all_r.size();
                    boolean valid = availableRegisters.remove(init.get(initIdx));
                    if (!valid) {
                        boolean initInAv = false;
                        for (BRegister register : registers) {
                            if (init.get(initIdx).equals(register)){
                                initInAv = true;
                                break;
                            }
                        }
                        if(initInAv) {
                            // Output is created and requires init Register while init is live!
                            return null;
                        }
                    }
                    r = init.get(initIdx);
                } else {
                    registerPicker:
                    for (BRegister availableRegister : availableRegisters) {
                        // Check if reg needs to be saved for init reg
                        for (int lu = 0; lu < initLastUsed.length; lu++) {
                            if (jk.getA() > initLastUsed[lu] && availableRegister.equals(init.get(lu))) {
                                continue registerPicker;
                            }
                        }
                        // Check if there are any constraints on other uses of proposed jk register

                        for (int l = 0; l < all_r.get(i).getLowers().size(); l++) {
                            G trueLower = all_r.get(i).getLowerTrueGoal(l);
                            if (trueLower.equivalent(all_r.get(jk.getA()).getUppers().get(jk.getB()))) {
                                for (int u = 0; u < all_r.get(i).getUppers().size(); u++) {
                                    if (all_r.get(i).getTransformation().inputRegisterOutputInterference(u)[l]) {
                                        if(lineMap.get(new Tuple<>(i, u)).equals(availableRegister)){
                                            continue registerPicker;
                                        }
                                    }
                                }
                            }

                        }
                        r = availableRegister;
                        break;
                    }
                    if (r == null) {
                        return null;
                    }
                    availableRegisters.remove(r);
                }
                trash.remove(r);
                if (jk.getA() < all_r.size()) {
                    map.put(all_r.get(jk.getA()).getUppers().get(jk.getB()), r);
                    lineMap.put(jk, r);
                } else {
                    // Initial cases
                    int initIdx = jk.getA() - all_r.size();
                    G trueGoal = initialTrueGoals.get(initIdx);
                    map.put(trueGoal, r);
                    lineMap.put(jk, r);

                }


            }
            map.putTrash(i, trash);

        }

        return map;
    }
    public static class BRegister extends RegisterAllocator.Register {
        public final int bank;
        public BRegister(int bank, String name) {
            super(name);
            this.bank = bank;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            BRegister bRegister = (BRegister) o;
            return this.bank == bRegister.bank;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), bank);
        }

        @Override
        public String toString() {
            return this.bank+"."+this.name;
        }
    }

    public static class Mapping<G extends Goal<G>> implements RegisterAllocator.Mapping<G> {
        private final Map<Wrapper, BRegister> map;
        private final Map<Integer, List<BRegister>> trashMap;
        private final List<BRegister> init;

        Mapping(List<BRegister> init) {
            this.init = init;
            this.map = new HashMap<>();
            this.trashMap = new HashMap<>();
        }

        @Override
        public List<BRegister> initRegisters(){
            return init;
        }

        private void put(G goal, BRegister register){
            BRegister r = this.map.put(new Wrapper(goal), register);
            assert r == null;
        }

        private void putTrash(int i, List<BRegister> registers){
            trashMap.put(i, registers);
        }

        @Override
        public List<BRegister> getTrash(int i){
            return trashMap.get(i);
        }

        @Override
        public BRegister get(G goal){
            return map.get(new Wrapper(goal));
        }

        @Override
        public String toString() {
            return map.entrySet().stream().map(e -> e.getKey().goal.getTableString(false, false, true, true) + "\n@" + Integer.toHexString(e.getKey().goal.hashCode()) + " -> " +e.getValue().toString()).collect(Collectors.joining("\n\n", "{\n", "\n}\n")) +
                    trashMap.entrySet().stream().map(e -> Integer.toString(e.getKey()) + " -> " +e.getValue()).collect(Collectors.joining(",\n ", "[\n", "\n]"));
        }

        private class Wrapper{
            private final G goal;

            private Wrapper(G goal) {
                this.goal = goal;
            }


            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Wrapper wrapper = (Wrapper) o;
                if(goal.getClass() != wrapper.goal.getClass()) return false;
                return goal.equivalent(wrapper.goal);
            }

            @Override
            public int hashCode() {

                return Objects.hash(goal);
            }
        }
    }
}
