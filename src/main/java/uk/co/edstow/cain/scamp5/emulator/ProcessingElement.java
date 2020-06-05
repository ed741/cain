package uk.co.edstow.cain.scamp5.emulator;

import uk.co.edstow.cain.structures.Atom;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;
import java.util.stream.Collectors;

import static uk.co.edstow.cain.scamp5.emulator.Scamp5Emulator.*;

class ProcessingElement {

    private static class UndefinedBusBehaviour extends RuntimeException{
        UndefinedBusBehaviour(String s) {
            super(s);
        }
    }

    private final Pos position;
    private final NoiseConfig noiseConfig;
    private final Map<Reg,RegisterState> registers;
    private boolean enabled;

    ProcessingElement(Pos position, NoiseConfig noiseConfig, Reg[] regs ) {
        this.position = position;
        this.noiseConfig = noiseConfig;
        registers = new HashMap<>();
        for (Reg reg : regs) {
            registers.put(reg, new RegisterState());
        }
        enabled = true;
    }

    public void addNeighbourRegister(Reg r1, ProcessingElement e, Reg r2){
        registers.put(r1, e.registers.get(r2));
    }

    public void addExtraRegister(Reg r1) {
        registers.put(r1, new RegisterState());
    }

    private boolean regDuplicates(Reg... regs){
        for (int i = 0; i < regs.length; i++) {
            for (int j = i + 1 ; j < regs.length; j++) {
                if (regs[i].equals(regs[j])) {
                    return true;
                }
            }
        }
        return false;
    }

    public void where(Reg r){
        enabled = registers.get(r).readValue()>0;
    }

    public void all(){
        enabled = true;
    }


    public void bus(Reg r){
        if(!enabled){
            return;
        }
        RegisterState state = registers.get(r);
        state.writeOver();
    }

    public void bus(Reg r1, Reg r2){
        if(!enabled){
            return;
        }
        if(regDuplicates(r1, r2)){
            throw new UndefinedBusBehaviour("bus/2 cannot have duplicate registers");
        }
        registers.get(r1).writeOver();
        registers.get(r1).addNegate(registers.get(r2));
    }

    public void bus(Reg r1, Reg r2, Reg r3){
        if(!enabled){
            return;
        }
        if(regDuplicates(r1, r2, r3)){
            throw new UndefinedBusBehaviour("bus/3 cannot have duplicate registers");
        }
        registers.get(r1).writeOver();
        registers.get(r1).addNegate(registers.get(r2), registers.get(r3));
    }

    public void bus(Reg r1, Reg r2, Reg r3, Reg r4){
        if(!enabled){
            return;
        }
        if(regDuplicates(r1, r2, r3, r4)){
            throw new UndefinedBusBehaviour("bus/4 cannot have duplicate registers");
        }
        registers.get(r1).writeOver();
        registers.get(r1).addNegate(registers.get(r2), registers.get(r3), registers.get(r4));
    }

    public void bus2(Reg r1, Reg r2){
        if(!enabled){
            return;
        }
        if(regDuplicates(r1, r2)){
            throw new UndefinedBusBehaviour("bus2/2 cannot have duplicate registers");
        }
        registers.get(r1).writeOver();
        registers.get(r2).writeOver();
    }

    public void bus2(Reg r1, Reg r2, Reg r3){
        if(!enabled){
            return;
        }
        if(regDuplicates(r1, r2, r3)){
            throw new UndefinedBusBehaviour("bus2/3 cannot have duplicate registers");
        }
        registers.get(r1).writeOver();
        registers.get(r2).writeOver();
        registers.get(r1).addNegate(0.5, registers.get(r3));
        registers.get(r2).addNegate(0.5, registers.get(r3));
    }

    public void input(Reg r, int i){
        if(!enabled){
            return;
        }
        RegisterState state = registers.get(r);
        state.writeOver();
        double value = ((double)i)/128d;
        state.contains.put(new OriginPos(position.x, position.y, r), value);
        state.writeNoise += noiseConfig.writeNoiseConstant + (noiseConfig.writeNoiseFactor *Math.abs(value));
        state.coverage.add(this.position);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<Goal> goals = new ArrayList<>();
        sb.append("PE ").append(position.toString()).append(":\n");
        registers.entrySet().stream()
                .sorted(Comparator.comparing(e->e.getKey().toString(), Comparator.naturalOrder()))
                .forEachOrdered(e-> {
                    if(verbose > 15) {
                        sb.append(e.getKey().toString())
                          .append(": ")
                          .append(e.getValue().getContained())
                          .append(" | ")
                          .append(e.getValue().getNoises())
                          .append(" | Value: ")
                          .append(e.getValue().value())
                          .append("\n");
                    }
                    Goal.Factory factory = new Goal.Factory();
                    e.getValue().getRawContains().forEach((tuple, d) -> {
                        if(d!=0) {
                            factory.add(new Atom(tuple.getA(), tuple.getB().getA(), 0, d >= 0), Math.abs(d.intValue()));
                        }
                    });
                    goals.add(factory.get());
                });
        sb.append("\n").append(GoalBag.toGoalsString(goals)).append("\n");
        return sb.toString();
    }

