package uk.co.edstow.cain;

import org.junit.jupiter.api.Test;
import uk.co.edstow.cain.fileRun.FileRun;
import uk.co.edstow.cain.fileRun.Result;
import uk.co.edstow.cain.goals.arrayGoal.ArrayGoal;
import uk.co.edstow.cain.goals.atomGoal.AtomGoal;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.scamp5.analogue.Scamp5AnalogueFileRun;
import uk.co.edstow.cain.scamp5.analogue.Scamp5AnalogueTransformation;
import uk.co.edstow.cain.scamp5.digital.Scamp5DigitalFileRun;
import uk.co.edstow.cain.scamp5.digital.Scamp5DigitalTransformation;

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
                "  \"registerAllocator\": {\n" +
                "    \"name\": \"linearScan\",\n" +
                "    \"availableRegisters\":[\"A\",\"B\",\"C\",\"D\",\"E\",\"F\"],\n" +
                "    \"initialRegisters\":[\"A\"]\n" +
                "  },\n" +
                "\n" +
                "  \"runConfig\":{\n" +
                "    \"searchTime\":3000,\n" +
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
                "    \"configGetter\": {\n" +
                "      \"name\":\"Threshold\",\n" +
                "      \"threshold\":10,\n" +
                "      \"ops\":all,\n" +
                "      \"heuristic\": \"Pattern\"\n" +
                "    }," +
                "    \"ops\":\"all\",\n" +
                "    \"threshold\":10,\n" +
                "    \"outputFormat\": {\n" +
                "      \"name\": \"defaultFormat\"\n" +
                "   }," +
                "  },\n" +
                "  \"verifier\":\"Scamp5Emulator\"\n" +
                "\n" +
                "}";
        try {
            FileRun<AtomGoal, Scamp5AnalogueTransformation<AtomGoal>, Register> fileRun = new Scamp5AnalogueFileRun.AtomGoalFileRun(FileRun.fromJson(json, true));
            fileRun.run();
            List<Result<AtomGoal, Scamp5AnalogueTransformation<AtomGoal>, Register>> results = fileRun.getResults();
            OptionalInt min = results.stream().mapToInt(r -> r.depth).min();
            assertTrue(min.isPresent());
            assertTrue(min.getAsInt()<=5, "Got value: " +min.getAsInt() + " Expected: <= 5");
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
                        "  \"registerAllocator\": {\n" +
                        "    \"name\": \"linearScan\",\n" +
                        "    \"availableRegisters\":[\"A\",\"B\",\"C\",\"D\",\"E\",\"F\"],\n" +
                        "    \"initialRegisters\":[\"A\"]\n" +
                        "  },\n" +
                        "\n" +
                        "  \"runConfig\":{\n" +
                        "    \"searchTime\":3000,\n" +
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
                        "    \"configGetter\": {\n" +
                        "      \"name\":\"Threshold\",\n" +
                        "      \"threshold\":10,\n" +
                        "      \"ops\":all,\n" +
                        "      \"heuristic\": \"Pattern\"\n" +
                        "    }," +
                        "    \"outputFormat\": {\n" +
                        "      \"name\": \"defaultFormat\"\n" +
                        "    }," +
                        "  },\n" +
                        "  \"verifier\":\"Scamp5Emulator\"\n" +
                        "\n" +
                        "}";
        try {
            FileRun<ArrayGoal, Scamp5AnalogueTransformation<ArrayGoal>, Register> fileRun = new Scamp5AnalogueFileRun.ArrayGoalFileRun(FileRun.fromJson(json, true));
            fileRun.run();
            List<Result<ArrayGoal, Scamp5AnalogueTransformation<ArrayGoal>, Register>> results = fileRun.getResults();
            OptionalInt min = results.stream().mapToInt(r -> r.depth).min();
            assertTrue(min.isPresent());
            assertTrue(min.getAsInt()<=7, "Got value: " +min.getAsInt() + " Expected: <= 7");
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
                        "  \"registerAllocator\": {\n" +
                        "    \"name\": \"linearScan\",\n" +
                        "    \"availableRegisters\":[\"A\",\"B\",\"C\"],\n" +
                        "    \"initialRegisters\":[\"A\"]\n" +
                        "  },\n" +
                        "\n" +
                        "  \"runConfig\":{\n" +
                        "    \"searchTime\":3000,\n" +
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
                        "    \"configGetter\": {\n" +
                        "      \"name\":\"Threshold\",\n" +
                        "      \"threshold\":10,\n" +
                        "      \"heuristic\": \"Pattern\"\n" +
                        "    }," +
                        "    \"bits\":2,\n" +
                        "    \"scratchRegs\":[\"S1\", \"S2\", \"S3\", \"S4\"],\n" +
                        "    \"regMapping\":{\n" +
                        "      \"A\":[\"DA1\",\"DA2\"],\n" +
                        "      \"B\":[\"DB1\",\"DB2\"],\n" +
                        "      \"C\":[\"DC1\",\"DC2\"],\n" +
                        "      \"D\":[\"DD1\",\"DD2\"]\n" +
                        "    },\n" +
                        "    \"outputFormat\": {\n" +
                        "      \"name\": \"defaultFormat\"\n" +
                        "   }," +
                        "  },\n" +
                        "  \"verifier\":\"None\"\n" +
                        "\n" +
                        "}";
        try {
            FileRun<AtomGoal, Scamp5DigitalTransformation<AtomGoal>, Register> fileRun = new Scamp5DigitalFileRun.AtomGoalFileRun(FileRun.fromJson(json, true));
            fileRun.run();
            List<Result<AtomGoal, Scamp5DigitalTransformation<AtomGoal>, Register>> results = fileRun.getResults();
//            System.out.println((fileRun.getBest()));
            OptionalInt min = results.stream().mapToInt(r -> r.cost).min();
            assertTrue(min.isPresent());
            assertTrue(min.getAsInt()<=140, "Got value: " +min.getAsInt() + " Expected: <= 140");
        } catch (Exception e){
            e.printStackTrace();
            e.getMessage();
            fail();
        }

    }
}