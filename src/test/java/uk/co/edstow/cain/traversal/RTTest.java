package uk.co.edstow.cain.traversal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class RTTest {

    private class Temp {}

    TraversalSystem<Temp> rt1;
    TraversalSystem<Temp> rt2;

    @AfterEach
    void tearDown() {
        rt1 =null;
        rt2 =null;
    }

    @Test
    void testSOTFactory() {
        Supplier<? extends TraversalSystem<Temp>> s = RT.RTFactory();
        rt1 = s.get();
        rt2 = s.get();
        assertTrue(rt1 != rt2);
    }

    @Test
    void testAdd() {
        testSOTFactory();
        Temp a = new Temp();
        Temp b = new Temp();
        rt1.add(a, b);

    }

    @Test
    void testAdd1() {
        testSOTFactory();
        Temp a = new Temp();
        rt1.add(a);
    }

    @Test
    void testPoll() {
        testSOTFactory();
        Temp a = new Temp();
        Temp poll = rt1.poll();
        assertTrue(poll==null);
        rt1.add(a);
        poll = rt1.poll();
        assertTrue(poll==a);
    }

    @Test
    void testSteal() throws InterruptedException {
        testSOTFactory();
        Temp a = new Temp();
        rt1.add(a);
        Temp b = rt2.steal(rt1);
        assertTrue(a==b);
    }

}