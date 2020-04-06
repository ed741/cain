package cpacgen;

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

    public Map<Goal, Register> solve(){
        Map<Goal, Register> map = new HashMap<>();
        List<Register> availableReg = new ArrayList<>(Arrays.asList(registers));

        List<Plan.Step> all = plan.getAll();

        Goal initalGoal = all.get(all.size()-1).getLowers().get(0);
        map.put(initalGoal, init);

        for (Plan.Step step: plan.getAll()) {
            if(step.getUpper() != null && !step.getUpper().equals(initalGoal)) {
                availableReg.add(0, map.get(step.getUpper()));
            }
            for (Goal goal: step.getLowers()) {
                if(!map.containsKey(goal)) {
                    map.put(goal, availableReg.remove(0));
                }
            }
        }
        return map;
    }


    public enum Register{
        A,B,C,D,E,F;
    }
}
