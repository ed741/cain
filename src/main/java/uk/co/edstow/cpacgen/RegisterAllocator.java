package uk.co.edstow.cpacgen;

import java.util.*;

public class RegisterAllocator {
    private final Register[] registers;
    private final Register init;

    public RegisterAllocator(Register init, Register... r) {
        this.init = init;
        registers = r;

    }

    public int getAvailableRegisters() {
        return registers.length;
    }

    public Register getInitRegister() {
        return init;
    }

    public Mapping solve(Plan plan){

        List<Plan.Step> all_r = plan.getAll();
        Set<Integer> requiresInit = new HashSet<>();
        int[] liveness = new int[all_r.size()+1];


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
                if (j == all_r.size()) {
                    assert trueGoal.same(all_r.get(all_r.size() - 1).getLowers().get(0));
                    requiresInit.add(liveUntil);
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
        int initLastUsed = Collections.min(requiresInit);


        for (int i = 1; i < liveness.length; i++) {
            if(live.contains(i)){
                live.remove(i);
                availableRegisters.add(0, lineMap.get(i));
            }
            List<Integer> needAllocating = new ArrayList<>();
            for (int j = liveness.length-1; j > i; j--) {
                if(liveness[j] == i){
                    if(i < all_r.size() && j < all_r.size() && all_r.get(i).getLowers().contains(all_r.get(j).getUpper())){
                        needAllocating.add(0, j);
                    } else {
                        needAllocating.add(j);
                    }
                }
            }
            for (int j : needAllocating) {
                live.add(j);
                Register r = null;
                if(availableRegisters.isEmpty()){
                    System.out.println("CANNOT MAKE ALLOCATION "+plan.toGoalsString());
                    System.out.println(String.format("i: %d,  j: %d,  \navailable reg: %s,  \nlive: %s,  \nlineMapping: %s,  \nmapping: %s", i, j, availableRegisters, live, lineMap, map));
                    System.exit(-1);
                    return null;
                }
                if(j == liveness.length-1){
                    boolean valid = availableRegisters.remove(init);
                    assert valid;
                    r = init;
                } else if (j > initLastUsed) {
                    for (Register availableRegister : availableRegisters) {
                        if (availableRegister!=init){
                            r = availableRegister;
                            break;
                        }
                    }
                    if(r==null){
                        return null;
                    }
                    availableRegisters.remove(r);
                } else {
                    r = availableRegisters.remove(0);
                }
                if(j < all_r.size()) {
                    map.put(all_r.get(j).getUpper(), r);
                    lineMap.put(j, r);
                }else{
                    // Initial case
                    map.put(all_r.get(all_r.size()-1).getLowerTrueGoal(0), r);
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
