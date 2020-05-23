package uk.co.edstow.cpacgen;

import java.util.*;

public class RegisterAllocator {
    private final Register[] registers;
    private final Register[] init;

    public RegisterAllocator(Register[] init, Register... r) {
        this.init = init;
        registers = r;

    }

    public int getAvailableRegisters() {
        return registers.length;
    }

    public Register[] getInitRegisters() {
        return init;
    }

    public Mapping solve(Plan plan, List<Goal> initialGoals){
        System.out.println(plan);
        List<Plan.Step> all_r = plan.getAll();
        List<Set<Integer>> requiresInit = new ArrayList<>(init.length);
        for (Register i : init) {
            requiresInit.add(new HashSet<>());
        }


        int[] liveness = new int[all_r.size()+initialGoals.size()];


        List<Register> availableRegisters = new ArrayList<>(Arrays.asList(registers));
        Set<Integer> live = new HashSet<>();
        Mapping map = new Mapping();
        Map<Integer, Register> lineMap = new HashMap<>();

        for (int i = 0; i < liveness.length; i++) {
            liveness[i] = i;
        }
        for (int i = 0; i < all_r.size(); i++) {
            Plan.Step step = all_r.get(i);

            List<Goal> lowers = step.getLowers();
            for (int lowerIdx = 0; lowerIdx < lowers.size(); lowerIdx++) {
                Goal trueGoal = step.getLowerTrueGoal(lowerIdx);
                int j = i + 1;
                while (j < all_r.size() && !all_r.get(j).getUpper().equivalent(trueGoal)) {
                    j++;
                }
                int liveUntil = i;
                if(step.getTransformation().inputRegisterOutputInterference()[lowerIdx]){
                    liveUntil--;
                }
                if (j >= all_r.size()) {
                    if(!initialGoals.contains(trueGoal)){
                        assert false;
                        return null;
                    }
                    int offset = initialGoals.indexOf(trueGoal);
                    requiresInit.get(offset).add(liveUntil);
                    j += offset;
                }
                liveness[j] = Math.min(liveness[j], liveUntil);
                if(i==0){
                    live.add(j);
                    availableRegisters.remove(registers[lowerIdx]);
                    lineMap.put(j, registers[lowerIdx]);
                    map.put(trueGoal, registers[lowerIdx]);
                }
            }
        }
        int[] initLastUsed = new int[init.length];
        for (int i = 0; i < init.length; i++) {
            initLastUsed[i] = Collections.min(requiresInit.get(i));
        }


        for (int i = 0; i < liveness.length; i++) {
            if(live.contains(i)){
                live.remove(i);
                availableRegisters.add(0, lineMap.get(i));
            }
            List<Integer> needAllocating = new ArrayList<>();
            for (int j = liveness.length-1; j > i; j--) {
                if(liveness[j] == i){
                    if(!lineMap.containsKey(j)) {
                        if (i < all_r.size() && j < all_r.size() && all_r.get(i).getLowers().contains(all_r.get(j).getUpper())) {
                            needAllocating.add(0, j);
                        } else {
                            needAllocating.add(j);
                        }
                    }
                }
            }
            for (int j : needAllocating) {
                live.add(j);
                Register r = null;
                if(availableRegisters.isEmpty()){
                    return null;
                }
                if(j >= all_r.size()){
                    int initIdx = j-all_r.size();
                    boolean valid = availableRegisters.remove(init[initIdx]);
                    if(!valid){
                        // Output is created and requires init Register while init is live!
                        return null;
                    }
                    r = init[initIdx];
                } else {
                    for (Register availableRegister : availableRegisters) {
                        boolean safe = true;
                        for (int k = 0; k < initLastUsed.length; k++) {
                            if(j > initLastUsed[k] && availableRegister == init[k]){
                                safe = false;
                            }
                        }
                        if(safe){
                            r = availableRegister;
                            break;
                        }
                    }
                    if(r==null){
                        return null;
                    }
                    availableRegisters.remove(r);
                }
                if(j < all_r.size()) {
                    map.put(all_r.get(j).getUpper(), r);
                    lineMap.put(j, r);
                }else{
                    // Initial cases
                    int initIdx = j-all_r.size();
                    List<Goal> lowers = all_r.get(i).getLowers();
                    Goal trueGoal = all_r.get(i).getLowerTrueGoal(lowers.indexOf(initialGoals.get(initIdx)));
                    map.put(trueGoal, r);
                    lineMap.put(j, r);

                }

            }
        }

        return map;
    }


    public enum Register{
        A,B,C,D,E,F
    }

    public class Mapping {
        Mapping() {
            this.map = new HashMap<>();
        }

        private void put(Goal goal, Register register){
            Register r = this.map.put(new Wrapper(goal), register);
            assert r == null;
        }

        public Register get(Goal goal){
            return map.get(new Wrapper(goal));
        }

        private class Wrapper {
            private final Goal goal;

            private Wrapper(Goal goal) {
                this.goal = goal;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Wrapper wrapper = (Wrapper) o;
                return goal == wrapper.goal;
            }

            @Override
            public int hashCode() {

                return Objects.hash(goal);
            }
        }
        private final Map<Wrapper, Register> map;
    }
}
