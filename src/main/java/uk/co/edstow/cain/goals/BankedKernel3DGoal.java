package uk.co.edstow.cain.goals;

public interface BankedKernel3DGoal<G extends BankedKernel3DGoal<G>> extends Kernel3DGoal<G>, BankedGoal<G>{

    @Override
    BankedKernel3DGoalFactory<G> newFactory();

    interface BankedKernel3DGoalFactory<G> extends BankedGoalFactory<G>, Kernel3DGoalFactory<G>{
    }
}
