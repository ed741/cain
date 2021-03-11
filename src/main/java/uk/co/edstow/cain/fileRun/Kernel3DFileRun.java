package uk.co.edstow.cain.fileRun;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.co.edstow.cain.Approximater;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.transformations.Transformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class Kernel3DFileRun<G extends Kernel3DGoal<G>, T extends Transformation<R>, R extends Register> extends FileRun<G, T, R> {
    protected int approximationDepth;

    public Kernel3DFileRun(JSONObject config) {
        super(config);
    }


    protected abstract Kernel3DGoal.Kernel3DGoalFactory<G> getGoalFactory(R reg);

    abstract protected List<R> getOutputRegisters();

    protected abstract List<R> getInputRegisters();

    protected boolean isThreeDimensional() {
        boolean threeDimensional = config.getBoolean("3d");
        printLn("Three Dimensional       : " + threeDimensional);
        return threeDimensional;
    }
    @Override
    protected List<G> makeFinalGoals() {

        int maxApproximationDepth = config.getInt("maxApproximationDepth");
        printLn("Max Approximation Depth : " + maxApproximationDepth);
        double maxApproximationError = config.getDouble("maxApproximationError");
        printLn("Max Approximation Error : " + maxApproximationError);

        Approximater<G> goalAprox = new Approximater<G>(maxApproximationDepth, maxApproximationError);


        boolean threeDimensional = isThreeDimensional();
        JSONObject filter = config.getJSONObject("filter");
        printLn("Kernels                 : " + filter.length());
        Iterator<String> filters = filter.keySet().stream().sorted().iterator();
        while (filters.hasNext()) {
            String reg = filters.next();
            Object o = filter.get(reg);
            if (o instanceof JSONArray) {
                addGoal(goalAprox, (JSONArray) o, threeDimensional, 1);
            } else {
                double scale = 1;
                if (filter.getJSONObject(reg).has("scale")) {
                    scale = filter.getJSONObject(reg).getDouble("scale");
                }
                if (filter.getJSONObject(reg).has("depth")) {
                    scale *= Math.pow(2, filter.getJSONObject(reg).getDouble("depth"));
                }
                addGoal(goalAprox, filter.getJSONObject(reg).getJSONArray("array"), threeDimensional, scale);
            }
        }

        List<R> outputRegisters = getOutputRegisters();
        List<G> finalGoals = goalAprox.solve(i -> getGoalFactory(outputRegisters.get(i)));
        this.approximationDepth = goalAprox.getDepth();
        printLn("Output Registers        : " + outputRegisters);
        printLn("\tApproximated goals:");
        printLn(GoalBag.toGoalsString(finalGoals, false, false, true, true));
        printLn("");
        printLn("Approximation Depth     : " + goalAprox.getDepth());
        printLn("Approximation Error     : " + goalAprox.getError());
        return finalGoals;
    }

    @Override
    protected List<G> makeInitialGoals() {
        int[] divisions = getInitDivisions();
        List<R> inputRegisters = getInputRegisters();
        List<G> initialGoals = new ArrayList<>();
        for (int i = 0; i < divisions.length; i++) {
            int division = divisions[i];
            initialGoals.add(getGoalFactory(inputRegisters.get(i)).add(0, 0, i, 1 << division).get());
        }
        return initialGoals;
    }

    protected int[] getInitDivisions() {
        int[] divisions = new int[config.getJSONObject("registerAllocator").getJSONArray("initialRegisters").length()];
        Arrays.fill(divisions, this.approximationDepth);
        return divisions;
    }


    protected Verifier<G, T, R> makeVerifier() {
        String verf = config.getString("verifier");
        switch (verf) {
            case "None":
                return Verifier.SkipVerify();
            default:
                throw new IllegalArgumentException("Verifier Unknown");
        }
    }

    private static <G extends Kernel3DGoal<G>> void addGoal(Approximater<G> goalAprox, JSONArray jsonArray, boolean threeDimentional, double scale) {
        int xMax = 0;
        int yMax = jsonArray.length();
        int zMax = 0;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray row = jsonArray.getJSONArray(i);
            xMax = Math.max(xMax, row.length());
            if (threeDimentional) {
                for (int j = 0; j < row.length(); j++) {
                    zMax = Math.max(zMax, row.getJSONArray(j).length());
                }
            }
        }

        int xOffset = xMax / 2;
        int yOffset = yMax / 2;
        int zOffset = zMax / 2;

        goalAprox.newGoal();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray row = jsonArray.getJSONArray(i);
            for (int j = 0; j < row.length(); j++) {
                Object o = row.get(j);
                if (o instanceof JSONArray) {
                    JSONArray array = (JSONArray) o;
                    for (int k = 0; k < array.length(); k++) {
                        double coefficant = array.getDouble(k);
                        int z = k;
                        goalAprox.put(j - xOffset, -(i - yOffset), z - zOffset, coefficant * scale);
                    }
                } else if (!threeDimentional) {
                    double coefficant = row.getDouble(j);
                    goalAprox.put(j - xOffset, -(i - yOffset), 0, coefficant * scale);
                } else {
                    throw new IllegalArgumentException("Cannot parse kernel: " + jsonArray);
                }
            }
        }
    }

}
