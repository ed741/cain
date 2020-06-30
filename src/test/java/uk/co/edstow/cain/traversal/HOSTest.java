package uk.co.edstow.cain.traversal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class HOSTest {
    private int count = 0;
    private Random r = new Random(10);

    private class Temp {
        List<Temp> children;
        final int h;
        int next =0;
        Temp(){h=0;}
        String name;

        Temp(int depth, int h, String name) {
            this.h = h;
            this.name = name;
            count++;
            this.children = new ArrayList<>();
            int c = depth > 0?r.nextInt(5):0;
            for (int i = 0; i < c; i++) {
                this.children.add(new Temp(depth-1, i, name+i));
            }
        }
        Temp next(){
            if(next<this.children.size()){
                int i = next;
                next++;
                return this.children.get(i);
            } else {
                return null;
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }

    TraversalSystem<Temp> hos1;
    TraversalSystem<Temp> hos2;

    @AfterEach
    void tearDown() {
        hos1 =null;
        hos2 =null;
    }

    @Test
    void testHOSFactory() {
        Supplier<? extends TraversalSystem<Temp>> s = HOS.HOSFactory();
        hos1 = s.get();
        hos2 = s.get();
        assertTrue(hos1 != hos2);
    }

    @Test
    void testAdd() {
        testHOSFactory();
        Temp a = new Temp();
        Temp b = new Temp();
        hos1.add(a, b);

    }

    @Test
    void testAdd1() {
        testHOSFactory();
        Temp a = new Temp();
        hos1.add(a);
    }

    @Test
    void testPoll() {
        testHOSFactory();
        Temp a = new Temp();
        Temp poll = hos1.poll();
        assertTrue(poll==null);
        hos1.add(a);
        poll = hos1.poll();
        assertTrue(poll==a);
    }

    @Test
    void testSteal() throws InterruptedException {
        testHOSFactory();
        Temp a = new Temp();
        hos1.add(a);
        Temp b = hos2.steal(hos1);
        assertTrue(a==b);
    }

    @Test
    void testOrder() {
        testHOSFactory();

        Temp root = new Temp(10, 0, "0");
        Set<Temp> active = new HashSet<>();

        hos1.add(root);
        active.add(root);

        Temp current = hos1.poll();
        while (current!=null){

            if(current.next==0) {
                int minH = active.stream().mapToInt(t -> t.h).min().getAsInt();
                active.remove(current);
                assertTrue(current.h <= minH);
                active.addAll(current.children);
            }
            Temp child = current.next();
            if(child!=null) {
                hos1.add(child, current);
            }

            current = hos1.poll();
        }
    }
}