package uk.co.edstow.cain.goals.atomGoal;

import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.util.Tuple;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"CollectionAddedToSelf", "EqualsWithItself", "MismatchedQueryAndUpdateOfCollection"})
class AtomGoalTest {

    @Test
    void testWithout() {
        AtomGoal g1 = new AtomGoal(
                new Atom(0,0,0, true),
                new Atom(0,0,0, true),
                new Atom(1,0,0, false));
        AtomGoal ge = new AtomGoal(
                new Atom(0,0,0, true),
                new Atom(1,0,0, false));
        AtomGoal ga = g1.without(new AtomGoal(new Atom(0,0,0, true)));

        assertEquals(ge, ga);

        {
            AtomGoal.Factory f1 = new AtomGoal.Factory();
            f1.add(0, 0, 0, 12);
            AtomGoal t1 = f1.get();
            AtomGoal.Factory f2 = new AtomGoal.Factory();
            f2.add(0, 0, 0, 17);
            AtomGoal t2 = f2.get();
            AtomGoal.Factory f3 = new AtomGoal.Factory();
            AtomGoal t3 = f3.get();
            assertEquals(t3, t1.without(t2));
        }
    }

    @Test
    void testSame() {
        AtomGoal g1 = new AtomGoal(
                new Atom(0,0,0, true),
                new Atom(0,0,0, true),
                new Atom(1,0,0, false));
        AtomGoal g2 = new AtomGoal(
                new Atom(0,0,0, true),
                new Atom(1,0,0, false));

        assertFalse(g1.same(g2));
        assertTrue(g1.same(g1));
        assertTrue(g2.same(g2));
    }

    @Test
    void testNegated() {
        AtomGoal g1 = new AtomGoal(
                new Atom(0,0,0, true),
                new Atom(0,0,0, true),
                new Atom(1,0,0, false));

        AtomGoal ge = new AtomGoal(
                new Atom(0,0,0, false),
                new Atom(0,0,0, false),
                new Atom(1,0,0, true));

        assertEquals(ge, g1.negated());
    }

