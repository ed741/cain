package uk.co.edstow.cpacgen.scamp5;

import uk.co.edstow.cpacgen.Atom;
import uk.co.edstow.cpacgen.Goal;
import uk.co.edstow.cpacgen.RegisterAllocator;
import uk.co.edstow.cpacgen.Transformation;
import uk.co.edstow.cpacgen.util.Tuple;

import java.util.Iterator;
import java.util.List;

public abstract class Scamp5Transformation extends Transformation {

    @Override
    public String toString() {
        return toStringN();
    }

    public enum Dir{
        North(0,0,1, "north"), East(1,1,0, "east"), South(2,0,-1, "south"), West(3,-1,0, "west");
        final int idx;
        final int x;
        final int y;
        final String code;
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

        public String toCode() {
            return code;
        }

        public static Dir fromDirection(Direction direction) {
            switch (direction){
                case N: return North;
                case E: return East;
                case S: return South;
                case W: return West;
            }
            throw new IllegalArgumentException();
        }
    }

    public static class Res extends Scamp5Transformation{
        // u := {}

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("res(%s)", upper);
        }

        @Override
        public int inputCount() {
            return 0;
        }

        @Override
        public Goal applyForwards() {
            return new Goal();
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return "Res/1";
        }

    }

    public static class Add_2 extends Scamp5Transformation {
        // u := a + b

        final Goal a;
        final Goal b;
        Goal sum;


        public Add_2(Goal a, Goal b) {
            this.a = a;
            this.b = b;
            this.sum = null;
        }
        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("add(%s, %s, %s)", upper, lowers.get(0), lowers.get(1));
        }

        @Override
        public int inputCount() {
            return 2;
        }

        @Override
        public Goal applyForwards() {
            if (this.sum == null){
                this.sum = new Goal.Factory(a).addAll(b).get();
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

    }

    public static class Add_3 extends Scamp5Transformation {
        // u := a + b + c

        final Goal a;
        final Goal b;
        final Goal c;
        Goal sum;


        public Add_3(Goal a, Goal b, Goal c) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.sum = null;
        }
        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("add(%s, %s, %s, %s)", upper, lowers.get(0), lowers.get(1), lowers.get(2));
        }

        @Override
        public int inputCount() {
            return 3;
        }

        @Override
        public Goal applyForwards() {
            if (this.sum == null){
                this.sum = new Goal.Factory(a).addAll(b).addAll(c).get();
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
    }

    public static class Sub extends Scamp5Transformation {
        // u := a - b

         final Goal a;
         final Goal b;
         Goal difference;


         public Sub(Goal a, Goal b) {
             this.a = a;
             this.b = b;
             this.difference = null;
         }
         @Override
         public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
             assert lowers.size() == inputCount();
             return String.format("sub(%s, %s, %s)", upper, lowers.get(0), lowers.get(1));
         }

         @Override
         public int inputCount() {
             return 2;
         }

         @Override
         public Goal applyForwards() {
             if (this.difference == null){
                 this.difference = new Goal.Factory(a).subAll(b).get();
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
    }

    public static class Neg extends Scamp5Transformation {
        // u := -a

        final Goal a;
        Goal neg;

        public Neg(Goal a) {
            this.a = a;
            this.neg = null;

        }

        public Neg(Goal in, boolean upper){
            if(!upper){
               this.a = in;
               this.neg = null;
            } else {
                this.a = in.negative();
                this.neg = in;
            }
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("neg(%s, %s)", upper, lowers.get(0));
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public Goal applyForwards(){
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
    }

    public static class Divq extends Scamp5Transformation {
        // u := a*0.5 + error

        final Goal a;
        Goal div;

        public Divq(Goal a) {
            this.a = a;
            this.div = null;
        }

        public Divq(Goal in, boolean upper){
            if(!upper){
                this.a = in;
                this.div = null;
            } else {
                this.a = new Goal.Factory(in).addAll(in).get();
                this.div = in;
            }
        }

        public boolean isPossbile(){
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
            return String.format("divq(%s, %s)", upper, lowers.get(0));
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public Goal applyForwards() throws TransformationApplicationException {
            if(this.div == null){
                Goal.Factory factory = new Goal.Factory();
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
    }


    public static class Movx extends Scamp5Transformation {
        //u := a_dir

        final Goal a;
        Goal moved = null;
        final Dir dir;

        public Movx(Goal a, Dir dir) {
            this.a = a;
            this.dir = dir;
        }

        public Movx(Goal in, Dir dir, boolean upper) {
            if(!upper){
                this.a = in;
                this.dir = dir;
            } else {
                this.a = in.translated(-dir.x, -dir.y, 0);
                this.moved = in;
                this.dir = dir;
            }
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("movx(%s, %s, %s)", upper, lowers.get(0), dir.toCode());
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public Goal applyForwards() {
            if(this.moved == null){
                this.moved = a.translated(dir.x, dir.y, 0);
            }
            return this.moved;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("MovX %s (%s)", dir, this.a);
        }
    }


    public static class Mov2x extends Scamp5Transformation {
        // u := a_dir1_dir2

        final Goal a;
        Goal moved = null;
        final Dir dir1;
        final Dir dir2;

        public Mov2x(Goal a, Dir dir1, Dir dir2) {
            this.a = a;
            this.dir1 = dir1;
            this.dir2 = dir2;
        }

        public Mov2x(Goal in, Dir dir1, Dir dir2, boolean upper) {
            if(!upper){
                this.a = in;
                this.dir1 = dir1;
                this.dir2 = dir2;
            } else {
                this.a = in.translated(-dir1.x-dir2.x, -dir1.y-dir2.y, 0);
                this.moved = in;
                this.dir1 = dir1;
                this.dir2 = dir2;
            }
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("mov2x(%s, %s, %s, %s)", upper, lowers.get(0), dir1.toCode(), dir2.toCode());
        }

        @Override
        public int inputCount() {
            return 1;
        }

        @Override
        public Goal applyForwards() {
            if(this.moved == null){
                this.moved = a.translated(dir1.x +dir2.x, dir1.y +dir2.y, 0);
            }
            return this.moved;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("MovX %s %s (%s)", dir1, dir2, this.a);
        }
    }


    public static class Addx extends Scamp5Transformation {
        // u := a_dir + b_dir

        final Goal a;
        final Goal b;
        Goal sum = null;
        final Dir dir;

        public Addx(Goal a, Goal b, Dir dir) {
            this.a = a;
            this.b = b;
            this.dir = dir;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("addx(%s, %s, %s, %s)", upper, lowers.get(0), lowers.get(1), dir.toCode());
        }

        @Override
        public int inputCount() {
            return 2;
        }

        @Override
        public Goal applyForwards() {
            if(this.sum == null){
                Goal.Factory factory = new Goal.Factory();
                this.a.forEach(atom -> factory.add(atom.moved(dir.x, +dir.y, 0)));
                this.b.forEach(atom -> factory.add(atom.moved(dir.x, +dir.y, 0)));
                this.sum = factory.get();
            }
            return this.sum;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Addx %s (%s, %s)", dir, this.a, this.b);
        }
    }


    public static class Add2x extends Scamp5Transformation {
        // u := a_dir1_dir2 + b_dir1_dir2

        final Goal a;
        final Goal b;
        Goal sum = null;
        final Dir dir1;
        final Dir dir2;

        public Add2x(Goal a, Goal b, Dir dir1, Dir dir2) {
            this.a = a;
            this.b = b;
            this.dir1 = dir1;
            this.dir2 = dir2;
        }

        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("add2x(%s, %s, %s, %s, %s)", upper, lowers.get(0), lowers.get(1), dir1.toCode(), dir2.toCode());
        }

        @Override
        public int inputCount() {
            return 2;
        }

        @Override
        public Goal applyForwards() {
            if(this.sum == null){
                Goal.Factory factory = new Goal.Factory();
                this.a.forEach(atom -> factory.add(atom.moved(dir1.x+dir2.x, dir1.y+dir2.y, 0)));
                this.b.forEach(atom -> factory.add(atom.moved(dir1.x+dir2.x, dir1.y+dir2.y, 0)));
                this.sum = factory.get();
            }
            return this.sum;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Add2x %s %s (%s, %s)", dir1, dir2, this.a, this.b);
        }
    }


    public static class Subx extends Scamp5Transformation {
        // u := a_dir - b

        final Goal a;
        final Goal b;
        final Dir dir;
        Goal difference;


        public Subx(Goal a, Goal b, Dir dir) {
            this.a = a;
            this.b = b;
            this.dir = dir;
            this.difference = null;
        }
        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("subx(%s, %s, %s, %s)", upper, lowers.get(0), dir, lowers.get(1));
        }

        @Override
        public int inputCount() {
            return 2;
        }

        @Override
        public Goal applyForwards() {
            if (this.difference == null){
                Goal.Factory factory = new Goal.Factory();
                this.a.forEach(atom -> factory.add(atom.moved(dir.x, dir.y, 0)));
                factory.subAll(b);
                this.difference = factory.get();
            }
            return this.difference;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("SubX %s (%s, %s)", dir, a, b);
        }
    }


    public static class Sub2x extends Scamp5Transformation {
        // u := a_dir1_dir2 - b

        final Goal a;
        final Goal b;
        final Dir dir1;
        final Dir dir2;
        Goal difference;


        public Sub2x(Goal a, Goal b, Dir dir1, Dir dir2) {
            this.a = a;
            this.b = b;
            this.dir1 = dir1;
            this.dir2 = dir2;
            this.difference = null;
        }
        @Override
        public String code(RegisterAllocator.Register upper, List<RegisterAllocator.Register> lowers) {
            assert lowers.size() == inputCount();
            return String.format("sub2x(%s, %s, %s, %s, %s)", upper, lowers.get(0), dir1, dir2, lowers.get(1));
        }

        @Override
        public int inputCount() {
            return 2;
        }

        @Override
        public Goal applyForwards() {
            if (this.difference == null){
                Goal.Factory factory = new Goal.Factory();
                this.a.forEach(atom -> factory.add(atom.moved(dir1.x+dir2.x, dir1.y+dir2.y, 0)));
                factory.subAll(b);
                this.difference = factory.get();
            }
            return this.difference;
        }

        @Override
        public double cost() {
            return 2;
        }

        @Override
        public String toStringN() {
            return String.format("Sub2X %s %s (%s, %s)", dir1, dir2, a, b);
        }
    }

}
