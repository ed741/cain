package uk.co.edstow.cain.experiments;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.co.edstow.cain.FileRun;
import uk.co.edstow.cain.RegisterAllocator;
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
        writer.write("\"kernels\",\"id\",\"part\",\"cost\"\n");

        final RegisterAllocator.Register[] registers = RegisterAllocator.Register.getRegisters(18);
        final int samples = 10;

        final Random r = new Random(900);
        final int divisions = 0;

        for (int goalCount = 1; goalCount <=10; goalCount++) {
            System.out.println("GoalCount: "+goalCount);
            for (int sampleId = 1; sampleId <= samples; sampleId++) {
                System.out.println("Sample: "+sampleId);

                // Generate Random Goals
                List<AtomGoal> allFinalGoals = new ArrayList<>();
                for (int j = 0; j < goalCount; j++) {
                    allFinalGoals.add(new AtomGoal.Factory(RandomKernel.makeRandom(r, 3, 0, 8, 0d)).get());
                }
                System.out.println(GoalBag.toGoalsString(allFinalGoals));


                // Generate Individually
                for (int part = 1; part <= goalCount; part++) {
                    System.out.println("GoalCount: " + goalCount + " SampleID: " + sampleId + " Part: " + part);

                    // get current final goal
                    List<AtomGoal> finalGoals = allFinalGoals.subList(part - 1, part);

                    // Load config
                    JSONObject conf = FileRun.fromJson(BaseJson, true);

                    // configure Registers
                    conf.put("availableRegisters", new JSONArray());
                    if (part == goalCount) {
                        // the last goal can be put into 'A'
                        conf.getJSONArray("availableRegisters").put("A");
                    }

                    for (int i = part; i < registers.length; i++) {
                        RegisterAllocator.Register reg = registers[i];
                        conf.getJSONArray("availableRegisters").put(reg.name);
                    }
                    run(goalCount, sampleId, part, conf, finalGoals, divisions);
                }
                // generate simultaneously
                int part = 0;
                System.out.println("GoalCount: "+goalCount+ " SampleID: " + sampleId + " AllParts");
                JSONObject conf = FileRun.fromJson(BaseJson, true);
                conf.put("availableRegisters", new JSONArray());
                for (RegisterAllocator.Register reg : registers) {
                    conf.getJSONArray("availableRegisters").put(reg.name);
                }

                conf.getJSONObject("runConfig").put("searchTime",60000);

                run(goalCount, sampleId, part, conf, allFinalGoals, divisions);



            }
        }



    }

    private static void run(int kernels, int sampleId, int part, JSONObject conf, List<AtomGoal> finalGoals, int divisions){
        System.out.println("Running: ksp:"+kernels+":"+sampleId+":"+part);
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
        String output = kernels+","+sampleId+","+part+","+minCost+"\n";
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
