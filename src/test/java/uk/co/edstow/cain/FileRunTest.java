package uk.co.edstow.cain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileRunTest {
    @Test
    void testSobel() {
        String json =
                "{\"name\":\"Sobel\",\n" +
                "  \"verbose\":-1,\n" +
                "  \"goalSystem\":Atom,\n" +
                "  \"maxApproximationDepth\":3,\n" +
                "  \"maxApproximationError\":0,\n" +
                "  \"3d\": false,\n" +
                "  \"filter\":{\n" +
                "    \"A\": {\"depth\":0,\n" +
                "      \"array\":\n" +
                "        [[1, 0, -1],\n" +
                "         [2, 0, -2],\n" +
                "         [1, 0, -1]\n" +
                "        ]}\n" +
                "  },\n" +
                "\n" +
                "  \"availableRegisters\":[\"A\",\"B\",\"C\",\"D\",\"E\",\"F\"],\n" +
                "  \"initialRegisters\":[\"A\"],\n" +
                "\n" +
                "  \"runConfig\":{\n" +
                "    \"searchTime\":1000,\n" +
                "    \"timeOut\":true,\n" +
                "    \"workers\":4,\n" +
                "    \"traversalAlgorithm\":\"SOT\",\n" +
                "    \"costFunction\":\"CircuitDepthThenLength\",\n" +
                "    \"liveCounter\":false,\n" +
                "    \"quiet\":true,\n" +
                "    \"livePrintPlans\":0,\n" +
                "    \"initialMaxDepth\":200,\n" +
                "    \"forcedDepthReduction\":1,\n" +
                "    \"initialMaxCost\":2147483647,\n" +
                "    \"forcedCostReduction\":0,\n" +
                "    \"allowableAtomsCoefficient\":2,\n" +
                "    \"goalReductionsPerStep\":1,\n" +
                "    \"goalReductionsTolerance\":1\n" +
                "  },\n" +
                "\n" +
                "  \"pairGen\":{\n" +
                "    \"name\": \"Scamp5\",\n" +
                "    \"configGetter\": \"Threshold\",\n" +
                "    \"ops\":\"all\",\n" +
                "    \"threshold\":10\n" +
                "  },\n" +
                "  \"verifier\":\"Scamp5Emulator\"\n" +
                "\n" +
                "}";
        try {
            FileRun<?,?> fileRun = FileRun.loadFromJson(json);
            fileRun.run();
            List<? extends FileRun<?,?>.Result> results = fileRun.getResults();
            assertTrue(results.stream().mapToInt(r -> r.depth).min().getAsInt()<=5);
        } catch (Exception e){
            e.printStackTrace();
            e.getMessage();
            fail();
        }

    }
}