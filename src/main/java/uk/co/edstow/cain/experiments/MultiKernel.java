package uk.co.edstow.cain.experiments;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.co.edstow.cain.FileRun;
import uk.co.edstow.cain.RegisterAllocator;
import uk.co.edstow.cain.atom.AtomGoal;
import uk.co.edstow.cain.structures.GoalBag;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
        File f = new File("MultiKernel_02.csv");
        if(!f.createNewFile()){
            System.out.println("Cannot make new file!");
            System.exit(-1);
        }
        writer = new OutputStreamWriter(new FileOutputStream(f, true));
        writer.write("\"kernels\",\"id\",\"part\",\"cost\"\n");

        final RegisterAllocator.Register[] registers = RegisterAllocator.Register.getRegisters(1000);
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
                    allFinalGoals.add(new AtomGoal.Factory(makeRandom(r, 3, 0, 8, 0d)).get());
                }
                System.out.println(GoalBag.toGoalsString(allFinalGoals));

//                if (goalCount< 10) continue;
//                if(sampleId<9) continue;

                // Generate Individually
                for (int part = 1; part <= goalCount; part++) {
                    System.out.println("GoalCount: " + goalCount + " SampleID: " + sampleId + " Part: " + part);

//                    if(part<100) continue;

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


    private static int[][] makeRandom(Random r, int size, int min, int max, double sparsity){

        int[][] filter = new int[size][size];
        for (int i = 0; i < filter.length; i++) {
            for (int j = 0; j < filter[i].length; j++) {
                if(r.nextDouble() > sparsity){
                    filter[i][j] = r.nextInt((max+1)-min)+min;
                }
            }
        }
        return filter;
    }



}
