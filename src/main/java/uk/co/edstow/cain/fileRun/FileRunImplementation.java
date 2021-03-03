package uk.co.edstow.cain.fileRun;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface FileRunImplementation {
    String key() default "";
    String arg() default "";
}
