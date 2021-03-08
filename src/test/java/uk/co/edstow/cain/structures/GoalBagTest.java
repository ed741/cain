package uk.co.edstow.cain.structures;

import org.junit.jupiter.api.Test;
import uk.co.edstow.cain.goals.atomGoal.Atom;
import uk.co.edstow.cain.goals.atomGoal.AtomGoal;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class GoalBagTest {

    static class TGoal implements Goal<TGoal>{
        final int i;
        final int j;

        TGoal(int i, int j) {
            this.i = i;
            this.j = j;
        }
        public int getI() {
            return i;
        }

        public int getJ() {
            return j;
        }

        @Override
        public boolean same(TGoal goal) {
            if (this.equivalent(goal)) return true;
            return this.i == goal.i && this.j == goal.j;
        }

        @Override
        public boolean equivalent(TGoal goal) {
            return this == goal;
        }

        @Override
        public double total() {
            return i+j;
        }

        @Override
        public String[][] getCharTable(Bounds b, boolean top, boolean bottom, boolean centreDot, boolean colourNeg) {
            String iStr = Integer.toString(i);
            String jStr = Integer.toString(j);
            String[][] strArr = new String[1][2];
            strArr[0][0] = iStr;
            strArr[0][1] = jStr;
            return strArr;
        }

        @Override
        public Bounds bounds() {
            return new Bounds.SimpleBounds(i,j, 0, 0, 0, 0);
        }

        @Override
        public int compareTo(TGoal tGoal) {
            int c = Double.compare(tGoal.total(), this.total());
            if (c != 0) return c;
            return Integer.compare(tGoal.i, this.i);
        }
    }

    @Test
    void testIsImmutable() {
        GoalBag<TGoal> bag = new GoalBag<>();
        bag.add(new TGoal(0,0));
        assertFalse(bag.isImmutable());
        bag.setImmutable();
        assertTrue(bag.isImmutable());
    }

    @Test
    void testSumTotal() {
        GoalBag<TGoal> bag = new GoalBag<>();
        bag.add(new TGoal(1,2));
        bag.add(new TGoal(3,4));
        assertEquals(10d, bag.sumTotal());
        bag = new GoalBag<>();
        bag.add(new TGoal(11,2));
        bag.add(new TGoal(3,1));
        bag.add(new TGoal(320,10));
        assertEquals(347d, bag.sumTotal());
    }

    @Test
    void testAdd() {
        GoalBag<TGoal> bag = new GoalBag<>();
        TGoal a = new TGoal(0,2);
        TGoal b = new TGoal(3,0);
        bag.add(a);
        bag.add(b);
        assertTrue(bag.get(0).equivalent(b));
        assertTrue(bag.get(1).equivalent(a));
    }

    @Test
    void testAddAll() {
        GoalBag<TGoal> bag = new GoalBag<>();
        TGoal a = new TGoal(0,2);
        TGoal b = new TGoal(3,0);
        bag.addAll(Arrays.asList(b, a));
        assertTrue(bag.get(0).equivalent(b));
        assertTrue(bag.get(1).equivalent(a));
    }

    @Test
    void testRemove() {
        GoalBag<TGoal> bag = new GoalBag<>();
        TGoal a = new TGoal(0,2);
        TGoal b = new TGoal(3,0);
        TGoal c = new TGoal(0,3);
        TGoal d = new TGoal(1,2);
        bag.addAll(Arrays.asList(b, a));
        assertTrue(bag.remove(a));
        assertFalse(bag.remove(a));
        assertNotEquals(a, bag.get(0));
        assertNotSame(a, bag.get(0));
        bag.add(a);
        bag.add(c);
        bag.add(d);
        assertTrue(bag.remove(b));
        assertNotEquals(b, bag.get(1));
    }

    @Test
    void testTestRemove() {
        GoalBag<TGoal> bag = new GoalBag<>();
        TGoal a = new TGoal(0,2);
        TGoal b = new TGoal(3,0);
        TGoal c = new TGoal(0,3);
        TGoal d = new TGoal(1,2);
        bag.addAll(Arrays.asList(b, a, d, c));
        assertTrue(Arrays.asList(b, d, c).contains(bag.remove(0)));
        assertTrue(Arrays.asList(b, d, c).contains(bag.remove(0)));
        assertTrue(Arrays.asList(b, d, c).contains(bag.remove(0)));
        assertSame(a, bag.remove(0));
    }

    @Test
    void testTestEquals() {
        {
            GoalBag<TGoal> bag1 = new GoalBag<>();
            GoalBag<TGoal> bag2 = new GoalBag<>();
            TGoal a = new TGoal(0, 2);
            TGoal b = new TGoal(3, 0);
            TGoal c = new TGoal(0, 3);
            TGoal d = new TGoal(1, 2);
            bag1.addAll(Arrays.asList(a, b, c, d));
            bag2.addAll(Arrays.asList(a, d, c, b));
            assertThrows(AssertionError.class, () -> bag1.equals(bag2));
            GoalBag<TGoal> bag1f = new GoalBag<>(bag1, true);
            GoalBag<TGoal> bag2f = new GoalBag<>(bag2, true);
            assertEquals(bag2f, bag1f);
        }
        {
            GoalBag<AtomGoal> bag1 = new GoalBag<>();
            GoalBag<AtomGoal> bag2 = new GoalBag<>();
            AtomGoal a = new AtomGoal(new Atom(0,0,0, true));
            AtomGoal b = new AtomGoal(new Atom(1,0,0, true));
            AtomGoal c = new AtomGoal(new Atom(2,0,0, true));
            AtomGoal d = new AtomGoal(new Atom(3,0,0, true));
            bag1.addAll(Arrays.asList(a, b, c, d));
            bag2.addAll(Arrays.asList(a, d, c, b));
            assertThrows(AssertionError.class, () -> bag1.equals(bag2));
            GoalBag<AtomGoal> bag1f = new GoalBag<>(bag1, true);
            GoalBag<AtomGoal> bag2f = new GoalBag<>(bag2, true);
            assertEquals(bag2f, bag1f);
        }
    }

    @Test
    void testSize() {
        GoalBag<TGoal> bag1 = new GoalBag<>();
        GoalBag<TGoal> bag2 = new GoalBag<>();
        assertEquals(0, bag2.size());
        TGoal a = new TGoal(0, 2);
        TGoal b = new TGoal(3, 0);
        TGoal c = new TGoal(0, 3);
        TGoal d = new TGoal(1, 2);
        bag1.addAll(Arrays.asList(a, b, c, d));
        bag2.addAll(Arrays.asList(a, d, c));
        assertEquals(4, bag1.size());
        assertEquals(3, bag2.size());
    }

    @Test
    void testGet() {
        GoalBag<TGoal> bag1 = new GoalBag<>();
        GoalBag<TGoal> bag2 = new GoalBag<>();
        TGoal a = new TGoal(0, 2);
        TGoal b = new TGoal(3, 0);
        TGoal c = new TGoal(0, 3);
        TGoal d = new TGoal(1, 2);
        bag1.addAll(Arrays.asList(a, b, c, d));
        bag2.addAll(Arrays.asList(d, b, c, a));
        assertSame(a, bag1.get(3));
        assertSame(a, bag2.get(3));
    }

    @Test
    void testIsEmpty() {
        GoalBag<TGoal> bag1 = new GoalBag<>();
        GoalBag<TGoal> bag2 = new GoalBag<>();
        assertTrue(bag1.isEmpty());
        assertTrue(bag2.isEmpty());
        TGoal a = new TGoal(0, 2);
        TGoal b = new TGoal(3, 0);
        TGoal c = new TGoal(0, 3);
        TGoal d = new TGoal(1, 2);
        bag1.addAll(Arrays.asList(a, b, c, d));
        bag2.addAll(Arrays.asList(d, b, c, a));
        assertFalse(bag1.isEmpty());
        assertFalse(bag2.isEmpty());
        bag1.remove(0);
        bag1.remove(0);
        bag1.remove(0);
        bag1.remove(0);
        assertTrue(bag1.isEmpty());
        bag2.remove(3);
        bag2.remove(2);
        bag2.remove(1);
        bag2.remove(0);
        assertTrue(bag2.isEmpty());
    }

    @Test
    void testContainsAll() {
        GoalBag<TGoal> bag1 = new GoalBag<>();
        GoalBag<TGoal> bag2 = new GoalBag<>();

        TGoal a = new TGoal(0, 2);
        TGoal b = new TGoal(3, 0);
        TGoal c = new TGoal(0, 3);
        TGoal d = new TGoal(1, 2);
        assertFalse(bag1.containsAll(Arrays.asList(a, b, c, d)));
        bag1.addAll(Arrays.asList(a, b, c, d));
        bag2.addAll(Arrays.asList(d, c, b, a));
        assertTrue(bag1.containsAll(Arrays.asList(a, b, c, d)));
        assertTrue(bag2.containsAll(Arrays.asList(a, b, c, d)));
        TGoal e = new TGoal(1, 1);
        assertFalse(bag1.containsAll(Arrays.asList(a, b, c, d, e)));
        assertFalse(bag2.containsAll(Arrays.asList(a, b, e, c, d)));
    }

    @Test
    void testBounds() {
        GoalBag<TGoal> bag1 = new GoalBag<>();
        GoalBag<TGoal> bag2 = new GoalBag<>();

        TGoal a = new TGoal(0, 2);
        TGoal b = new TGoal(3, 0);
        TGoal c = new TGoal(0, 3);
        TGoal d = new TGoal(1, 2);
        bag1.addAll(Arrays.asList(a, b, c, d));
        bag2.addAll(Arrays.asList(d, c, b, a));
        assertEquals(bag1.bounds(), bag2.bounds());
        assertEquals(3, bag1.bounds().getXMax());
    }
}