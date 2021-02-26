package uk.co.edstow.cain.scamp5.analogue;

import uk.co.edstow.cain.StandardTransformation;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.goals.atomGoal.Atom;
import uk.co.edstow.cain.goals.atomGoal.pairGen.SimpleTransformation;
import uk.co.edstow.cain.regAlloc.RegisterAllocator;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;

public abstract class Scamp5AnalogueTransformation<G extends Kernel3DGoal<G>> implements StandardTransformation {
    public abstract List<G> applyOpForwards() throws TransformationApplicationException;


    static class Null<G extends Kernel3DGoal<G>> extends Scamp5AnalogueTransformation<G> {
        private final int inputCount;
        private final int outputCount;

        public Null(int inputCount, int outputCount) {
            this.inputCount = inputCount;
            this.outputCount = outputCount;
        }

        @Override
        public boolean[] inputRegisterOutputInterference(int u){
            return new boolean[inputCount()];
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            int[] out = new int[inputCount()];
            for (int i = 0; i < out.length; i++) {
                out[i]=i;
            }
            return out;
        }

        @Override
        public boolean clobbersInput(int i) {
            return false;
        }


        @Override
        public String code(List<RegisterAllocator.Register> uppers, List<RegisterAllocator.Register> lowers, List<RegisterAllocator.Register> trash) {
            return String.format("//Null Instruction: %s <- %s", uppers, lowers);
        }

        @Override
        public int inputCount() {
            return inputCount;
        }

        @Override
        public int outputCount() {
            return outputCount;
        }

        @Override
        public double cost() {
            return 0;
        }

        @Override
        public String toStringN() {
            return "Null_t";
        }

        @Override
        public String toString() {
            return "Null_t";
        }

        @Override
        public List<G> applyOpForwards() {
            return Collections.emptyList();
        }
    }

    abstract static class SimpleScamp5AnalogueTransformation<G extends Kernel3DGoal<G>> extends Scamp5AnalogueTransformation<G> {

        @Override
        public String code(List<RegisterAllocator.Register> uppers, List<RegisterAllocator.Register> lowers, List<RegisterAllocator.Register> trash) {
            if (uppers.size() == 1) {
                return code(uppers.get(0), lowers);
            } else {
                throw new IllegalArgumentException("This Transformation only accepts one Upper register");
            }
        }

        abstract String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers);

        public abstract G applyForwards() throws TransformationApplicationException;
        public List<G> applyOpForwards() throws TransformationApplicationException{
            return Collections.singletonList(applyForwards());
        }

        @Override
        public boolean[] inputRegisterOutputInterference(int u) {
            return this.inputRegisterOutputInterference();
        }

        abstract boolean[] inputRegisterOutputInterference();

        @Override
        public int ExtraRegisterCount() {
            return inputRegisterOutputInterferes()?1:0;
        }
        abstract boolean inputRegisterOutputInterferes();

        @Override
        public String toString() {
            return toStringN();
        }

        @Override
        public int outputCount() {
            return 1;
        }

