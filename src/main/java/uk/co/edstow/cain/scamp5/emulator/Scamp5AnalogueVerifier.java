package uk.co.edstow.cain.scamp5.emulator;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.regAlloc.RegisterAllocator;
import uk.co.edstow.cain.fileRun.Verifier;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.scamp5.analogue.Scamp5AnalogueTransformation;
import uk.co.edstow.cain.structures.Bounds;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.Plan;
import uk.co.edstow.cain.util.Tuple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Scamp5AnalogueVerifier<G extends Kernel3DGoal<G>> implements Verifier<G, Scamp5AnalogueTransformation<G>, Register> {
    int verbose = 0;

    @Override
    public void verbose(int v) {
        verbose = v;
    }

    @Override
    public VerificationResult verify(String code, List<G> initialGoals, List<G> finalGoals, Plan<G,Scamp5AnalogueTransformation<G>,Register> plan, RegisterAllocator<G,Scamp5AnalogueTransformation<G>,Register> registerAllocator) {
        boolean passed = true;
//        List<G> finalGoals = reverseSearch.getFinalGoals();
        int[] initAtoms = initialGoals.stream().mapToInt(g -> g.get(0,0, g.bounds().getZMax())).toArray();
//        int[] divisions = reverseSearch.getInitialDivisions();
        List<Bounds> coverage = new ArrayList<>();
        double noise = 0;
        Bounds bounds = new Bounds.SimpleBounds(finalGoals.stream().map(Goal::bounds).collect(Collectors.toList()));
        List<Register> availableRegistersList = new ArrayList<>(registerAllocator.getAvailableRegistersArray());
        List<Register> initRegistersList = new ArrayList<>(registerAllocator.getInitRegisters());
        List<Register> regs = new ArrayList<>();
        regs.addAll(availableRegistersList);
        regs.addAll(initRegistersList);

        Scamp5Emulator emulator = Scamp5Emulator.newWithRegs((plan.bounds().largestMagnitude() + 1) * 3, regs);
        for (int i = 0; i < initRegistersList.size(); i++) {
            Register r = initRegistersList.get(i);
            emulator.run(String.format("input(%s,%d)", r, (initAtoms[i]) * 128));
        }
        emulator.pushCode(code);
        emulator.flushInstructionBuffer();
        for (int i = 0; i < finalGoals.size(); i++) {

            final String reg = registerAllocator.getAvailableRegistersArray().get(i).toString();
            Map<Tuple<Integer, Tuple<Integer, String>>, Double> testMap = emulator.getRawProcessingElementContains(0, 0, reg);
            noise += emulator.readNoise(0, 0, reg);

            Iterator<Tuple<Kernel3DGoal.Coord, Integer>> iterator = finalGoals.get(i).uniqueCountIterator();
            while (iterator.hasNext()) {
                Tuple<Kernel3DGoal.Coord, Integer> t = iterator.next();
                Tuple<Integer, Tuple<Integer, String>> coordinate = Tuple.triple(t.getA().x, t.getA().y, registerAllocator.getInitRegisters().get(t.getA().z).toString());
                Double d = testMap.get(coordinate);
                int expected = t.getB();
                if (d == null || Double.compare(expected, d) != 0) {
                    printLnCritial("INTEGRITY CHECK ERROR");
                    printLnCritial("In Goal: %d \n %s", i, finalGoals.get(i).getTableString(false, false, true, true));
                    printLnCritial(coordinate.toString());
                    printLnCritial("%s", d == null ? "null" : d);
                    printLnCritial("%s", expected);
                    printLnCritial(code);
                    printLnCritial(GoalBag.toGoalsString(finalGoals));
                    printLnCritial(plan.toGoalsString());
                    passed = false;
                }
                testMap.remove(coordinate);
            }
            if (!testMap.isEmpty()) {
                printLnCritial("INTEGRITY CHECK ERROR!");
                printLnCritial(testMap.toString());
                return null;
            }
            coverage.add(emulator.getRegCoverge(0, 0, reg));

        }
        return new Scamp5Result(passed, coverage, noise);
    }



    private void printLnCritial(String s, Object... args) {
        if(verbose>=0) System.out.println(String.format(s, args));
    }

    private void printLnCritial(String s) {
        if(verbose>=0) System.out.println(s);
    }

    static class Scamp5Result implements VerificationResult {
        private final boolean passed;
        public final List<Bounds> coverage;
        public final double noise;

        Scamp5Result(boolean passed, List<Bounds> coverage, double noise) {
            this.passed = passed;
            this.coverage = coverage;
            this.noise = noise;
        }

        @Override
        public boolean passed() {
            return passed;
        }

        @Override
        public String getInfo() {
            return new Tuple <>(new Bounds.SimpleBounds(coverage), noise).toString();
        }
    }


}
