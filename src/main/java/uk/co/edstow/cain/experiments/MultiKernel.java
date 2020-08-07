package uk.co.edstow.cain.experiments;

import org.json.JSONObject;
import uk.co.edstow.cain.FileRun;
import uk.co.edstow.cain.atom.AtomGoal;
import uk.co.edstow.cain.structures.GoalBag;
import uk.co.edstow.cain.util.RandomKernel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MultiKernel {

    private static final String BaseJson =
            "{\"name\":\"MultiKernelTest\",\n" +
                    "  \"verbose\":1,\n" +
                    "  \"goalSystem\":Atom,\n" +
                    "\n" +
                    "  \"availableRegisters\":[\"A\",\"B\",\"C\",\"D\",\"E\",\"F\"],\n" +
                    "  \"initialRegisters\":[\"A\"],\n" +
                    "\n" +
                    "  \"runConfig\":{\n" +
                    "    \"searchTime\":60000,\n" +
                    "    \"timeOut\":true,\n" +
                    "    \"workers\":4,\n" +
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
                    "    \"threshold\":0\n" +
                    "  },\n" +
                    "  \"verifier\":\"Scamp5Emulator\"\n" +
                    "\n" +
                    "}";

    private static OutputStreamWriter writer;
    public static void main(String[] args) throws IOException {
        File f = new File("MultiKernel_00.csv");
        if(!f.createNewFile()){
            System.out.println("Cannot make new file!");
            System.exit(-1);
        }
        writer = new OutputStreamWriter(new FileOutputStream(f, true));
        writer.write("\"kernels\",\"id\",\"cost\"\n");

        final int samples = 25;

        final Random r = new Random(1000);
        final int divisions = 3;

        for (int goalCount = 1; goalCount <=4; goalCount++) {
            System.out.println("GoalCount: "+goalCount);
            for (int sampleId = 1; sampleId <= samples; sampleId++) {
                System.out.println("Sample: "+sampleId);

                // Generate Random Goals
                List<AtomGoal> allFinalGoals = new ArrayList<>();
                for (int j = 0; j < goalCount; j++) {
                    allFinalGoals.add(new AtomGoal.Factory(RandomKernel.makeRandom(r, 3, 0, 8, 0d)).get());
                }
                System.out.println(GoalBag.toGoalsString(allFinalGoals));

                // generate simultaneously
                System.out.println("GoalCount: "+goalCount+ " SampleID: " + sampleId + " AllParts");
                JSONObject conf = FileRun.fromJson(BaseJson, true);

                run(goalCount, sampleId, conf, allFinalGoals, divisions);



            }
        }



    }

    private static void run(int kernels, int sampleId, JSONObject conf, List<AtomGoal> finalGoals, int divisions){
        System.out.println("Running: ks:"+kernels+":"+sampleId);
        System.out.println(GoalBag.toGoalsString(finalGoals));


        FileRun.Scamp5AtomFileRun fileRun = new FileRun.Scamp5AtomFileRun(conf, finalGoals, divisions);
        fileRun.run();
        List<? extends FileRun.Scamp5AtomFileRun.Result> results = fileRun.getResults();
        int minCost = Integer.MAX_VALUE;
        for (int i = 0; i < results.size(); i++) {
            if(results.get(i).cost < minCost){
                minCost = results.get(i).cost;
            }
        }
        String output = kernels+","+sampleId+","+minCost+"\n";
        try {
            writer.write(output);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error writing: \""+output+"\"");
            System.exit(-1);
        }


    }


}
