package uk.co.edstow.cpacgen.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Tuple<A, B> implements Iterable<Object> {
    final A a;
    final B b;

    public Tuple(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getA(){
        return a;
    }

    public B getB() {
        return b;
    }

    public static <X, Y, Z> Tuple<X, Tuple<Y, Z>> triple(X x, Y y, Z z){
        return new Tuple<>(x, new Tuple<>(y, z));
    }

    @Override
    public Iterator<Object> iterator() {
        return new
                Iterator<Object>() {
                    boolean hasA = true;
                    boolean hasB = true;
                    @Override
                    public boolean hasNext() {
                        return hasA || hasB;
                    }

                    @Override
                    public Object next() {
                        if (hasA) {
                            hasA = false;
                            return a;
                        }else if (hasB){
                            hasB = false;
                            return b;
                        } else {
                            throw new NoSuchElementException();
                        }
                    }
                };
    }

    @Override
    public int hashCode() {
        return a.hashCode() ^ b.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Tuple) && a.equals(((Tuple) o).getA()) && b.equals(((Tuple) o).getB());

    }

    @Override
    public String toString() {
        return "(" + a +", "+ b + ")";
    }
}
