package uk.co.edstow.cain.scamp5.analogue;

import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.DirectSolver;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.structures.Bounds;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;

public class Scamp5AnalogueDirectSolver<G extends Kernel3DGoal<G>> extends DirectSolver<G, Scamp5AnalogueTransformation<G>, Register> {
    private final Scamp5AnalogueConfig scamp5AnalogueConfig;

    public Scamp5AnalogueDirectSolver(Scamp5AnalogueConfig scamp5AnalogueConfig) {
        this.scamp5AnalogueConfig = scamp5AnalogueConfig;
    }


    @Override
    public List<GoalPair<G, Scamp5AnalogueTransformation<G>, Register>> solveDirectly(Context<G, Scamp5AnalogueTransformation<G>, Register> context, GoalBag<G> goalsIn){
        GoalBag<G> goals = new GoalBag<>(goalsIn);
        List<GoalPair<G, Scamp5AnalogueTransformation<G>, Register>> allPairs = new ArrayList<>();
        GoalBag<G> empties = new GoalBag<>();
        for (int i = goals.size() - 1; i >= 0; i--) {
            if(goals.get(i).allZero()){
                empties.add(goals.remove(i));
            }
        }
        if(!scamp5AnalogueConfig.onlyMov) {
            for (int i = 0; i < empties.size(); i++) {
                if (scamp5AnalogueConfig.useRes2 && i + 1 < empties.size()) {
                    allPairs.add(new GoalPair<>(Arrays.asList(empties.get(i), empties.get(i + 1)), Collections.emptyList(), new Scamp5AnalogueTransformation.Res_2<>(empties.get(i), empties.get(i + 1), scamp5AnalogueConfig)));
                    i++;
                } else if (scamp5AnalogueConfig.useRes) {
                    allPairs.add(new GoalPair<>(empties.get(i), Collections.emptyList(), new Scamp5AnalogueTransformation.Res<>(empties.get(i), scamp5AnalogueConfig)));
                }
            }
        }
        for (int i = goals.size() - 1; i >= 0; i--) {
            G goal = goals.get(i);
            for (int j = 0; j < i; j++) {
                G g = goals.get(j);
                if(goal.same(g)){
                    goals.remove(i);
                    allPairs.add(new GoalPair<>(goal, g, new Scamp5AnalogueTransformation.Mov<>(goal,  scamp5AnalogueConfig)));
                }
            }
        }

        List<GoalPair<G, Scamp5AnalogueTransformation<G>, Register>> pairList = super.solveDirectly(context, goals);
        if(pairList==null){
            return null;
        }
        allPairs.addAll(pairList);
        return allPairs;
    }

