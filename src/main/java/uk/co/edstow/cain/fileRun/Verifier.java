package uk.co.edstow.cain.fileRun;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.regAlloc.RegisterAllocator;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.structures.Plan;
import uk.co.edstow.cain.transformations.Transformation;

import java.util.List;

public interface Verifier<G extends Goal<G>, T extends Transformation<R>, R extends Register> {

    class VerificationError extends RuntimeException{
        public VerificationError(String s) {
            super(s);
        }
    }

    static <G extends Goal<G>, T extends Transformation<R>, R extends Register> Verifier<G,T,R> SkipVerify(){
        return (code, initialGoals, finalGoals, plan, registerAllocator) -> new VerificationResult() {
            @Override
            public boolean passed() { return true; }
            @Override
            public String getInfo() { return "Verification Skipped"; }
        };
    }

    default void verbose(int v){}
    VerificationResult verify(String code, List<G> initialGoals, List<G> finalGoals, Plan<G,T,R> plan, RegisterAllocator<G,T,R> registerAllocator);

    interface VerificationResult {
        boolean passed();
        String getInfo();
    }

    VerificationResult GenericFail = new VerificationResult(){
        @Override
        public boolean passed() {
            return false;
        }

        @Override
        public String getInfo() {
            return "Failed, Check logs for more information";
        }
    };
}