        @Override
        public boolean clobbersInput(int i) {
            return false;
        }
    }

    public enum Dir{
        North(0,0,1, "north"), East(1,1,0, "east"), South(2,0,-1, "south"), West(3,-1,0, "west");
        final int idx;
        final int x;
        final int y;
        private final String code;
        Dir(int idx, int x, int y, String code){
            this.idx = idx;
            this.x = x;
            this.y = y;
            this.code = code;
        }

        public Dir opposite() {
            return Dir.values()[(idx+2)%4];
        }

        public Dir cw() {
            return Dir.values()[(idx+1)%4];
        }

        public Dir ccw() {
            return Dir.values()[(idx+3)%4];
        }

        @SuppressWarnings("WeakerAccess")
        public String toCode() {
            return code;
        }

        public static Dir fromDirection(SimpleTransformation.Direction direction) {
            switch (direction){
                case N: return North;
                case E: return East;
                case S: return South;
                case W: return West;
            }
            assert false;
            System.exit(-1);
            return null;
        }
    }

    public static class Res<G extends Kernel3DGoal<G>> extends SimpleScamp5AnalogueTransformation<G> {
        // u := {}
        final G result;

        public Res(G result) {
            this.result = result;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("res(%s);", upper);
        }

        @Override
        public int inputCount() {
            return 0;
        }

        @Override
        public G applyForwards() {
            return result;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return "Res/1";
        }

        private static final boolean[] inputRegisterOutputInterference = new boolean[0];
        private static final int[] inputRegisterIntraInterference = new int[0];

        @Override
        public boolean[] inputRegisterOutputInterference() {
            return inputRegisterOutputInterference;
        }

        @Override
        public boolean inputRegisterOutputInterferes() {
            return false;
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }
    }


    public static class Res_2<G extends Kernel3DGoal<G>> extends Scamp5AnalogueTransformation<G> {
        // u := {}
        final G result1;
        final G result2;

        public Res_2(G a, G b) {
            this.result1 = a;
            this.result2 = b;
        }

        @Override
        public String code(List<RegisterAllocator.Register> upper, List<RegisterAllocator.Register> lowers, List<RegisterAllocator.Register> trash) {
            assert lowers.size() == inputCount();
            return String.format("res(%s, %s);", upper.get(0), upper.get(1));
        }

        @Override
        public int inputCount() {
            return 0;
        }

        @Override
        public int outputCount() {
            return 2;
        }

        @Override
        public List<G> applyOpForwards() {
            return Arrays.asList(result1, result2);
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return "Res/2";
        }
        @Override
        public String toString() {
            return toStringN();
        }

        private static final boolean[] inputRegisterOutputInterference = new boolean[0];
        private static final int[] inputRegisterIntraInterference = new int[0];

        @Override
        public boolean[] inputRegisterOutputInterference(int u) {
            return inputRegisterOutputInterference;
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }

        @Override
        public boolean clobbersInput(int i) {
            return false;
        }
    }


    public static class Mov<G extends Kernel3DGoal<G>> extends SimpleScamp5AnalogueTransformation<G> {
        //u := a

        final G a;
        G moved = null;

        @SuppressWarnings("WeakerAccess")
        public Mov(G a) {
            this.a = a;
        }

        @SuppressWarnings("WeakerAccess")
        public Mov(G in, boolean upper) {
            if (upper) {
                this.a = in.copy();
                this.moved = in;
            } else {
                this.a = in;
            }
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("mov(%s, %s);", upper, lowers.get(0));
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public G applyForwards() {
            if(this.moved == null){
                this.moved = a.copy();
            }
            return this.moved;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Mov (%s)", this.a);
        }

        private static final boolean[] inputRegisterOutputInterference = new boolean[1];
        private static final int[] inputRegisterIntraInterference = new int[1];

        @Override
        public boolean[] inputRegisterOutputInterference() {
            return inputRegisterOutputInterference;
        }

        @Override
        public boolean inputRegisterOutputInterferes() {
            return false;
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }
    }


    public static class Add_2<G extends Kernel3DGoal<G>> extends SimpleScamp5AnalogueTransformation<G> {
        // u := a + b

        final G a;
        final G b;
        G sum;


        public Add_2(G a, G b) {
            this.a = a;
            this.b = b;
            this.sum = null;
        }
        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("add(%s, %s, %s);", upper, lowers.get(0), lowers.get(1));
        }

        @Override
        public int inputCount() {
            return 2;
        }

        @Override
        public G applyForwards() {
            if (this.sum == null){
                this.sum = a.added(b);
            }
            return this.sum;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Add2(%s, %s)", a, b);
        }

        private static final boolean[] inputRegisterOutputInterference = {false, false};
        private static final int[] inputRegisterIntraInterference = {0,1};

        @Override
        public boolean[] inputRegisterOutputInterference() {
            return inputRegisterOutputInterference;
        }

        @Override
        public boolean inputRegisterOutputInterferes() {
            return false;
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }

    }

    public static class Add_3<G extends Kernel3DGoal<G>> extends SimpleScamp5AnalogueTransformation<G> {
        // u := a + b + c

        final G a;
        final G b;
        final G c;
        G sum;


        public Add_3(G a, G b, G c) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.sum = null;
        }
        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("add(%s, %s, %s, %s);", upper, lowers.get(0), lowers.get(1), lowers.get(2));
        }

        @Override
        public int inputCount() {
            return 3;
        }

        @Override
        public G applyForwards() {
            if (this.sum == null){
                this.sum = a.newFactory().add(a).add(b).add(c).get();
            }
            return this.sum;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Add3(%s, %s, %s)", a, b, c);
        }

        private static final boolean[] inputRegisterOutputInterference = new boolean[3];
        private static final int[] inputRegisterIntraInterference = {0,1,2};

        @Override
        public boolean[] inputRegisterOutputInterference() {
            return inputRegisterOutputInterference;
        }

        @Override
        public boolean inputRegisterOutputInterferes() {
            return false;
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }
    }

    public static class Sub<G extends Kernel3DGoal<G>> extends SimpleScamp5AnalogueTransformation<G> {
        // u := a - b

         final G a;
         final G b;
         G difference;


         public Sub(G a, G b) {
             this.a = a;
             this.b = b;
             this.difference = null;
         }
         @Override
         public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
             assert lowers.size() == inputCount();
             return String.format("sub(%s, %s, %s);", upper, lowers.get(0), lowers.get(1));
         }

         @Override
         public int inputCount() {
             return 2;
         }

         @Override
         public G applyForwards() {
             if (this.difference == null){
                 this.difference = a.subtracted(b);
             }
             return this.difference;
         }

         @Override
         public double cost() {
             return 2;
         }

         @Override
         public String toStringN() {
             return String.format("Sub(%s, %s)", a, b);
         }

        private static final boolean[] inputRegisterOutputInterference = {false, true};
        private static final int[] inputRegisterIntraInterference = {0,0};

        @Override
        public boolean[] inputRegisterOutputInterference() {
            return inputRegisterOutputInterference;
        }

        @Override
        public boolean inputRegisterOutputInterferes() {
            return true;
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }
    }

    public static class Neg<G extends Kernel3DGoal<G>> extends SimpleScamp5AnalogueTransformation<G> {
        // u := -a

        final G a;
        G neg;

        public Neg(G a) {
            this.a = a;
            this.neg = null;

        }

        public Neg(G in, boolean upper){
            if (upper) {
                this.a = in.negated();
                this.neg = in;
            } else {
               this.a = in;
               this.neg = null;
            }
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("neg(%s, %s);", upper, lowers.get(0));
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public G applyForwards(){
            if(this.neg == null){
                this.neg = a.negated();
            }
            return this.neg;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Neg(%s)", a);
        }

        private static final boolean[] inputRegisterOutputInterference = {true};
        private static final int[] inputRegisterIntraInterference = {0};

        @Override
        public boolean[] inputRegisterOutputInterference() {
            return inputRegisterOutputInterference;
        }

        @Override
        public boolean inputRegisterOutputInterferes() {
            return true;
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }
    }

    public static class Divq<G extends Kernel3DGoal<G>> extends SimpleScamp5AnalogueTransformation<G> {
        // u := a*0.5 + error

        final G a;
        G div;

        public Divq(G a) {
            this.a = a;
            this.div = null;
        }

        public Divq(G in, boolean upper){
            if(!upper){
                this.a = in;
                this.div = null;
            } else {
                this.a = in.added(in);
                this.div = in;
            }
        }

        public boolean isPossible(){
            Iterator<Tuple<Atom, Integer>> it = a.uniqueCountIterator();
            while(it.hasNext()){
                Tuple<Atom, Integer> t = it.next();
                int count = t.getB();
                if(count < 2 || count % 2 != 0){
                    return false;
                }
            }
            return true;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("divq(%s, %s);", upper, lowers.get(0));
        }

        @Override
        public int inputCount() {
            return 1;
        }


        @SuppressWarnings("ConstantConditions")
        @Override
        public G applyForwards() throws TransformationApplicationException {
            if(this.div == null){
                Kernel3DGoal.Kernel3DGoalFactory<G> factory = a.newFactory();
                Iterator<Tuple<Atom, Integer>> it = a.uniqueCountIterator();
                while(it.hasNext()){
                    Tuple<Atom, Integer> t = it.next();
                    int count = t.getB();
                    if(count < 2 || count % 2 != 0){
                        throw new TransformationApplicationException("Cannot divide uneven number of atoms!");
                    }
                    factory.add(t.getA().x, t.getA().y, t.getA().z, t.getA().positive?count/2:(-count/2));
                }
                this.div = factory.get();
            }
            return this.div;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Div(%s)", this.a);
        }

        private static final boolean[] inputRegisterOutputInterference = new boolean[]{true};
        private static final int[] inputRegisterIntraInterference = {0};

        @Override
        public boolean[] inputRegisterOutputInterference() {
            return inputRegisterOutputInterference;
        }

        @Override
        public boolean inputRegisterOutputInterferes() {
            return true;
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return inputRegisterIntraInterference;
        }
    }


    public static class Movx<G extends Kernel3DGoal<G>> extends Mov<G> {
        //u := a_dir

        final Dir dir;

        public Movx(G a, Dir dir) {
            super(a);
            this.dir = dir;
        }

        public Movx(G in, Dir dir, boolean upper) {
            super(upper?in.translated(-dir.x, -dir.y, 0):in);
            this.moved = upper?in:null;
            this.dir = dir;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("movx(%s, %s, %s);", upper, lowers.get(0), dir.toCode());
        }

        @Override
        public G applyForwards() {
            if(this.moved == null){
                this.moved = a.translated(dir.x, dir.y, 0);
            }
            return this.moved;
        }

        @Override
        public String toStringN() {
            return String.format("MovX %s (%s)", dir, this.a);
        }
    }


    public static class Mov2x<G extends Kernel3DGoal<G>> extends Mov<G>{
        // u := a_dir1_dir2

        final Dir dir1;
        final Dir dir2;

        public Mov2x(G a, Dir dir1, Dir dir2) {
            super(a);
            this.dir1 = dir1;
            this.dir2 = dir2;
        }

        public Mov2x(G in, Dir dir1, Dir dir2, boolean upper) {
            super(upper?in.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0):in);
            this.moved = upper?in:null;
            this.dir1 = dir1;
            this.dir2 = dir2;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("mov2x(%s, %s, %s, %s);", upper, lowers.get(0), dir1.toCode(), dir2.toCode());
        }

        @Override
        public G applyForwards() {
            if(this.moved == null){
                this.moved = a.translated(dir1.x +dir2.x, dir1.y +dir2.y, 0);
            }
            return this.moved;
        }

        @Override
        public String toStringN() {
            return String.format("Mov2X %s %s (%s)", dir1, dir2, this.a);
        }

    }


    public static class Addx<G extends Kernel3DGoal<G>> extends Add_2<G> {
        // u := a_dir + b_dir

        final Dir dir;

        public Addx(G a, G b, Dir dir) {
            super(a, b);
            this.dir = dir;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("addx(%s, %s, %s, %s);", upper, lowers.get(0), lowers.get(1), dir.toCode());
        }

        @Override
        public G applyForwards() {
            if(this.sum == null){
                this.sum = this.a.added(this.b).translated(dir.x, dir.y, 0);
            }
            return this.sum;
        }

        @Override
        public String toStringN() {
            return String.format("Addx %s (%s, %s)", dir, this.a, this.b);
        }


    }


    public static class Add2x<G extends Kernel3DGoal<G>> extends Add_2<G> {
        // u := a_dir1_dir2 + b_dir1_dir2
        final Dir dir1;
        final Dir dir2;

        public Add2x(G a, G b, Dir dir1, Dir dir2) {
            super(a, b);
            this.dir1 = dir1;
            this.dir2 = dir2;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("add2x(%s, %s, %s, %s, %s);", upper, lowers.get(0), lowers.get(1), dir1.toCode(), dir2.toCode());
        }

        @Override
        public G applyForwards() {
            if(this.sum == null){
                this.sum = this.a.added(b).translated(dir1.x+dir2.x, dir1.y+dir2.y, 0);
            }
            return this.sum;
        }

        @Override
        public String toStringN() {
            return String.format("Add2x %s %s (%s, %s)", dir1, dir2, this.a, this.b);
        }
    }


    public static class Subx<G extends Kernel3DGoal<G>> extends Sub<G> {
        // u := a_dir - b

        final Dir dir;

        public Subx(G a, G b, Dir dir) {
            super(a, b);
            this.dir = dir;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("subx(%s, %s, %s, %s);", upper, lowers.get(0), dir.toCode(), lowers.get(1));
        }

        @Override
        public G applyForwards() {
            if (this.difference == null){
                this.difference = a.translated(dir.x, dir.y, 0).subtracted(b);
            }
            return this.difference;
        }

        @Override
        public String toStringN() {
            return String.format("SubX %s (%s, %s)", dir, a, b);
        }
    }


    public static class Sub2x<G extends Kernel3DGoal<G>> extends Sub<G> {
        // u := a_dir1_dir2 - b

        final Dir dir1;
        final Dir dir2;

        public Sub2x(G a, G b, Dir dir1, Dir dir2) {
            super(a, b);
            this.dir1 = dir1;
            this.dir2 = dir2;
            this.difference = null;
        }
        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("sub2x(%s, %s, %s, %s, %s);", upper, lowers.get(0), dir1.toCode(), dir2.toCode(), lowers.get(1));
        }

        @Override
        public G applyForwards() {
            if (this.difference == null){
                this.difference = a.translated(dir1.x+dir2.x, dir1.y+dir2.y, 0).subtracted(b);
            }
            return this.difference;
        }

        @Override
        public String toStringN() {
            return String.format("Sub2X %s %s (%s, %s)", dir1, dir2, a, b);
        }
    }


    public static class Div<G extends Kernel3DGoal<G>> extends Scamp5AnalogueTransformation<G> {
        // u := a*0.5 + error

        final G a;
        final boolean clobber;
        G div;


        public Div(G a, boolean clobber) {
            this.a = a;
            this.div = null;
            this.clobber = clobber;
        }

        public Div(G in, boolean upper, boolean clobber){
            this.clobber = clobber;
            if(!upper){
                this.a = in;
                this.div = null;
            } else {
                this.a = in.added(in);
                this.div = in;
            }
        }

        public boolean isPossible(){
            Iterator<Tuple<Atom, Integer>> it = a.uniqueCountIterator();
            while(it.hasNext()){
                Tuple<Atom, Integer> t = it.next();
                int count = t.getB();
                if(count < 2 || count % 2 != 0){
                    return false;
                }
            }
            return true;
        }

        @Override
        public String code(List<RegisterAllocator.Register> uppers, List<RegisterAllocator.Register> lowers, List<RegisterAllocator.Register> trash) {
            assert lowers.size() == inputCount();
            assert uppers.size() == outputCount();
            if(uppers.get(0).equals(lowers.get(0))){
                assert trash.size()>=2;
                return String.format("diva(%s, %s, %s);", uppers.get(0), trash.get(0), trash.get(1));
            }
            if(this.clobber) {
                assert trash.size() >= 1;
                return String.format("div(%s, %s, %s);", uppers.get(0), trash.get(0), lowers.get(0));
            }else{
                assert trash.size() >= 2;
                return String.format("div(%s, %s, %s, %s);", uppers.get(0), trash.get(0), trash.get(1), lowers.get(0));
            }

        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public int outputCount() {
            return 1;
        }

        @Override
        public boolean clobbersInput(int i) {
            return this.clobber;
        }

        @Override
        public int ExtraRegisterCount() {
            return this.clobber?2:3;
        }

        @Override
        public boolean[] inputRegisterOutputInterference(int u) {
            if(this.clobber) {
                return new boolean[]{false};
            } else {
                return new boolean[]{true};
            }
        }

        @Override
        public int[] inputRegisterIntraInterference() {
            return new int[]{0};
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        public List<G> applyOpForwards() throws TransformationApplicationException {
            if(this.div==null) {
                this.div = applyDiv(this.a);
            }
            return Collections.singletonList(this.div);
        }

        private G applyDiv(G in) throws TransformationApplicationException {
            Kernel3DGoal.Kernel3DGoalFactory<G> factory = a.newFactory();
            Iterator<Tuple<Atom, Integer>> it = a.uniqueCountIterator();
            while(it.hasNext()){
                Tuple<Atom, Integer> t = it.next();
                int count = t.getB();
                if(count < 2 || count % 2 != 0){
                    throw new TransformationApplicationException("Cannot divide uneven number of atoms!");
                }
                factory.add(t.getA().x, t.getA().y, t.getA().z, t.getA().positive?count/2:(-count/2));
            }
            return factory.get();
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Divide(%b)(%s)", this.clobber, this.a);
        }

        @Override
        public String toString() {
            return toStringN();
        }


    }

}