    public double readOutValue(Reg reg) {
        return registers.get(reg).readValue();
    }

    public double getValue(Reg reg) {
        return registers.get(reg).value();
    }

    public Map<Tuple<Integer,Tuple<Integer,String>>,Double> getRawRegisterContains(Reg r) {
        RegisterState registerState = this.registers.get(r);
        if(registerState == null){
            return null;
        }
        return registerState.getRawContains();
    }

    public String getRegToString(Reg r) {
        RegisterState registerState = this.registers.get(r);
        if(registerState == null){
            return null;
        }
        return registerState.toString();
    }


    private class RegisterState {
        private final Set<Pos> coverage;
        private final Map<OriginPos, Double> contains;
        private double writeNoise;
        private double readNoise;

        RegisterState() {
            contains = new HashMap<>();
            coverage = new HashSet<>();
        }

        private Pos getPos(){return position;}

        double readValue(){
            double sum = 0;
            for (Map.Entry<OriginPos, Double> posDoubleEntry : contains.entrySet()) {
                sum += posDoubleEntry.getValue();
            }
            readNoise += noiseConfig.readNoiseConstant + (noiseConfig.readNoiseFactor * (sum + readNoise + writeNoise));
            sum += noiseConfig.getValue(readNoise+writeNoise);
            return sum;
        }

        double value(){
            double sum = 0;
            for (Map.Entry<OriginPos, Double> posDoubleEntry : contains.entrySet()) {
                sum += posDoubleEntry.getValue();
            }
            sum += noiseConfig.getValue(readNoise+writeNoise);
            return sum;
        }

        void writeOver() {
            contains.clear();
            coverage.clear();
            coverage.add(getPos());
            writeNoise = noiseConfig.writeNoiseConstant;
            readNoise = 0;
        }

        void addNegate(RegisterState... states){
            addNegate(1, states);
        }

        void addNegate(double factor, RegisterState... states){

            double magnitude = 0;
            for (RegisterState state : states) {
                coverage.addAll(state.coverage);
                coverage.add(state.getPos());
                double stateMagnitude = state.writeNoise + state.readNoise;
                for (Map.Entry<OriginPos, Double> entry : state.contains.entrySet()) {
                    double c = contains.getOrDefault(entry.getKey(), 0d);
                    contains.put(entry.getKey(), c - (entry.getValue()*factor));
                    stateMagnitude += Math.abs(entry.getValue());
                }
                state.readNoise += noiseConfig.readNoiseConstant + (noiseConfig.readNoiseFactor * stateMagnitude);
                magnitude += stateMagnitude;
            }
            writeNoise += noiseConfig.writeNoiseConstant + (noiseConfig.writeNoiseFactor *magnitude);
        }

        @Override
        public String toString() {
            return "RegisterState{" +
                    "contains=" + contains +
                    ", writeNoise=" + writeNoise +
                    ", readNoise=" + readNoise +
                    ", coverage:\n" + new Goal.Factory(coverage.stream().map(p -> new Atom(p.x, p.y, 0, true)).collect(Collectors.toList())).get().getCharTableString(false, false, false, true)+"\n"+
                    '}';
        }

        String getContained(){
            StringBuilder sb = new StringBuilder("{");
            Iterator<Map.Entry<OriginPos,Double>> it = contains.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<OriginPos, Double> entry = it.next();
                OriginPos k = entry.getKey();
                Double v = entry.getValue();
                sb.append(String.format("(%d,%d,%s)=%f", k.x, k.y, k.r.toString(), v));
                if (it.hasNext()) {
                    sb.append(',');
                }
            }

            sb.append('}');
            return sb.toString();
        }

        String getNoises() {
            return "(Read Noise: " + readNoise + ", Write Noise: " + writeNoise + ")";
        }

        Map<Tuple<Integer, Tuple<Integer, String>>, Double> getRawContains() {
            Map<Tuple<Integer, Tuple<Integer, String>>, Double> out = new HashMap<>();
            contains.forEach((pos, value) -> out.put(Tuple.triple(pos.x, pos.y, pos.r.toString()), value));
            return out;
        }
    }
}