    @Test
    void testTranslated() {
        AtomGoal g1 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false));

        AtomGoal ge = new AtomGoal(
                new Atom(0,2,0, true),
                new Atom(0,2,0, true),
                new Atom(1,2,0, false));

        assertEquals(ge, g1.translated(0,1,0));
    }

    @Test
    void testAllSplitsRecursive() {
        AtomGoal g1 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(1,1,0, false));

        AtomGoal ge1 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(1,1,0, false));
        AtomGoal ge2 = new AtomGoal(
                new Atom(0,1,0, true));
        AtomGoal ge3 = new AtomGoal(
                new Atom(1,1,0, false));

        Collection<AtomGoal> allSplits = g1.allSplitsRecursive();
        assertTrue(allSplits.contains(ge1));
        assertTrue(allSplits.contains(ge2));
        assertTrue(allSplits.contains(ge3));
        assertEquals(3, allSplits.size());
    }

    @Test
    void testDivide() {
        AtomGoal g1 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        AtomGoal g2 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(1,1,0, false));

        assertEquals(2, g1.divide(g2));
    }

    @Test
    void testMinimumCount() {
        AtomGoal g1 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));
        assertEquals(2, g1.minimumCount());
    }

    @Test
    void testCompareTo() {
        AtomGoal g1 = new AtomGoal(
                new Atom(1,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false));
        AtomGoal g2 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(1,1,0, false));
        AtomGoal g3 = new AtomGoal(
                new Atom(0,0,0, true),
                new Atom(1,1,1, false));

        assertEquals(g1.compareTo(g2), -g2.compareTo(g1));
        assertEquals(0, g1.compareTo(g1));
        assertNotEquals(0, g2.compareTo(g3));
    }

    @Test
    void testAtomCount() {
        AtomGoal g1 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));
        assertEquals(5, g1.atomCount());
    }

    @Test
    void testHasSubGoal() {
        AtomGoal g1 = new AtomGoal(
                new Atom(1,-1,0, true),
                new Atom(1,-1,0, true),
                new Atom(1,1,0, false));

        AtomGoal g2 = new AtomGoal(
                new Atom(1,-1,0, true),
                new Atom(1,1,0, false));

        assertTrue(g1.hasSubGoal(g2));
        assertFalse(g2.hasSubGoal(g1));
    }

    @Test
    void testUniqueIterator() {
        AtomGoal g1 = new AtomGoal(
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
    void testUniqueAtomCountIterator() {
        AtomGoal g1 = new AtomGoal(
                new Atom(1,-1,0, true),
                new Atom(1,-1,0, true),
                new Atom(1,1,0, false));
        Iterator<Tuple<Atom, Integer>> it = g1.uniqueAtomCountIterator();
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
    void testUniqueCountIterator() {
        AtomGoal g1 = new AtomGoal(
                new Atom(1,-1,0, true),
                new Atom(1,-1,0, true),
                new Atom(1,1,0, false));
        Iterator<Tuple<Kernel3DGoal.Coord, Integer>> it = g1.uniqueCountIterator();
        Map<Atom, Integer> map = new HashMap<>();
        while(it.hasNext()){
            Tuple<Kernel3DGoal.Coord, Integer> t = it.next();
            map.put(new Atom(t.getA(), t.getB()>0), Math.abs(t.getB()));
        }

        assertEquals(2, (int) map.get(new Atom(1,-1,0, true)));
        assertEquals(1, (int) map.get(new Atom(1,1,0, false)));
        assertEquals(2, map.size());
    }

    @Test
    void testCount() {
        AtomGoal g1 = new AtomGoal(
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
        AtomGoal g1 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        AtomGoal g2 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        assertEquals(g1.hashCode(), g2.hashCode());
    }

    @Test
    void testEquals() {

        AtomGoal g1 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        AtomGoal g2 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        assertEquals(g1, g1);
        assertEquals(g2, g2);
        assertNotEquals(g2, g1);

    }

    @Test
    void testEquivalent() {
        AtomGoal g1 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        AtomGoal g2 = new AtomGoal(g1);

        assertTrue(g1.equivalent(g1));
        assertTrue(g2.equivalent(g2));
        assertFalse(g1.equivalent(g2));
        assertFalse(g2.equivalent(g1));
    }

    @Test
    void testAllSplits() {
        AtomGoal g1 = new AtomGoal(
                new Atom(1,1,0, true),
                new Atom(2,1,0, true),
                new Atom(3,1,0, true),
                new Atom(4,1,0, true));
        List<AtomGoal> lists = g1.allSplits();

        assertTrue(lists.contains(new AtomGoal()));

        assertTrue(lists.contains(new AtomGoal(new Atom(1,1,0,true))));
        assertTrue(lists.contains(new AtomGoal(new Atom(2,1,0,true))));
        assertTrue(lists.contains(new AtomGoal(new Atom(3,1,0,true))));
        assertTrue(lists.contains(new AtomGoal(new Atom(4,1,0,true))));

        assertTrue(lists.contains(new AtomGoal(new Atom(1,1,0,true), new Atom(2,1,0,true))));
        assertTrue(lists.contains(new AtomGoal(new Atom(1,1,0,true), new Atom(3,1,0,true))));
        assertTrue(lists.contains(new AtomGoal(new Atom(1,1,0,true), new Atom(4,1,0,true))));

        assertTrue(lists.contains(new AtomGoal(new Atom(2,1,0,true), new Atom(3,1,0,true))));
        assertTrue(lists.contains(new AtomGoal(new Atom(2,1,0,true), new Atom(4,1,0,true))));

        assertTrue(lists.contains(new AtomGoal(new Atom(3,1,0,true), new Atom(4,1,0,true))));

        assertTrue(lists.contains(new AtomGoal(new Atom(2,1,0,true), new Atom(3,1,0,true), new Atom(4,1,0,true))));
        assertTrue(lists.contains(new AtomGoal(new Atom(1,1,0,true), new Atom(3,1,0,true), new Atom(4,1,0,true))));
        assertTrue(lists.contains(new AtomGoal(new Atom(1,1,0,true), new Atom(2,1,0,true), new Atom(4,1,0,true))));
        assertTrue(lists.contains(new AtomGoal(new Atom(1,1,0,true), new Atom(2,1,0,true), new Atom(3,1,0,true))));

        assertTrue(lists.contains(new AtomGoal(new Atom(1,1,0,true), new Atom(2,1,0,true), new Atom(3,1,0,true), new Atom(4,1,0,true))));

        assertEquals(16, lists.size());
    }

    @Test
    void testMaximumCount() {
        AtomGoal g1 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false),
                new Atom(1,1,0, false));

        assertEquals(3, g1.maximumCount());
        AtomGoal g2 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(1,2,0, false),
                new Atom(1,3,0, false));

        assertEquals(2, g2.maximumCount());
    }

    @Test
    void testGetAveragePos() {
        AtomGoal g1 = new AtomGoal(
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
        AtomGoal g1 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false),
                new Atom(0,1,0, true),
                new Atom(0,1,0, false));
        assertFalse(g1.allSame());
        AtomGoal g2 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(0,1,0, false),
                new Atom(0,1,0, true));
        assertTrue(g2.allSame());
    }

    @Test
    void testSubtract() {
        AtomGoal g1 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(0,1,0, false),
                new Atom(0,1,0, true),
                new Atom(1,1,0, false));
        AtomGoal g2 = new AtomGoal(
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(0,1,0, true),
                new Atom(0,1,0, true));
        assertEquals(new AtomGoal(new Atom(1,1,0, false), new Atom(0,1,0, false), new Atom(0,1,0, false)), g1.subtracted(g2));
    }
}