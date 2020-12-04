package uk.co.edstow.cain.scamp5.analogue;

import uk.co.edstow.cain.atom.Atom;
import uk.co.edstow.cain.atom.AtomGoal;
import uk.co.edstow.cain.RegisterAllocator;
import uk.co.edstow.cain.Transformation;
import uk.co.edstow.cain.atom.pairGen.SimpleTransformation;
import uk.co.edstow.cain.util.Tuple;

import java.util.*;

public abstract class Scamp5AnalogueTransformation extends Transformation {
    public abstract List<AtomGoal> applyOpForwards() throws TransformationApplicationException;


    abstract static class SimpleScamp5AnalogueTransformation extends Scamp5AnalogueTransformation {

        @Override
        public String code(List<RegisterAllocator.Register> uppers, List<RegisterAllocator.Register> lowers, List<RegisterAllocator.Register> trash) {
            if (uppers.size() == 1) {
                return code(uppers.get(0), lowers);
            } else {
                throw new IllegalArgumentException("This Transformation only accepts one Upper register");
            }
        }

        abstract String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers);

        public abstract AtomGoal applyForwards() throws TransformationApplicationException;
        public List<AtomGoal> applyOpForwards() throws TransformationApplicationException{
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
                default: return null;
            }
        }
    }

    public static class Res extends SimpleScamp5AnalogueTransformation {
        // u := {}
        final AtomGoal result;

        public Res(AtomGoal result) {
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
        public AtomGoal applyForwards() {
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


    public static class Res_2 extends Scamp5AnalogueTransformation {
        // u := {}
        final AtomGoal result1;
        final AtomGoal result2;

        public Res_2(AtomGoal a, AtomGoal b) {
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
        public List<AtomGoal> applyOpForwards() {
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


    public static class Mov extends SimpleScamp5AnalogueTransformation {
        //u := a

        final AtomGoal a;
        AtomGoal moved = null;

        @SuppressWarnings("WeakerAccess")
        public Mov(AtomGoal a) {
            this.a = a;
        }

        @SuppressWarnings("WeakerAccess")
        public Mov(AtomGoal in, boolean upper) {
            if (upper) {
                this.a = new AtomGoal(in);
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
        public AtomGoal applyForwards() {
            if(this.moved == null){
                this.moved = new AtomGoal(a);
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


    public static class Add_2 extends SimpleScamp5AnalogueTransformation {
        // u := a + b

        final AtomGoal a;
        final AtomGoal b;
        AtomGoal sum;


        public Add_2(AtomGoal a, AtomGoal b) {
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
        public AtomGoal applyForwards() {
            if (this.sum == null){
                this.sum = new AtomGoal.Factory(a).addAll(b).get();
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

    public static class Add_3 extends SimpleScamp5AnalogueTransformation {
        // u := a + b + c

        final AtomGoal a;
        final AtomGoal b;
        final AtomGoal c;
        AtomGoal sum;


        public Add_3(AtomGoal a, AtomGoal b, AtomGoal c) {
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
        public AtomGoal applyForwards() {
            if (this.sum == null){
                this.sum = new AtomGoal.Factory(a).addAll(b).addAll(c).get();
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

    public static class Sub extends SimpleScamp5AnalogueTransformation {
        // u := a - b

         final AtomGoal a;
         final AtomGoal b;
         AtomGoal difference;


         public Sub(AtomGoal a, AtomGoal b) {
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
         public AtomGoal applyForwards() {
             if (this.difference == null){
                 this.difference = new AtomGoal.Factory(a).subAll(b).get();
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

    public static class Neg extends SimpleScamp5AnalogueTransformation {
        // u := -a

        final AtomGoal a;
        AtomGoal neg;

        public Neg(AtomGoal a) {
            this.a = a;
            this.neg = null;

        }

        public Neg(AtomGoal in, boolean upper){
            if (upper) {
                this.a = in.negative();
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
        public AtomGoal applyForwards(){
            if(this.neg == null){
                this.neg = a.negative();
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

    public static class Divq extends SimpleScamp5AnalogueTransformation {
        // u := a*0.5 + error

        final AtomGoal a;
        AtomGoal div;

        public Divq(AtomGoal a) {
            this.a = a;
            this.div = null;
        }

        public Divq(AtomGoal in, boolean upper){
            if(!upper){
                this.a = in;
                this.div = null;
            } else {
                this.a = new AtomGoal.Factory(in).addAll(in).get();
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
        public AtomGoal applyForwards() throws TransformationApplicationException {
            if(this.div == null){
                AtomGoal.Factory factory = new AtomGoal.Factory();
                if (!this.a.isEmpty()) {
                    int count = 1;
                    Atom last = a.get(0);
                    for (int i = 1; i < a.size()+1; i++) {
                        Atom c = i < a.size()?a.get(i):null;
                        if(c == null || !last.equals(c)){
                            if(count/2 != (count+1)/2){
                                throw new TransformationApplicationException("Cannot divide uneven number of atoms!");
                            } else {
                                for (int j = 0; j < count / 2; j++) {
                                    factory.add(last);
                                }
                            }
                            last = c;
                            count = 1;
                        } else {
                            count++;
                        }
                    }
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


    public static class Movx extends Mov {
        //u := a_dir

        final Dir dir;

        public Movx(AtomGoal a, Dir dir) {
            super(a);
            this.dir = dir;
        }

        public Movx(AtomGoal in, Dir dir, boolean upper) {
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
        public AtomGoal applyForwards() {
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


    public static class Mov2x extends Mov{
        // u := a_dir1_dir2

        final Dir dir1;
        final Dir dir2;

        public Mov2x(AtomGoal a, Dir dir1, Dir dir2) {
            super(a);
            this.dir1 = dir1;
            this.dir2 = dir2;
        }

        public Mov2x(AtomGoal in, Dir dir1, Dir dir2, boolean upper) {
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
        public AtomGoal applyForwards() {
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


    public static class Addx extends Add_2 {
        // u := a_dir + b_dir

        final Dir dir;

        public Addx(AtomGoal a, AtomGoal b, Dir dir) {
            super(a, b);
            this.dir = dir;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("addx(%s, %s, %s, %s);", upper, lowers.get(0), lowers.get(1), dir.toCode());
        }

        @Override
        public AtomGoal applyForwards() {
            if(this.sum == null){
                AtomGoal.Factory factory = new AtomGoal.Factory();
                this.a.forEach(atom -> factory.add(atom.moved(dir.x, +dir.y, 0)));
                this.b.forEach(atom -> factory.add(atom.moved(dir.x, +dir.y, 0)));
                this.sum = factory.get();
            }
            return this.sum;
        }

        @Override
        public String toStringN() {
            return String.format("Addx %s (%s, %s)", dir, this.a, this.b);
        }


    }


    public static class Add2x extends Add_2 {
        // u := a_dir1_dir2 + b_dir1_dir2
        final Dir dir1;
        final Dir dir2;

        public Add2x(AtomGoal a, AtomGoal b, Dir dir1, Dir dir2) {
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
        public AtomGoal applyForwards() {
            if(this.sum == null){
                AtomGoal.Factory factory = new AtomGoal.Factory();
                this.a.forEach(atom -> factory.add(atom.moved(dir1.x+dir2.x, dir1.y+dir2.y, 0)));
                this.b.forEach(atom -> factory.add(atom.moved(dir1.x+dir2.x, dir1.y+dir2.y, 0)));
                this.sum = factory.get();
            }
            return this.sum;
        }

        @Override
        public String toStringN() {
            return String.format("Add2x %s %s (%s, %s)", dir1, dir2, this.a, this.b);
        }
    }


    public static class Subx extends Sub {
        // u := a_dir - b

        final Dir dir;

        public Subx(AtomGoal a, AtomGoal b, Dir dir) {
            super(a, b);
            this.dir = dir;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("subx(%s, %s, %s, %s);", upper, lowers.get(0), dir.toCode(), lowers.get(1));
        }

        @Override
        public AtomGoal applyForwards() {
            if (this.difference == null){
                AtomGoal.Factory factory = new AtomGoal.Factory();
                this.a.forEach(atom -> factory.add(atom.moved(dir.x, dir.y, 0)));
                factory.subAll(b);
                this.difference = factory.get();
            }
            return this.difference;
        }

        @Override
        public String toStringN() {
            return String.format("SubX %s (%s, %s)", dir, a, b);
        }
    }


    public static class Sub2x extends Sub {
        // u := a_dir1_dir2 - b

        final Dir dir1;
        final Dir dir2;

        public Sub2x(AtomGoal a, AtomGoal b, Dir dir1, Dir dir2) {
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
        public AtomGoal applyForwards() {
            if (this.difference == null){
                AtomGoal.Factory factory = new AtomGoal.Factory();
                this.a.forEach(atom -> factory.add(atom.moved(dir1.x+dir2.x, dir1.y+dir2.y, 0)));
                factory.subAll(b);
                this.difference = factory.get();
            }
            return this.difference;
        }

        @Override
        public String toStringN() {
            return String.format("Sub2X %s %s (%s, %s)", dir1, dir2, a, b);
        }
    }


    public static class Div extends Scamp5AnalogueTransformation {
        // u := a*0.5 + error

        final AtomGoal a;
        final boolean clobber;
        AtomGoal div;


        public Div(AtomGoal a, boolean clobber) {
            this.a = a;
            this.div = null;
            this.clobber = clobber;
        }

        public Div(AtomGoal in, boolean upper, boolean clobber){
            this.clobber = clobber;
            if(!upper){
                this.a = in;
                this.div = null;
            } else {
                this.a = new AtomGoal.Factory(in).addAll(in).get();
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
        public List<AtomGoal> applyOpForwards() throws TransformationApplicationException {
            if(this.div==null) {
                this.div = applyDiv(this.a);
            }
            return Collections.singletonList(this.div);
        }

        private AtomGoal applyDiv(AtomGoal in) throws TransformationApplicationException {
            AtomGoal.Factory factory = new AtomGoal.Factory();
            if (!in.isEmpty()) {
                int count = 1;
                Atom last = in.get(0);
                for (int i = 1; i < in.size()+1; i++) {
                    Atom c = i < in.size()?in.get(i):null;
                    if(c == null || !last.equals(c)){
                        if(count/2 != (count+1)/2){
                            throw new TransformationApplicationException("Cannot divide uneven number of atoms!");
                        } else {
                            for (int j = 0; j < count / 2; j++) {
                                factory.add(last);
                            }
                        }
                        last = c;
                        count = 1;
                    } else {
                        count++;
                    }
                }
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
