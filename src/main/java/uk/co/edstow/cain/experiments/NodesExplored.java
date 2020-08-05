package uk.co.edstow.cain.experiments;

import org.json.JSONObject;
import uk.co.edstow.cain.FileRun;
import uk.co.edstow.cain.atom.AtomGoal;
import uk.co.edstow.cain.scamp5.Scamp5Config;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.util.RandomKernel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class NodesExplored {

    private static final String BaseJson =
            "{\"name\":\"NodesExploredTest\",\n" +
                    "  \"verbose\":1,\n" +
                    "  \"goalSystem\":Atom,\n" +
                    "\n" +
                    "  \"availableRegisters\":[\"A\",\"B\",\"C\",\"D\",\"E\",\"F\"],\n" +
                    "  \"initialRegisters\":[\"A\"],\n" +
                    "\n" +
                    "  \"runConfig\":{\n" +
                    "    \"searchTime\":60000,\n" +
                    "    \"timeOut\":true,\n" +
                    "    \"workers\":1,\n" +
                    "    \"traversalAlgorithm\":\"SOT\",\n" +
                    "    \"costFunction\":\"PlanLength\",\n" +
                    "    \"liveCounter\":true,\n" +
                    "    \"quiet\":false,\n" +
                    "    \"livePrintPlans\":1,\n" +
                    "    \"initialMaxDepth\":1000,\n" +
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

    private static OutputStreamWriter writer;
    public static void main(String[] args) throws IOException {
        File f = new File("NodesExplored_00.csv");
        if(!f.createNewFile()){
            System.out.println("Cannot make new file!");
            System.exit(-1);
        }
        writer = new OutputStreamWriter(new FileOutputStream(f, true));
        writer.write("\"id\",\"length\",\"time\",\"nodes\"\n");

        final int samples = 100;

        final Random r = new Random(100);
        final int divisions = 0;

        for (int sampleId = 1; sampleId <= samples; sampleId++) {
            System.out.println("Sample: "+sampleId);

            // Generate Random Goals
            AtomGoal finalGoal = new AtomGoal.Factory(RandomKernel.makeRandom(r, 3, 0, 8, 0d)).get();
            System.out.println(finalGoal.getTableString(false, false, true, true));
            JSONObject conf = FileRun.fromJson(BaseJson, true);

            run(sampleId, conf, Collections.singletonList(finalGoal), divisions);

        }

    }

    private static void run(int sampleId, JSONObject conf, List<AtomGoal> finalGoals, int divisions){
        System.out.println("Running sample: "+sampleId);
        System.out.println(GoalBag.toGoalsString(finalGoals));


        FileRun.Scamp5AtomFileRun fileRun = new FileRun.Scamp5AtomFileRun(conf, finalGoals, divisions);
        fileRun.run();
        List<? extends FileRun.Scamp5AtomFileRun.Result> results = fileRun.getResults();
        for (FileRun<AtomGoal, Scamp5Config<AtomGoal>>.Result result : results) {
            String output = sampleId + "," + result.depth + "," + result.time + "," + result.nodesExpanded + "\n";
            try {
                writer.write(output);

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error writing: \"" + output + "\"");
                System.exit(-1);
            }
        }
        try {
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error flushing");
            System.exit(-1);
        }


    }


}
