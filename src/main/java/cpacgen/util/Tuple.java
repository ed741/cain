package cpacgen.util;

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
}
