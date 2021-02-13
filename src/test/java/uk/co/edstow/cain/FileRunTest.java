package uk.co.edstow.cain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class FileRunTest {
    @Test
    void testSobelAnalogueAtom() {
        String json =
                "{\"name\":\"Sobel\",\n" +
                "  \"verbose\":-1,\n" +
                "  \"goalSystem\":Kernel3D,\n" +
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
                "    \"name\": \"Scamp5AnalogueAtomGoal\",\n" +
                "    \"configGetter\": \"Threshold\",\n" +
                "    \"ops\":\"all\",\n" +
                "    \"threshold\":10\n" +
                "  },\n" +
                "  \"verifier\":\"Scamp5Emulator\"\n" +
                "\n" +
                "}";
        try {
            FileRun<?> fileRun = FileRun.loadFromJson(json);
            fileRun.run();
            List<? extends FileRun<?>.Result> results = fileRun.getResults();
            OptionalInt min = results.stream().mapToInt(r -> r.depth).min();
            assertTrue(min.isPresent());
            assertTrue(min.getAsInt()<=5);
        } catch (Exception e){
            e.printStackTrace();
            e.getMessage();
            fail();
        }

    }
    @Test
    void testSobelAnalogueArray() {
        String json =
                "{\"name\":\"Sobel\",\n" +
                        "  \"verbose\":-1,\n" +
                        "  \"goalSystem\":Kernel3D,\n" +
                        "  \"maxApproximationDepth\":3,\n" +
                        "  \"maxApproximationError\":0,\n" +
                        "  \"3d\": false,\n" +
                        "  \"filter\":{\n" +
                        "    \"A\": {\"depth\":0,\n" +
                        "      \"array\":\n" +
                        "        [[1, 1, -1],\n" +
                        "         [2, 1, -2],\n" +
                        "         [1, 1, -1]\n" +
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
                        "    \"name\": \"Scamp5AnalogueArrayGoal\",\n" +
                        "    \"configGetter\": \"Threshold\",\n" +
                        "    \"ops\":\"all\",\n" +
                        "    \"threshold\":10\n" +
                        "  },\n" +
                        "  \"verifier\":\"Scamp5Emulator\"\n" +
                        "\n" +
                        "}";
        try {
            FileRun<?> fileRun = FileRun.loadFromJson(json);
            fileRun.run();
            List<? extends FileRun<?>.Result> results = fileRun.getResults();
            OptionalInt min = results.stream().mapToInt(r -> r.depth).min();
            assertTrue(min.isPresent());
            assertTrue(min.getAsInt()<=6);
        } catch (Exception e){
            e.printStackTrace();
            e.getMessage();
            fail();
        }

    }
    @Test
    void testDigitalBox() {
        String json =
                "{\"name\":\"DigitalBox\",\n" +
                        "  \"verbose\":-1,\n" +
                        "  \"goalSystem\":Kernel3D,\n" +
                        "  \"maxApproximationDepth\":3,\n" +
                        "  \"maxApproximationError\":0,\n" +
                        "  \"3d\": false,\n" +
                        "  \"filter\":{\n" +
                        "    \"A\": {\"depth\":0,\n" +
                        "      \"array\":\n" +
                        "        [[1, 1, 1],\n" +
                        "         [1, 1, 1],\n" +
                        "         [1, 1, 1]\n" +
                        "        ]},\n" +
                        "    \"B\": {\"depth\":0,\n" +
                        "      \"array\":\n" +
                        "        [[1, 1, 0],\n" +
                        "         [1, 1, 0],\n" +
                        "         [0, 0, 0]\n" +
                        "        ]}\n" +
                        "  },\n" +
                        "\n" +
                        "  \"availableRegisters\":[\"A\",\"B\",\"C\"],\n" +
                        "  \"initialRegisters\":[\"A\"],\n" +
                        "\n" +
                        "  \"runConfig\":{\n" +
                        "    \"searchTime\":1000,\n" +
                        "    \"timeOut\":true,\n" +
                        "    \"workers\":4,\n" +
                        "    \"traversalAlgorithm\":\"SOT\",\n" +
                        "    \"costFunction\":\"InstructionCost\",\n" +
                        "    \"liveCounter\":false,\n" +
                        "    \"quiet\":true,\n" +
                        "    \"livePrintPlans\":0,\n" +
                        "    \"initialMaxDepth\":200,\n" +
                        "    \"forcedDepthReduction\":0,\n" +
                        "    \"initialMaxCost\":2147483647,\n" +
                        "    \"forcedCostReduction\":1,\n" +
                        "    \"allowableAtomsCoefficient\":2,\n" +
                        "    \"goalReductionsPerStep\":1,\n" +
                        "    \"goalReductionsTolerance\":1\n" +
                        "  },\n" +
                        "\n" +
                        "  \"pairGen\":{\n" +
                        "    \"name\": \"Scamp5DigitalAtomGoal\",\n" +
                        "    \"configGetter\": \"Threshold\",\n" +
                        "    \"threshold\":10,\n" +
                        "    \"bits\":2,\n" +
                        "    \"scratchRegs\":[\"S1\", \"S2\", \"S3\", \"S4\"],\n" +
                        "    \"regMapping\":{\n" +
                        "      \"A\":[\"DA1\",\"DA2\"],\n" +
                        "      \"B\":[\"DB1\",\"DB2\"],\n" +
                        "      \"C\":[\"DC1\",\"DC2\"],\n" +
                        "      \"D\":[\"DD1\",\"DD2\"]\n" +
                        "    }\n" +
                        "  },\n" +
                        "  \"verifier\":\"None\"\n" +
                        "\n" +
                        "}";
        try {
            FileRun<?> fileRun = FileRun.loadFromJson(json);
            fileRun.run();
            List<? extends FileRun<?>.Result> results = fileRun.getResults();
//            System.out.println((fileRun.getBest()));
            OptionalInt min = results.stream().mapToInt(r -> r.cost).min();
            assertTrue(min.isPresent());
            assertTrue(min.getAsInt()<=140);
        } catch (Exception e){
            e.printStackTrace();
            e.getMessage();
            fail();
        }

    }
}