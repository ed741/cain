package uk.co.edstow.cain.traversal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class CGDSTest {

    private class Temp {}

    TraversalSystem<Temp> cgds1;
    TraversalSystem<Temp> cgds2;

    @AfterEach
    void tearDown() {
        cgds1=null;
        cgds2=null;
    }

    @Test
    void testCGDSFactory() {
        Supplier<? extends TraversalSystem<Temp>> s = CGDS.CGDSFactory();
        cgds1 = s.get();
        cgds2 = s.get();
        assertTrue(cgds1!=cgds2);
    }

    @Test
    void testAdd() {
        testCGDSFactory();
        Temp a = new Temp();
        Temp b = new Temp();
        cgds1.add(a, b);

    }

    @Test
    void testAdd1() {
        testCGDSFactory();
        Temp a = new Temp();
        cgds1.add(a);
    }

    @Test
    void testPoll() {
        testCGDSFactory();
        Temp a = new Temp();
        Temp poll = cgds1.poll();
        assertTrue(poll==null);
        cgds1.add(a);
        poll = cgds1.poll();
        assertTrue(poll==a);
    }

    @Test
    void testSteal() throws InterruptedException {
        testCGDSFactory();
        Temp a = new Temp();
        cgds1.add(a);
        Temp b = cgds2.steal(cgds1);
        assertTrue(a==b);
    }

    @Test
    void testOrder() {
        testCGDSFactory();
        List<Temp> ts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ts.add(new Temp());
        }
        cgds1.add(ts.get(0)); //[0]

        Temp a = cgds1.poll(); //[]
        assertEquals(ts.get(0), a);
        cgds1.add(ts.get(1), ts.get(0)); //[1,0]

        Temp b = cgds1.poll(); //[0]
        assertEquals(ts.get(1), b);
        cgds1.add(ts.get(2), ts.get(1)); //[2,0,1]

        Temp c = cgds1.poll(); //[0,1]
        assertEquals(ts.get(2), c);
        cgds1.add(ts.get(3), ts.get(2)); //[3,0,1,2]

        Temp d = cgds1.poll(); //[0,1,2]
        assertEquals(ts.get(3), d);

        Temp e = cgds1.poll(); //[1,2]
        assertEquals(ts.get(0), e);
        cgds1.add(ts.get(4), ts.get(0)); //[4,1,2,0]

        Temp f = cgds1.poll(); //[1,2,0]
        assertEquals(ts.get(4), f);
        cgds1.add(ts.get(5), ts.get(4)); //[5,1,2,0,4]

        Temp g = cgds1.poll(); //[1,2,0,4]
        assertEquals(ts.get(5), g);

        Temp h = cgds1.poll(); //[2,0,4]
        assertEquals(ts.get(1), h);

        Temp i = cgds1.poll(); //[0,4]
        assertEquals(ts.get(2), i);

        Temp j = cgds1.poll(); //[4]
        assertEquals(ts.get(0), j);

        Temp k = cgds1.poll(); //[]
        assertEquals(ts.get(4), k);

        Temp l = cgds1.poll(); //[]
        assertEquals(null, l);

    }
}