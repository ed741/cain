package uk.co.edstow.cain;

import uk.co.edstow.cain.goals.Kernel3DGoal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Approximater<G extends Kernel3DGoal<G>> {
    private class Vector {
        final int[] val;
        private Vector(int... val) {
            this.val = val;
        }
    }
    private final List<Map<Vector, Double>> input;
    private final int maxDepth;
    private final double maxError;
    private int depth;
    private double error;
    private double maxCoefficient = 0;
    private double minCoefficient = Double.MAX_VALUE;

    public Approximater(int maxDepth, double maxError) {
        this.maxDepth = maxDepth;
        input = new ArrayList<>();
        this.maxError = maxError;
    }

    public void newGoal(){
        input.add(new HashMap<>());
    }

    public void put(int x, int y, int z, double coefficient){
        if (coefficient==0) return;
        input.get(input.size()-1).put(new Vector(x, y, z), coefficient);
        maxCoefficient = Math.max(maxCoefficient, Math.abs(coefficient));
        minCoefficient = Math.min(minCoefficient, Math.abs(coefficient));
    }

    public List<G> solve(Supplier<Kernel3DGoal.Kernel3DGoalFactory<G>> factoryfactory){
        depth = maxDepth;
        double totalError = 0;
        for (int i = -10; i <= maxDepth; i++) {
            totalError = 0;
            for (int g = 0; g < this.input.size(); g++) {
                Map<Vector, Double> vecDoubleMap = input.get(g);
                for (Map.Entry<Vector, Double> entry : vecDoubleMap.entrySet()) {

                    totalError += getError(i, entry.getValue());
                }
            }
            if (totalError <= maxError){
                depth = i;
                break;
            }
        }
        error = totalError;

        List<G> out = new ArrayList<>(input.size());
        for (Map<Vector, Double> vecDoubleMap : input) {
            Kernel3DGoal.Kernel3DGoalFactory<G> factory = factoryfactory.get();
            for (Map.Entry<Vector, Double> entry : vecDoubleMap.entrySet()) {
                if(entry.getValue()>=0){
                    factory.add(entry.getKey().val[0], entry.getKey().val[1], entry.getKey().val[2], getCount(depth, entry.getValue()));
                } else {
                    factory.add(entry.getKey().val[0], entry.getKey().val[1], entry.getKey().val[2], -getCount(depth, entry.getValue()));
                }
            }
            G g = factory.get();
            out.add(g);
        }
        input.clear();
        return out;
    }



    private int getCount(int divisions, double coefficient){
        return (int) Math.abs(Math.round(coefficient* Math.pow(2, divisions)));
    }


    private double getError(int divisions, double coefficient){
        double d =  Math.round(coefficient* Math.pow(2, divisions));
        d /= Math.pow(2, divisions);
        return Math.abs(coefficient - d);
    }

    public int getDepth() {
        return depth;
    }

    public double getError() {
        return error;
    }


}
