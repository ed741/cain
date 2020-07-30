package uk.co.edstow.cain.traversal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class SOTNTest {

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

    TraversalSystem<Temp> sot1;
    TraversalSystem<Temp> sot2;

    @AfterEach
    void tearDown() {
        sot1=null;
        sot2=null;
    }

    @Test
    void testSOTN1Factory() {
        Supplier<? extends TraversalSystem<Temp>> s = SOTN.SOTNFactory(1);
        sot1 = s.get();
        sot2 = s.get();
        assertTrue(sot1!=sot2);
    }

    @Test
    void testSOTN2Factory() {
        Supplier<? extends TraversalSystem<Temp>> s = SOTN.SOTNFactory(2);
        sot1 = s.get();
        sot2 = s.get();
        assertTrue(sot1!=sot2);
    }

    @Test
    void testAdd() {
        testSOTN1Factory();
        Temp a = new Temp();
        Temp b = new Temp();
        sot1.add(a, b);

    }

    @Test
    void testAdd1() {
        testSOTN1Factory();
        Temp a = new Temp();
        sot1.add(a);
    }

    @Test
    void testPoll() {
        testSOTN1Factory();
        Temp a = new Temp();
        Temp poll = sot1.poll();
        assertTrue(poll==null);
        sot1.add(a);
        poll = sot1.poll();
        assertTrue(poll==a);
    }

    @Test
    void testSteal() throws InterruptedException {
        testSOTN1Factory();
        Temp a = new Temp();
        sot1.add(a);
        Temp b = sot2.steal(sot1);
        assertTrue(a==b);
    }

    @Test
    void testOrder1() {
        testSOTN1Factory();
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

    @Test
    void testOrder2() {

        testSOTN2Factory();
        List<Temp> ts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ts.add(new Temp());
        }
        sot1.add(ts.get(0)); //[0]_[]-2

        Temp a = sot1.poll(); //[]_[]-2
        assertEquals(ts.get(0), a);
        sot1.add(ts.get(1), ts.get(0)); //[]0[1]-1

        Temp b = sot1.poll(); //[]_[1]1
        assertEquals(ts.get(0), b);
        sot1.add(ts.get(2), ts.get(0)); //[1,2,0]_[]-2

        Temp c = sot1.poll(); //[2,0]_[]-2
        assertEquals(ts.get(1), c);
        sot1.add(ts.get(3), ts.get(1)); //[2,0]1[3]-1

        Temp d = sot1.poll(); //[2,0]_[3]-1
        assertEquals(ts.get(1), d);

        Temp e = sot1.poll();  //[2,0]_[]-2
        assertEquals(ts.get(3), e);
        sot1.add(ts.get(4), ts.get(3)); //[2,0]3[4]-1

        Temp f = sot1.poll(); //[2,0]_[4]-1
        assertEquals(ts.get(3), f);
        sot1.add(ts.get(5), ts.get(3)); //[4,5,2,0,3]_[]-2

        Temp g = sot1.poll(); //[5,2,0,3]_[]-2
        assertEquals(ts.get(4), g);

        Temp h = sot1.poll(); //[2,0,3]_[]-2
        assertEquals(ts.get(5), h);

        Temp i = sot1.poll(); //[0,3]_[]-2
        assertEquals(ts.get(2), i);

        Temp j = sot1.poll(); //[3]_[]-2
        assertEquals(ts.get(0), j);

        Temp k = sot1.poll(); //[]_[]-2
        assertEquals(ts.get(3), k);

        Temp l = sot1.poll(); //[]
        assertEquals(null, l);

    }


    @Test
    void testOrderDFS() {

        Supplier<? extends TraversalSystem<Temp>> s = SOTN.SOTNFactory(10000);
        sot1 = s.get();

        List<Temp> ts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ts.add(new Temp());
        }
        sot1.add(ts.get(0)); //[0]_[]-

        Temp a = sot1.poll(); //[]_[]-
        assertEquals(ts.get(0), a);
        sot1.add(ts.get(1), ts.get(0)); //[]0[1]-

        Temp b = sot1.poll(); //[]_[1]-
        assertEquals(ts.get(0), b);
        sot1.add(ts.get(2), ts.get(0)); //[]0[1,2]-

        Temp c = sot1.poll(); //[]_[1,2]-
        assertEquals(ts.get(0), c);
        sot1.add(ts.get(3), ts.get(0)); //[]0[1,2,3]-

        Temp d = sot1.poll(); //[]_[1,2,3]-
        assertEquals(ts.get(0), d);

        Temp e = sot1.poll(); //[2,3]_[]-
        assertEquals(ts.get(1), e);
        sot1.add(ts.get(4), ts.get(1)); //[2,3]1[4]-

        Temp f = sot1.poll(); //[2,3]_[4]-
        assertEquals(ts.get(1), f);
        sot1.add(ts.get(5), ts.get(1)); //[2,3]1[4,5]-

        Temp g = sot1.poll(); //[2,3]_[4,5]-
        assertEquals(ts.get(1), g);

        Temp h = sot1.poll(); //[5,2,3]_[]-
        assertEquals(ts.get(4), h);

        Temp i = sot1.poll(); //[2,3]_[]-
        assertEquals(ts.get(5), i);

        Temp j = sot1.poll(); //[3]_[]-
        assertEquals(ts.get(2), j);

        Temp k = sot1.poll(); //[]_[]-
        assertEquals(ts.get(3), k);

        Temp l = sot1.poll(); //[]
        assertEquals(null, l);

    }
}