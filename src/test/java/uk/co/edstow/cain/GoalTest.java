package uk.co.edstow.cain;

import uk.co.edstow.cain.structures.Atom;
import uk.co.edstow.cain.structures.Goal;
import uk.co.edstow.cain.util.Tuple;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"CollectionAddedToSelf", "EqualsWithItself", "MismatchedQueryAndUpdateOfCollection"})
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
    void testAllSplitsRecursive() {
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

        Collection<Goal> allSplits = g1.allSplitsRecursive();
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

    @Test
    void testEquivalent() {
        Goal g1 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        Goal g2 = new Goal(g1);

        assertTrue(g1.equivalent(g1));
        assertTrue(g2.equivalent(g2));
        assertFalse(g1.equivalent(g2));
        assertFalse(g2.equivalent(g1));
    }

    @Test
    void testAllSplits() {
        Goal g1 = new Goal(
                new Atom(1,1,0, true),
                new Atom(2,1,0, true),
                new Atom(3,1,0, true),
                new Atom(4,1,0, true));
        List<Goal> lists = g1.allSplits();

        assertTrue(lists.contains(new Goal(new Atom(1,1,0,true))));
        assertTrue(lists.contains(new Goal(new Atom(2,1,0,true))));
        assertTrue(lists.contains(new Goal(new Atom(3,1,0,true))));
        assertTrue(lists.contains(new Goal(new Atom(4,1,0,true))));

        assertTrue(lists.contains(new Goal(new Atom(1,1,0,true), new Atom(2,1,0,true))));
        assertTrue(lists.contains(new Goal(new Atom(1,1,0,true), new Atom(3,1,0,true))));
        assertTrue(lists.contains(new Goal(new Atom(1,1,0,true), new Atom(4,1,0,true))));

        assertTrue(lists.contains(new Goal(new Atom(2,1,0,true), new Atom(3,1,0,true))));
        assertTrue(lists.contains(new Goal(new Atom(2,1,0,true), new Atom(4,1,0,true))));

        assertTrue(lists.contains(new Goal(new Atom(3,1,0,true), new Atom(4,1,0,true))));

        assertTrue(lists.contains(new Goal(new Atom(2,1,0,true), new Atom(3,1,0,true), new Atom(4,1,0,true))));
        assertTrue(lists.contains(new Goal(new Atom(1,1,0,true), new Atom(3,1,0,true), new Atom(4,1,0,true))));
        assertTrue(lists.contains(new Goal(new Atom(1,1,0,true), new Atom(2,1,0,true), new Atom(4,1,0,true))));
        assertTrue(lists.contains(new Goal(new Atom(1,1,0,true), new Atom(2,1,0,true), new Atom(3,1,0,true))));

        assertTrue(lists.contains(new Goal(new Atom(1,1,0,true), new Atom(2,1,0,true), new Atom(3,1,0,true), new Atom(4,1,0,true))));

        assertEquals(15, lists.size());
    }

    @Test
    void testMaximumCount() {
        Goal g1 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        assertEquals(3, g1.maximumCount());
        Goal g2 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,2,0, false),
                new Atom(1,3,0, false));

        assertEquals(2, g2.maximumCount());
    }

    @Test
    void testGetAveragePos() {
        Goal g1 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        assertEquals(3d/5d, g1.getAveragePos().x, 0.001);
        assertEquals(1, g1.getAveragePos().y, 0.001);
        assertEquals(0, g1.getAveragePos().z, 0.001);

    }

    @Test
    void testAllSame() {
        Goal g1 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(0,1,0, true),
                new Atom(0,1,0, false));
        assertFalse(g1.allSame());
        Goal g2 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(0,1,0, false),
                new Atom(0,1,0, true));
        assertTrue(g2.allSame());
    }

    @Test
    void testSubtract() {
        Goal g1 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(0,1,0, false),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false));
        Goal g2 = new Goal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(0,1,0, true));
        assertEquals(new Goal(new Atom(1,1,0, false), new Atom(0,1,0, false), new Atom(0,1,0, false)), g1.subtract(g2));
    }
}