package uk.co.edstow.cain.goals.arrayGoal;

import org.junit.jupiter.api.Test;
import uk.co.edstow.cain.goals.atomGoal.Atom;
import uk.co.edstow.cain.goals.atomGoal.AtomGoal;
import uk.co.edstow.cain.structures.Bounds;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArrayGoalTest {

    @Test
    void testGet() {
        AtomGoal ag = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,false)
        ).get();
        ArrayGoal g = new ArrayGoal(ag);
        assertEquals(2, g.get(0,3,0));
        assertEquals(1, g.get(1,2,1));
        assertEquals(-1, g.get(-2,3,0));
//        System.out.println(ag.getTableString(false, false, true, true));
//        System.out.println(g.getTableString(true, true, true, true));

    }

    @Test
    void testEquals() {
        AtomGoal ag1 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,false)
        ).get();
        AtomGoal ag2 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,true)
        ).get();
        ArrayGoal g1 = new ArrayGoal(ag1);
        ArrayGoal g2 = new ArrayGoal(ag1);
        ArrayGoal g3 = new ArrayGoal(ag2);
        assertNotSame(g1, g2);
        assertEquals(g1, g2);
        assertEquals(g1, g1);
        assertNotEquals(g1, g3);
        assertNotEquals(g2, g3);
        assertEquals(g3, g3);
    }

    @Test
    void testHashCode() {
        AtomGoal ag1 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,false)
        ).get();
        AtomGoal ag2 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,true)
        ).get();
        ArrayGoal g1 = new ArrayGoal(ag1);
        ArrayGoal g2 = new ArrayGoal(ag1);
        ArrayGoal g3 = new ArrayGoal(ag2);
        assertNotSame(g1, g2);
        assertEquals(g1.hashCode(), g2.hashCode());
        assertEquals(g1.hashCode(), g1.hashCode());
        assertNotEquals(g1.hashCode(), g3.hashCode());// Could be a hash collision but we hope not
        assertNotEquals(g2.hashCode(), g3.hashCode());
        assertEquals(g3.hashCode(), g3.hashCode());
    }

    @Test
    void testSame() {
        AtomGoal ag1 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,false)
        ).get();
        AtomGoal ag2 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,true)
        ).get();
        ArrayGoal g1 = new ArrayGoal(ag1);
        ArrayGoal g2 = new ArrayGoal(ag1);
        ArrayGoal g3 = new ArrayGoal(ag2);
        assertTrue(g1.same(g2));
        assertTrue(g2.same(g1));
        assertFalse(g1.same(g3));
        assertFalse(g3.same(g2));
    }

    @Test
    void testEquivalent() {
        AtomGoal ag1 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,false)
        ).get();
        AtomGoal ag2 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,true)
        ).get();
        ArrayGoal g1 = new ArrayGoal(ag1);
        ArrayGoal g2 = new ArrayGoal(ag1);
        ArrayGoal g3 = new ArrayGoal(ag2);
        assertFalse(g1.equivalent(g2));
        assertTrue(g1.equivalent(g1));
        assertTrue(g3.equivalent(g3));
        assertFalse(g1.equivalent(g3));
        assertFalse(g3.equivalent(g2));
    }

    @Test
    void testTotal() {
        AtomGoal ag1 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,false)
        ).get();
        AtomGoal ag2 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,true)
        ).get();
        AtomGoal ag3 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(0,3,9,false),
                new Atom(0,3,10,true),
                new Atom(45,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,true)
        ).get();
        ArrayGoal g1 = new ArrayGoal(ag1);
        ArrayGoal g2 = new ArrayGoal(ag1);
        ArrayGoal g3 = new ArrayGoal(ag2);
        ArrayGoal g4 = new ArrayGoal(ag3);
        assertEquals(4d, g1.total());
        assertEquals(4d, g2.total());
        assertEquals(4d, g3.total());
        assertEquals(7d, g4.total());
    }

    @Test
    void testGetCharTable() {
    }

    @Test
    void testBounds() {
        AtomGoal ag1 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,false)
        ).get();
        AtomGoal ag2 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,true)
        ).get();
        AtomGoal ag3 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(0,3,9,false),
                new Atom(0,3,10,true),
                new Atom(45,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,true)
        ).get();
        ArrayGoal g1 = new ArrayGoal(ag1);
        ArrayGoal g2 = new ArrayGoal(ag2);
        ArrayGoal g3 = new ArrayGoal(ag3);
        assertEquals(new Bounds.SimpleBounds(1,-2,3,2,1,0), g1.bounds());
        assertEquals(new Bounds.SimpleBounds(1,-2,3,2,1,0), g2.bounds());
        assertEquals(new Bounds.SimpleBounds(45,-2,3,2,10,0), g3.bounds());
    }

    @Test
    void testCompareTo() {
        AtomGoal ag1 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,false)
        ).get();
        AtomGoal ag2 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,true)
        ).get();
        AtomGoal ag3 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(0,3,9,false),
                new Atom(0,3,10,true),
                new Atom(45,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,true)
        ).get();
        ArrayGoal g1 = new ArrayGoal(ag1);
        ArrayGoal g2 = new ArrayGoal(ag1);
        ArrayGoal g3 = new ArrayGoal(ag2);
        ArrayGoal g4 = new ArrayGoal(ag3);
        assertEquals(0, g1.compareTo(g2));
        assertEquals(0, g2.compareTo(g1));
        assertEquals(-1, g4.compareTo(g1));
        assertEquals(1, g1.compareTo(g4));
        assertNotEquals(0, g1.compareTo(g3));
        assertNotEquals(0, g3.compareTo(g1));
        assertEquals(-g1.compareTo(g3), g3.compareTo(g1));
    }


    @Test
    void testWithout() {
        AtomGoal ag1 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,false)
        ).get();
        AtomGoal ag2 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,true)
        ).get();
        AtomGoal ag3 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(0,3,9,false),
                new Atom(0,3,10,true),
                new Atom(45,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,true)
        ).get();
        ArrayGoal g1 = new ArrayGoal(ag1);
        ArrayGoal g2 = new ArrayGoal(ag1);
        ArrayGoal g3 = new ArrayGoal(ag2);
        ArrayGoal g4 = new ArrayGoal(ag3);
        assertEquals(new ArrayGoal(new AtomGoal.Factory(new Atom(-2,3,0,false)).get()), g1.without(g3));
        assertEquals(new ArrayGoal(new AtomGoal.Factory(new Atom(-2,3,0,true)).get()), g3.without(g1));
        assertEquals(new ArrayGoal(new AtomGoal.Factory().get()), g1.without(g1));
        assertEquals(new ArrayGoal(), g1.without(g1));
        assertEquals(new ArrayGoal(), g2.without(g2));
        assertEquals(new ArrayGoal(), g1.without(g2));
        assertEquals(new ArrayGoal(), g2.without(g1));
        assertEquals(new ArrayGoal(), g3.without(g3));
        assertEquals(new ArrayGoal(), g4.without(g4));
        assertEquals(new ArrayGoal(new AtomGoal.Factory(
                                        new Atom(0,3,9,false),
                                        new Atom(0,3,10,true),
                                        new Atom(45,3,0,true),
                                        new Atom(-2,3,0,true)
                                                            ).get()), g4.without(g1));
        assertFalse(g1.equivalent(g1.without(new ArrayGoal())));

    }

    @Test
    void testNegative() {

        AtomGoal ag1 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,false)
        ).get();
        AtomGoal ag2 = new AtomGoal.Factory(
                new Atom(0,3,0,false),
                new Atom(0,3,0,false),
                new Atom(1,2,1,false),
                new Atom(-2,3,0,true)
        ).get();
        ArrayGoal g1 = new ArrayGoal(ag1);
        ArrayGoal g2 = new ArrayGoal(ag2);
        assertEquals(g1, g2.negated());
        assertEquals(g2, g1.negated());
        assertEquals(g2, g1.negated());
        assertEquals(g1, g1.negated().negated());
        assertEquals(g2, g2.negated().negated());
        assertNotEquals(g2, g2.negated());
        assertNotEquals(g1, g1.negated());
        assertFalse(g1.equivalent(g1.negated().negated()));
    }

    @Test
    void testTranslated() {
        AtomGoal ag1 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,false)
        ).get();
        AtomGoal ag2 = new AtomGoal.Factory(
                new Atom(1,3,0,true),
                new Atom(1,3,0,true),
                new Atom(2,2,1,true),
                new Atom(-1,3,0,false)
        ).get();
        ArrayGoal g1 = new ArrayGoal(ag1);
        ArrayGoal g2 = new ArrayGoal(ag2);
        assertEquals(g1, g2.translated(-1, 0, 0));
        assertEquals(g2, g1.translated(1, 0, 0));
        assertEquals(g1, g1.translated(0, 0, 0));
        assertFalse(g1.equivalent(g1.translated(0, 0, 0)));
        assertEquals(g1, g1.translated(0, 0, 0).translated(10, -1, 5).translated(-2, 4, -3).translated(-8, -3, -2));
        assertNotEquals(g1, g1.translated(0, 0, 0).translated(10, -1, 5).translated(-2, 4, -3).translated(-8, -4, -2));
    }

    @Test
    void testAdd() {
        AtomGoal ag1 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,false)
        ).get();
        AtomGoal ag2 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,true)
        ).get();
        AtomGoal ag3 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(0,3,9,false),
                new Atom(0,3,10,true),
                new Atom(45,3,0,true),
                new Atom(1,2,1,true),
                new Atom(-2,3,0,true)
        ).get();
        ArrayGoal g1 = new ArrayGoal(ag1);
        ArrayGoal g2 = new ArrayGoal(ag1);
        ArrayGoal g3 = new ArrayGoal(ag2);
        ArrayGoal g4 = new ArrayGoal(ag3);
        assertEquals(g1.added(g2), g2.added(g1));
        assertEquals(g1.added(g3), g2.added(g3));
        assertEquals(g1.added(g3), g3.added(g1));
        assertEquals(g4.added(g3), g3.added(g4));
        AtomGoal ag4 = new AtomGoal.Factory(
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true),
                new Atom(0,3,0,true),
                new Atom(0,3,0,true),
                new Atom(1,2,1,true)
        ).get();
        assertEquals(new ArrayGoal(ag4), g1.added(g3));
    }

    @Test
    void testAllSplits() {
        AtomGoal ag1 = new AtomGoal.Factory(
                new Atom(1, 1, 0, true),
                new Atom(0, 1, 0, true),
                new Atom(0, 1, 0, true),
                new Atom(1, 1, 0, true),
                new Atom(-1, 1, 0, false)
        ).get();
        AtomGoal ag2 = new AtomGoal.Factory(
                new Atom(-1, -1, 0, false),
                new Atom(-1, -1, 0, false),
                new Atom(-1, -1, 0, false),
                new Atom(0, 0, 0, true),
                new Atom(0, 0, 0, true),
                new Atom(0, 0, 0, true),
                new Atom(0, 1, 0, true),
                new Atom(1, 1, 0, true),
                new Atom(1, 1, 0, true),
                new Atom(1, -1, 0, true),
                new Atom(1, -1, 0, true),
                new Atom(0, 1, 0, true),
                new Atom(1, 1, 0, true),
                new Atom(1, 1, 0, true),
                new Atom(1, -1, 0, true),
                new Atom(1, -1, 0, true),
                new Atom(0, 1, 0, true),
                new Atom(1, 1, 0, true),
                new Atom(1, 1, 0, true),
                new Atom(1, -1, 0, true),
                new Atom(1, -1, 0, true),
                new Atom(1, 0, 0, true)
        ).get();
        ArrayGoal g1 = new ArrayGoal(ag1);
        ArrayGoal g2 = new ArrayGoal(ag2);
        for (ArrayGoal g : Arrays.asList(g1, g2)) {
            List<ArrayGoal> goals = g.allSplits();
            for (int i = 0; i < goals.size() / 2; i++) {
                ArrayGoal goal1 = goals.get(i);
                ArrayGoal goal2 = goals.get(goals.size() - i - 1);
                assertEquals(g, goal1.added(goal2));
//                System.out.println(i + " - " + (goals.size() - i - 1) + ":\n " + GoalBag.toGoalsString(Arrays.asList(goal1, goal2), new Bounds.SimpleBounds(1, -1, 1, -1, 0, 0), new boolean[2], new boolean[2], true, true));
            }
        }
        {
            ArrayGoal goal = new ArrayGoal(new AtomGoal(
                    new Atom(1,1,0, true),
                    new Atom(2,1,0, true),
                    new Atom(3,1,0, true),
                    new Atom(4,1,0, true)));
            List<ArrayGoal> lists = goal.allSplits();

            assertTrue(lists.contains(new ArrayGoal()));

            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(1,1,0,true)))));
            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(2,1,0,true)))));
            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(3,1,0,true)))));
            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(4,1,0,true)))));

            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(1,1,0,true), new Atom(2,1,0,true)))));
            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(1,1,0,true), new Atom(3,1,0,true)))));
            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(1,1,0,true), new Atom(4,1,0,true)))));

            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(2,1,0,true), new Atom(3,1,0,true)))));
            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(2,1,0,true), new Atom(4,1,0,true)))));

            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(3,1,0,true), new Atom(4,1,0,true)))));

            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(2,1,0,true), new Atom(3,1,0,true), new Atom(4,1,0,true)))));
            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(1,1,0,true), new Atom(3,1,0,true), new Atom(4,1,0,true)))));
            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(1,1,0,true), new Atom(2,1,0,true), new Atom(4,1,0,true)))));
            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(1,1,0,true), new Atom(2,1,0,true), new Atom(3,1,0,true)))));

            assertTrue(lists.contains(new ArrayGoal(new AtomGoal(new Atom(1,1,0,true), new Atom(2,1,0,true), new Atom(3,1,0,true), new Atom(4,1,0,true)))));

            assertEquals(16, lists.size());
        }
    }


}