package cpacgen;

import java.lang.reflect.Array;
import java.util.*;

public class RegisterAllocator {
    private final Plan plan;
    private Register[] registers;
    private final Register init;

    public RegisterAllocator(Plan plan, Register init, Register... r) {
        this.plan = plan;
        this.init = init;
        registers = r;

    }

    public Map<Integer, Register> solve(){

        List<Plan.Step> all_r = plan.getAll();
        Set<Integer> requiresInit = new HashSet<>();
        int[] liveness = new int[all_r.size()+1];
        for (int i = 0; i < liveness.length; i++) {
            liveness[i] = i;
        }
        for (int i = 0; i < all_r.size(); i++) {
            Plan.Step step = all_r.get(i);
            for (Goal goal : step.getLowers()) {
                int j = i+1;
                while(j < all_r.size() && !all_r.get(j).getUpper().equals(goal)){
                    j++;
                }
                if(j == all_r.size()){
                    assert goal == all_r.get(all_r.size()-1).getLowers().get(0);
                    requiresInit.add(i);
                }
                liveness[j] = Math.min(liveness[j], i);
            }
        }
        int initLastUsed = Collections.min(requiresInit);

        List<Register> availableRegisters = new ArrayList<>(Arrays.asList(registers));
        Set<Integer> live = new HashSet<>();
        Map<Integer, Register> map = new HashMap<>();
        for (int i = 0; i < liveness.length; i++) {
            if(live.contains(i)){
                live.remove(i);
                availableRegisters.add(0, map.get(i));
            }
            for (int j = liveness.length-1; j > i; j--) {
                if(liveness[j] == i){
                    live.add(j);
                    Register r = null;

                    if(j == liveness.length-1){
                        boolean valid = availableRegisters.remove(init);
                        System.out.println("Pos " + j + " using Init " + valid);
                        assert valid;
                        r = init;
                    } else if (j > initLastUsed) {
                        for (Register availableRegister : availableRegisters) {
                            if (availableRegister!=init){
                                r = availableRegister;
                                break;
                            }
                        }
                        assert r != null;
                        availableRegisters.remove(r);
                    } else {
                        r = availableRegisters.remove(0);
                    }
                    map.put(j, r);
                }
            }
        }

        return map;
    }


    public enum Register{
        A,B,C,D,E,F;
    }
}
