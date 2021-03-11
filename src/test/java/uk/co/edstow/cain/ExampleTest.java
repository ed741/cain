package uk.co.edstow.cain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.co.edstow.cain.fileRun.FileRun;
import uk.co.edstow.cain.fileRun.Result;;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


public class ExampleTest {
    public static Stream<Arguments> data() {
        List<Arguments> args = new ArrayList<>();
        File folder = new File("testExamples/");
        File[] files = folder.listFiles();
        assertNotNull(files);
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                String path = file.getPath();
                args.add(Arguments.of(path));
            }
        }
        return args.stream();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void test(String path) {
        try {
            FileRun<?,?,?> fileRun = FileRun.loadFromJson(path);
            fileRun.run();
            List<? extends Result> results = fileRun.getResults();
            OptionalInt min = results.stream().mapToInt(r -> r.depth).min();
            assertTrue(min.isPresent(), "Unable to solve: " + path);
        } catch (Exception e){
            e.printStackTrace();
            e.getMessage();
            fail();
        }
    }
}