    @Override
    public Collection<Tuple<List<GoalPair<G, Scamp5AnalogueTransformation<G>, Register>>, G>> solveDirectly(Context<G, Scamp5AnalogueTransformation<G>, Register> context, G goal) {
        ArrayList<Tuple<List<GoalPair<G, Scamp5AnalogueTransformation<G>, Register>>, G>> list = new ArrayList<>();

        if(context.initialGoals.contains(goal)){
            list.add(new Tuple<>(Collections.emptyList(), goal));
            return list;
        }

        //Res
        if(scamp5AnalogueConfig.useRes && goal.allZero()){
            Scamp5AnalogueTransformation.Res<G> res = new Scamp5AnalogueTransformation.Res<>(goal, scamp5AnalogueConfig);
            list.add(new Tuple<>(Collections.singletonList((new GoalPair<>(goal, Collections.emptyList(), res))), context.initialGoals.get(0)));
            return list;
        }

        //Negate
        if(scamp5AnalogueConfig.useNeg) {
            Scamp5AnalogueTransformation.Neg<G> neg = new Scamp5AnalogueTransformation.Neg<>(goal, true, scamp5AnalogueConfig);
            if (context.initialGoals.contains(neg.a)) {
                list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(neg.applyForwards(), neg.a, neg)), neg.a));
            }
        }

        //Divide
        if(scamp5AnalogueConfig.useDiv3 || scamp5AnalogueConfig.useDiv4) {
            try {
                Bounds goalBounds = goal.bounds();
                if (goalBounds.largestMagnitude(true, true, false) == 0 && goalBounds.getZMax() - goalBounds.getZMin() == 0) {
                    int z = goalBounds.getZMax();
                    G ic = null;
                    for (G i : context.initialGoals) {
                        if (i.get(0,0,z)>0) {
                            ic = i;
                        }
                    }
                    if (ic != null) {
                        List<GoalPair<G, Scamp5AnalogueTransformation<G>, Register>> pairs = new ArrayList<>();
                        Scamp5AnalogueTransformation.Div<G> div = new Scamp5AnalogueTransformation.Div<>(goal, true, scamp5AnalogueConfig.useDiv3, scamp5AnalogueConfig);
                        pairs.add(new GoalPair<>(div.applyOpForwards(), Collections.singletonList(div.a), div));
                        while (div.a.totalI() < ic.totalI()) {
                            div = new Scamp5AnalogueTransformation.Div<>(div.a, true, scamp5AnalogueConfig.useDiv3, scamp5AnalogueConfig);
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
        } else if(scamp5AnalogueConfig.useDivq) {
            try {
                Bounds goalBounds = goal.bounds();
                if (goalBounds.largestMagnitude(true, true, false) == 0 && goalBounds.getZMax() - goalBounds.getZMin() == 0) {
                    int z = goalBounds.getZMax();
                    G ic = null;
                    for (G i : context.initialGoals) {
                        if (i.get(0,0,z)>0) {
                            ic = i;
                        }
                    }
                    if (ic != null) {
                        List<GoalPair<G, Scamp5AnalogueTransformation<G>, Register>> pairs = new ArrayList<>();
                        Scamp5AnalogueTransformation.Divq<G> div = new Scamp5AnalogueTransformation.Divq<>(goal, true, scamp5AnalogueConfig);
                        pairs.add(new GoalPair<>(div.applyOpForwards(), Collections.singletonList(div.a), div));
                        while (div.a.totalI() < ic.totalI()) {
                            div = new Scamp5AnalogueTransformation.Divq<>(div.a, true, scamp5AnalogueConfig);
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

        //Move x
        if(scamp5AnalogueConfig.useMovx) {
            for (Scamp5AnalogueTransformation.Dir dir : Scamp5AnalogueTransformation.Dir.values()) {
                Scamp5AnalogueTransformation.Movx<G> movx = new Scamp5AnalogueTransformation.Movx<>(goal, dir, true, scamp5AnalogueConfig);
                if (context.initialGoals.contains(movx.a)) {
                    list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, movx.a, movx)), movx.a));
                }
            }
        }

        //Move 2x
        if(scamp5AnalogueConfig.useMov2x) {
            for (Scamp5AnalogueTransformation.Dir dir1 : Scamp5AnalogueTransformation.Dir.values()) {
                Scamp5AnalogueTransformation.Mov2x<G> mov2xa = new Scamp5AnalogueTransformation.Mov2x<>(goal, dir1, dir1, true, scamp5AnalogueConfig);
                if(context.initialGoals.contains(mov2xa.a)) {
                    list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, mov2xa.a, mov2xa)), mov2xa.a));
                }
                Scamp5AnalogueTransformation.Mov2x<G> mov2xb = new Scamp5AnalogueTransformation.Mov2x<>(goal, dir1, dir1.cw(), true, scamp5AnalogueConfig);
                if(context.initialGoals.contains(mov2xb.a)) {
                    list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, mov2xb.a, mov2xb)), mov2xb.a));
                }
            }
        }

        return list;
    }


    @Override
    public Scamp5AnalogueTransformation<G> getDummyTransformation(List<G> upperGoals, List<G> lowerGoals, Context<G, Scamp5AnalogueTransformation<G>, Register> context) {
        return new Scamp5AnalogueTransformation.Null<>(lowerGoals.size(), upperGoals.size(), scamp5AnalogueConfig);
    }

}
