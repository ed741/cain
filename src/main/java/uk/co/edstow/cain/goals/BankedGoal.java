package uk.co.edstow.cain.goals;

public interface BankedGoal<G extends BankedGoal<G>> {

    int getBank();
    G inBank(int bank);
    boolean isInBank(int bank);

    BankedGoal.BankedGoalFactory<G> newFactory();

    interface BankedGoalFactory<G>{
        G get();
        BankedGoalFactory<G> setBank(int bank);
    }

}
