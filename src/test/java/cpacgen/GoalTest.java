package cpacgen;

import cpacgen.util.Tuple;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GoalTest {

    @Test
    void testWithout() {
        Goal g1 = new Goal(
                new Atom(0,0,0, true),
                new Atom(0,0,0, true),
                new Atom(1,0,0, false));
        Goal ge = new Goal(
                new Atom(0,0,0, true),
                new Atom(1,0,0, false));
        Goal ga = g1.without(new Goal(new Atom(0,0,0, true)));

        assertEquals(ge, ga);
    }

    @Test
    void testSame() {
        Goal g1 = new Goal(
                new Atom(0,0,0, true),
                new Atom(0,0,0, true),
                new Atom(1,0,0, false));
        Goal g2 = new Goal(
                new Atom(0,0,0, true),
                new Atom(1,0,0, false));

        assertFalse(g1.same(g2));
        assertTrue(g1.same(g1));
        assertTrue(g2.same(g2));
    }

    @Test
    void testNegative() {
        Goal g1 = new Goal(
                new Atom(0,0,0, true),
                new Atom(0,0,0, true),
                new Atom(1,0,0, false));

        Goal ge = new Goal(
                new Atom(0,0,0, false),
                new Atom(0,0,0, false),
                new Atom(1,0,0, true));

        assertEquals(ge, g1.negative());
    }

    @Test
    void testTranslated() {
        Goal g1 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false));

        Goal ge = new Goal(
                new Atom(0,2,0, true),
                new Atom(0,2,0, true),
                new Atom(1,2,0, false));

        assertEquals(ge, g1.translated(0,1,0));
    }

    @Test
    void testAllSplits() {
        Goal g1 = new Goal(
                new Atom(0,1,0, true),
                new Atom(1,1,0, false));

        Goal ge1 = new Goal(
                new Atom(0,1,0, true),
                new Atom(1,1,0, false));
        Goal ge2 = new Goal(
                new Atom(0,1,0, true));
        Goal ge3 = new Goal(
                new Atom(1,1,0, false));

        Collection<Goal> allSplits = g1.allSplits();
        assertTrue(allSplits.contains(ge1));
        assertTrue(allSplits.contains(ge2));
        assertTrue(allSplits.contains(ge3));
        assertTrue(allSplits.size()==3);
    }

    @Test
    void testDivide() {
        Goal g1 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        Goal g2 = new Goal(
                new Atom(0,1,0, true),
                new Atom(1,1,0, false));

        assertEquals(2, g1.divide(g2));
    }

    @Test
    void testMinimumCount() {
        Goal g1 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));
        assertEquals(2, g1.minimumCount());
    }

    @Test
    void testCompareTo() {
        Goal g1 = new Goal(
                new Atom(1,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false));
        Goal g2 = new Goal(
                new Atom(0,1,0, true),
                new Atom(1,1,0, false));

        assertEquals(g1.compareTo(g2), -g2.compareTo(g1));
        assertEquals(0, g1.compareTo(g1));
    }

    @Test
    void testAtomCount() {
        Goal g1 = new Goal(
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));
        assertEquals(5, g1.atomCount());
    }

    @Test
    void testHasSubGoal() {
        Goal g1 = new Goal(
                new Atom(1,-1,0, true),
                new Atom(1,-1,0, true),
                new Atom(1,1,0, false));

        Goal g2 = new Goal(
                new Atom(1,-1,0, true),
                new Atom(1,1,0, false));

        assertTrue(g1.hasSubGoal(g2));
        assertFalse(g2.hasSubGoal(g1));
    }

    @Test
    void testUniqueIterator() {
        Goal g1 = new Goal(
                new Atom(1,-1,0, true),
                new Atom(1,-1,0, true),
                new Atom(1,1,0, false));
        Iterator<Atom> it = g1.uniqueIterator();
        List<Atom> list = new ArrayList<>();
        while(it.hasNext()){
            list.add(it.next());
        }
        assertTrue(list.contains(new Atom(1,-1,0,true)));
        assertTrue(list.contains(new Atom(1,1,0,false)));
        assertEquals(2, list.size());

    }

    @Test
    void testUniqueCountIterator() {
        Goal g1 = new Goal(
                new Atom(1,-1,0, true),
                new Atom(1,-1,0, true),
                new Atom(1,1,0, false));
        Iterator<Tuple<Atom, Integer>> it = g1.uniqueCountIterator();
        Map<Atom, Integer> map = new HashMap<>();
        while(it.hasNext()){
            Tuple<Atom, Integer> t = it.next();
            map.put(t.getA(), t.getB());
        }

        assertEquals(2, (int) map.get(new Atom(1,-1,0, true)));
        assertEquals(1, (int) map.get(new Atom(1,1,0, false)));
        assertEquals(2, map.size());
    }

    @Test
    void testCount() {
        Goal g1 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        assertEquals(2, g1.count(new Atom(0,1,0, true)));
        assertEquals(3, g1.count(new Atom(1,1,0, false)));
    }

    @Test
    void testHashCode() {
        Goal g1 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        Goal g2 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        assertEquals(g1.hashCode(), g2.hashCode());
    }

    @Test
    void testEquals() {

        Goal g1 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        Goal g2 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        assertTrue(g1.equals(g1));
        assertTrue(g2.equals(g2));
        assertFalse(g1.equals(g2));

    }
}