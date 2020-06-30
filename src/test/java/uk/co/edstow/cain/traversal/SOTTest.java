package uk.co.edstow.cain.traversal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class SOTTest {

    private class Temp {}

    TraversalSystem<Temp> sot1;
    TraversalSystem<Temp> sot2;

    @AfterEach
    void tearDown() {
        sot1=null;
        sot2=null;
    }

    @Test
    void testSOTFactory() {
        Supplier<? extends TraversalSystem<Temp>> s = SOT.SOTFactory();
        sot1 = s.get();
        sot2 = s.get();
        assertTrue(sot1!=sot2);
    }

    @Test
    void testAdd() {
        testSOTFactory();
        Temp a = new Temp();
        Temp b = new Temp();
        sot1.add(a, b);

    }

    @Test
    void testAdd1() {
        testSOTFactory();
        Temp a = new Temp();
        sot1.add(a);
    }

    @Test
    void testPoll() {
        testSOTFactory();
        Temp a = new Temp();
        Temp poll = sot1.poll();
        assertTrue(poll==null);
        sot1.add(a);
        poll = sot1.poll();
        assertTrue(poll==a);
    }

    @Test
    void testSteal() throws InterruptedException {
        testSOTFactory();
        Temp a = new Temp();
        sot1.add(a);
        Temp b = sot2.steal(sot1);
        assertTrue(a==b);
    }

    @Test
    void testOrder() {
        testSOTFactory();
        List<Temp> ts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ts.add(new Temp());
        }
        sot1.add(ts.get(0)); //[0]

        Temp a = sot1.poll(); //[]
        assertEquals(ts.get(0), a);
        sot1.add(ts.get(1), ts.get(0)); //[1,0]

        Temp b = sot1.poll(); //[0]
        assertEquals(ts.get(1), b);
        sot1.add(ts.get(2), ts.get(1)); //[2,0,1]

        Temp c = sot1.poll(); //[0,1]
        assertEquals(ts.get(2), c);
        sot1.add(ts.get(3), ts.get(2)); //[3,0,1,2]

        Temp d = sot1.poll(); //[0,1,2]
        assertEquals(ts.get(3), d);

        Temp e = sot1.poll(); //[1,2]
        assertEquals(ts.get(0), e);
        sot1.add(ts.get(4), ts.get(0)); //[4,1,2,0]

        Temp f = sot1.poll(); //[1,2,0]
        assertEquals(ts.get(4), f);
        sot1.add(ts.get(5), ts.get(4)); //[5,1,2,0,4]

        Temp g = sot1.poll(); //[1,2,0,4]
        assertEquals(ts.get(5), g);

        Temp h = sot1.poll(); //[2,0,4]
        assertEquals(ts.get(1), h);

        Temp i = sot1.poll(); //[0,4]
        assertEquals(ts.get(2), i);

        Temp j = sot1.poll(); //[4]
        assertEquals(ts.get(0), j);

        Temp k = sot1.poll(); //[]
        assertEquals(ts.get(4), k);

        Temp l = sot1.poll(); //[]
        assertEquals(null, l);

    }
}