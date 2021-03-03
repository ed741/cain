package uk.co.edstow.cain.fileRun;

import java.lang.reflect.Constructor;

public class ImproperFileRunDeclaredException extends RuntimeException {
    public ImproperFileRunDeclaredException(Constructor constructor, String e) {
        super(constructor.toString() + " :: " + e);
    }
}
