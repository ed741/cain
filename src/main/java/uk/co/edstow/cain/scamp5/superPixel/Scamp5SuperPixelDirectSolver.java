package uk.co.edstow.cain.scamp5.superPixel;

import uk.co.edstow.cain.goals.BankedKernel3DGoal;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.pairgen.Context;
import uk.co.edstow.cain.pairgen.DirectSolver;
import uk.co.edstow.cain.pairgen.PairGen;
import uk.co.edstow.cain.pairgen.PairGenFactory;
import uk.co.edstow.cain.regAlloc.BRegister;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.scamp5.digital.Scamp5DigitalTransformation;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.structures.GoalPair;
import uk.co.edstow.cain.transformations.Transformation;
import uk.co.edstow.cain.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Scamp5SuperPixelDirectSolver<G extends BankedKernel3DGoal<G>> extends DirectSolver<G, Scamp5SuperPixelTransformation<G>, BRegister> {
    private final Scamp5SuperPixelConfig scamp5SuperPixelConfig;

    public Scamp5SuperPixelDirectSolver(Scamp5SuperPixelConfig scamp5SuperPixelConfig) {
        this.scamp5SuperPixelConfig = scamp5SuperPixelConfig;
    }

    @Override
    public List<GoalPair<G, Scamp5SuperPixelTransformation<G>, BRegister>> solveDirectly(Context<G, Scamp5SuperPixelTransformation<G>, BRegister> context, GoalBag<G> goals){
        goals = new GoalBag<>(goals);
        List<GoalPair<G, Scamp5SuperPixelTransformation<G>, BRegister>> allPairs = new ArrayList<>();
        List<G> empties = new ArrayList<>();
        for (int i = goals.size() - 1; i >= 0; i--) {
            if(goals.get(i).allZero()){
                empties.add(goals.remove(i));
            }
        }
        if(!scamp5SuperPixelConfig.onlyMov()) {
            if(scamp5SuperPixelConfig.useRes && !empties.isEmpty()) {
                allPairs.add(new GoalPair<>(empties, Collections.emptyList(), new Scamp5SuperPixelTransformation.Res<>(empties, scamp5SuperPixelConfig)));
            }
        }
        List<GoalPair<G,Scamp5SuperPixelTransformation<G>, BRegister>> pairList = super.solveDirectly(context, goals);
        if(pairList==null){
            return null;
        }
        allPairs.addAll(pairList);
        return allPairs;
    }

    @Override
    public Collection<Tuple<List<GoalPair<G, Scamp5SuperPixelTransformation<G>, BRegister>>, G>> solveDirectly(Context<G, Scamp5SuperPixelTransformation<G>, BRegister> context, G goal) {
        ArrayList<Tuple<List<GoalPair<G,Scamp5SuperPixelTransformation<G>, BRegister>>, G>> list = new ArrayList<>();

        if(context.initialGoals.contains(goal)){
            list.add(new Tuple<>(Collections.emptyList(), goal));
            return list;
        }

        //Res
        if(scamp5SuperPixelConfig.useRes && goal.allZero()){
            Scamp5SuperPixelTransformation.Res<G> res = new Scamp5SuperPixelTransformation.Res<>(Collections.singletonList(goal), scamp5SuperPixelConfig);
            list.add(new Tuple<>(Collections.singletonList((new GoalPair<>(goal, Collections.emptyList(), res))), context.initialGoals.get(0)));
            return list;
        }

        //Divide
        if(scamp5SuperPixelConfig.useDiv) {
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
                        List<GoalPair<G,Scamp5SuperPixelTransformation<G>, BRegister>> pairs = new ArrayList<>();
                        Scamp5SuperPixelTransformation.Div<G> div = new Scamp5SuperPixelTransformation.Div<>(goal, true, scamp5SuperPixelConfig);
                        pairs.add(new GoalPair<>(div.applyForwards(), Collections.singletonList(div.a), div));
                        while (div.a.totalI() < ic.totalI()) {
                            div = new Scamp5SuperPixelTransformation.Div<>(div.a, true, scamp5SuperPixelConfig);
                            pairs.add(new GoalPair<>(div.applyForwards(), Collections.singletonList(div.a), div));
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
        if(scamp5SuperPixelConfig.useAddSelf) {
            Scamp5SuperPixelTransformation.AddSelf<G> addSelf = new Scamp5SuperPixelTransformation.AddSelf<>(goal, true, scamp5SuperPixelConfig);
            if(addSelf.isPossible()) {
                list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, addSelf.a, addSelf)), addSelf.a));
            }
        }

        //Move x
        if(scamp5SuperPixelConfig.useMovbx) {
            for (Scamp5SuperPixelTransformation.Dir dir : Scamp5SuperPixelTransformation.Dir.values()) {
                for (int i = 0; i < scamp5SuperPixelConfig.banks; i++) {
                    if(scamp5SuperPixelConfig.isBankSameShape(goal.getBank(), i)) {
                        Scamp5SuperPixelTransformation.Movxb<G> movx = new Scamp5SuperPixelTransformation.Movxb<>(goal, dir.x, dir.y, i, scamp5SuperPixelConfig);
                        list.add(new Tuple<>(Collections.singletonList(new GoalPair<>(goal, movx.lower, movx)), movx.lower));
                    }
                }
            }
        }

        return list;
    }



    @Override
    public Scamp5SuperPixelTransformation<G> getDummyTransformation(List<G> upperGoals, List<G> lowerGoals, Context<G, Scamp5SuperPixelTransformation<G>, BRegister> context) {
        return new Scamp5SuperPixelTransformation.Null<>(lowerGoals.size(), upperGoals.size(), this.scamp5SuperPixelConfig);
    }
}
