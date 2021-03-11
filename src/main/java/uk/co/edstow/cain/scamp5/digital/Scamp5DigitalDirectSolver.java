package uk.co.edstow.cain.scamp5.digital;

import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.DirectSolver;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;

public class Scamp5DigitalDirectSolver<G extends Kernel3DGoal<G>> extends DirectSolver<G, Scamp5DigitalTransformation<G>, Register> {
    private final Scamp5DigitalConfig scamp5DigitalConfig;

    public Scamp5DigitalDirectSolver(Scamp5DigitalConfig scamp5DigitalConfig) {
        this.scamp5DigitalConfig = scamp5DigitalConfig;
    }


    @Override
    public List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> solveDirectly(Context<G, Scamp5DigitalTransformation<G>, Register> context, GoalBag<G> goals){
        goals = new GoalBag<>(goals);
        List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> allPairs = new ArrayList<>();
        GoalBag<G> empties = new GoalBag<>();
        for (int i = goals.size() - 1; i >= 0; i--) {
            if(goals.get(i).allZero()){
                empties.add(goals.remove(i));
            }
        }
        if(!scamp5DigitalConfig.onlyMov()) {
            for (int i = 0; i < empties.size(); i++) {
                if (scamp5DigitalConfig.useRes2 && i + 1 < empties.size()) {
                    allPairs.add(new GoalPair<>(Arrays.asList(empties.get(i), empties.get(i + 1)), Collections.emptyList(), new Scamp5DigitalTransformation.Res_2<>(empties.get(i), empties.get(i + 1), scamp5DigitalConfig)));
                    i++;
                } else if (scamp5DigitalConfig.useRes) {
                    allPairs.add(new GoalPair<>(empties.get(i), Collections.emptyList(), new Scamp5DigitalTransformation.Res<>(empties.get(i), scamp5DigitalConfig)));
                }
            }
        }
        List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> pairList = super.solveDirectly(context, goals);
        if(pairList==null){
            return null;
        }
        allPairs.addAll(pairList);
        return allPairs;
    }

    @Override
    public Collection<Tuple<List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>>, G>> solveDirectly(Context<G, Scamp5DigitalTransformation<G>, Register> context, G goal) {
        ArrayList<Tuple<List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>>, G>> list = new ArrayList<>();

        if(context.initialGoals.contains(goal)){
            list.add(new Tuple<>(Collections.emptyList(), goal));
            return list;
        }

        //Res
        if(scamp5DigitalConfig.useRes && goal.allZero()){
            Scamp5DigitalTransformation.Res<G> res = new Scamp5DigitalTransformation.Res<>(goal, scamp5DigitalConfig);
            list.add(new Tuple<>(Collections.singletonList((new GoalPair<>(goal, Collections.emptyList(), res))), context.initialGoals.get(0)));
            return list;
        }

        //Divide
        if(scamp5DigitalConfig.useDiv) {
            try {
                if (goal.total() == 1d && goal.get(0, 0 ,goal.bounds().getZMax())==1) {
                    int z = goal.bounds().getZMax();
                    G ic = null;
                    for (G i : context.initialGoals) {
                        if (i.get(0,0,z)>0) {
                            ic = i;
                        }
                    }
                    if (ic != null) {
                        List<GoalPair<G, Scamp5DigitalTransformation<G>, Register>> pairs = new ArrayList<>();
                        Scamp5DigitalTransformation.Div<G> div = new Scamp5DigitalTransformation.Div<>(goal, true, scamp5DigitalConfig);
                        pairs.add(new GoalPair<>(div.applyOpForwards(), Collections.singletonList(div.a), div));
                        while (div.a.totalI() < ic.totalI()) {
                            div = new Scamp5DigitalTransformation.Div<>(div.a, true, scamp5DigitalConfig);
                            pairs.add(new GoalPair<>(div.applyOpForwards(), Collections.singletonList(div.a), div));
                        }
                        if (div.a.equals(ic)) {
                            list.add(new Tuple<>(pairs, ic));
                        }
                    }
                }

            } catch (Transformation.TransformationApplicationException e) {
                e.printStackTrace();
                // Should be unreachable
                assert false;
            }
        }

        // AddSelf (mul2)
        if(scamp5DigitalConfig.useAddSelf) {
            Scamp5DigitalTransformation.AddSelf<G> addSelf = new Scamp5DigitalTransformation.AddSelf<>(goal, true, scamp5DigitalConfig);
            if(addSelf.isPossible()) {
                list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, addSelf.a, addSelf)), addSelf.a));
            }
        }

        //Move x
        if(scamp5DigitalConfig.useMovx) {
            for (Scamp5DigitalTransformation.Dir dir : Scamp5DigitalTransformation.Dir.values()) {
                Scamp5DigitalTransformation.Movx<G> movx = new Scamp5DigitalTransformation.Movx<>(goal, dir, true, scamp5DigitalConfig);
                list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, movx.a, movx)), movx.a));
            }
        }

        return list;
    }

    @Override
    public Scamp5DigitalTransformation<G> getDummyTransformation(List<G> upperGoals, List<G> lowerGoals, Context<G, Scamp5DigitalTransformation<G>, Register> context) {
        return new Scamp5DigitalTransformation.Null<>(lowerGoals.size(), upperGoals.size(), this.scamp5DigitalConfig);
    }

}
