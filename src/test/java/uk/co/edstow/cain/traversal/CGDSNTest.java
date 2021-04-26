package uk.co.edstow.cain.traversal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class CGDSNTest {

    private static class Temp {
        private static int i= 0;
        final int n;

        private Temp() {
            n = i;
            i++;
        }

        @Override
        public String toString() {
            return "["+n+"]";
        }
    }

    TraversalSystem<Temp> cgds1;
    TraversalSystem<Temp> cgds2;

    @AfterEach
    void tearDown() {
        cgds1=null;
        cgds2=null;
    }

    @Test
    void testCGDSN1Factory() {
        Supplier<? extends TraversalSystem<Temp>> s = CGDSN.CGDSNFactory(1);
        cgds1 = s.get();
        cgds2 = s.get();
        assertTrue(cgds1!=cgds2);
    }

    @Test
    void testCGDSN2Factory() {
        Supplier<? extends TraversalSystem<Temp>> s = CGDSN.CGDSNFactory(2);
        cgds1 = s.get();
        cgds2 = s.get();
        assertTrue(cgds1!=cgds2);
    }

    @Test
    void testAdd() {
        testCGDSN1Factory();
        Temp a = new Temp();
        Temp b = new Temp();
        cgds1.add(a, b);

    }

    @Test
    void testAdd1() {
        testCGDSN1Factory();
        Temp a = new Temp();
        cgds1.add(a);
    }

    @Test
    void testPoll() {
        testCGDSN1Factory();
        Temp a = new Temp();
        Temp poll = cgds1.poll();
        assertTrue(poll==null);
        cgds1.add(a);
        poll = cgds1.poll();
        assertTrue(poll==a);
    }

    @Test
    void testSteal() throws InterruptedException {
        testCGDSN1Factory();
        Temp a = new Temp();
        cgds1.add(a);
        Temp b = cgds2.steal(cgds1);
        assertTrue(a==b);
    }

    @Test
    void testOrder1() {
        testCGDSN1Factory();
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

    @Test
    void testOrder2() {

        testCGDSN2Factory();
        List<Temp> ts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ts.add(new Temp());
        }
        cgds1.add(ts.get(0)); //[0]_[]-2

        Temp a = cgds1.poll(); //[]_[]-2
        assertEquals(ts.get(0), a);
        cgds1.add(ts.get(1), ts.get(0)); //[]0[1]-1

        Temp b = cgds1.poll(); //[]_[1]1
        assertEquals(ts.get(0), b);
        cgds1.add(ts.get(2), ts.get(0)); //[1,2,0]_[]-2

        Temp c = cgds1.poll(); //[2,0]_[]-2
        assertEquals(ts.get(1), c);
        cgds1.add(ts.get(3), ts.get(1)); //[2,0]1[3]-1

        Temp d = cgds1.poll(); //[2,0]_[3]-1
        assertEquals(ts.get(1), d);

        Temp e = cgds1.poll();  //[2,0]_[]-2
        assertEquals(ts.get(3), e);
        cgds1.add(ts.get(4), ts.get(3)); //[2,0]3[4]-1

        Temp f = cgds1.poll(); //[2,0]_[4]-1
        assertEquals(ts.get(3), f);
        cgds1.add(ts.get(5), ts.get(3)); //[4,5,2,0,3]_[]-2

        Temp g = cgds1.poll(); //[5,2,0,3]_[]-2
        assertEquals(ts.get(4), g);

        Temp h = cgds1.poll(); //[2,0,3]_[]-2
        assertEquals(ts.get(5), h);

        Temp i = cgds1.poll(); //[0,3]_[]-2
        assertEquals(ts.get(2), i);

        Temp j = cgds1.poll(); //[3]_[]-2
        assertEquals(ts.get(0), j);

        Temp k = cgds1.poll(); //[]_[]-2
        assertEquals(ts.get(3), k);

        Temp l = cgds1.poll(); //[]
        assertEquals(null, l);

    }


    @Test
    void testOrderDFS() {

        Supplier<? extends TraversalSystem<Temp>> s = CGDSN.CGDSNFactory(10000);
        cgds1 = s.get();

        List<Temp> ts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ts.add(new Temp());
        }
        cgds1.add(ts.get(0)); //[0]_[]-

        Temp a = cgds1.poll(); //[]_[]-
        assertEquals(ts.get(0), a);
        cgds1.add(ts.get(1), ts.get(0)); //[]0[1]-

        Temp b = cgds1.poll(); //[]_[1]-
        assertEquals(ts.get(0), b);
        cgds1.add(ts.get(2), ts.get(0)); //[]0[1,2]-

        Temp c = cgds1.poll(); //[]_[1,2]-
        assertEquals(ts.get(0), c);
        cgds1.add(ts.get(3), ts.get(0)); //[]0[1,2,3]-

        Temp d = cgds1.poll(); //[]_[1,2,3]-
        assertEquals(ts.get(0), d);

        Temp e = cgds1.poll(); //[2,3]_[]-
        assertEquals(ts.get(1), e);
        cgds1.add(ts.get(4), ts.get(1)); //[2,3]1[4]-

        Temp f = cgds1.poll(); //[2,3]_[4]-
        assertEquals(ts.get(1), f);
        cgds1.add(ts.get(5), ts.get(1)); //[2,3]1[4,5]-

        Temp g = cgds1.poll(); //[2,3]_[4,5]-
        assertEquals(ts.get(1), g);

        Temp h = cgds1.poll(); //[5,2,3]_[]-
        assertEquals(ts.get(4), h);

        Temp i = cgds1.poll(); //[2,3]_[]-
        assertEquals(ts.get(5), i);

        Temp j = cgds1.poll(); //[3]_[]-
        assertEquals(ts.get(2), j);

        Temp k = cgds1.poll(); //[]_[]-
        assertEquals(ts.get(3), k);

        Temp l = cgds1.poll(); //[]
        assertEquals(null, l);

    }
}