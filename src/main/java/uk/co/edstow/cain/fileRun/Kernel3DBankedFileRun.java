package uk.co.edstow.cain.fileRun;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.co.edstow.cain.goals.BankedKernel3DGoal;
import uk.co.edstow.cain.regAlloc.BRegister;
import uk.co.edstow.cain.regAlloc.BankedLinearScanRegisterAllocator;
import uk.co.edstow.cain.regAlloc.BankedRegisterAllocator;
import uk.co.edstow.cain.transformations.BankedTransformation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Kernel3DBankedFileRun<G extends BankedKernel3DGoal<G>, T extends BankedTransformation> extends Kernel3DFileRun<G, T, BRegister> {

    public Kernel3DBankedFileRun(JSONObject config) {
        super(config);
    }


    private List<BRegister> getRegisterArray(JSONArray availableRegisters) {
        ArrayList<BRegister> out = new ArrayList<>(availableRegisters.length());
        if (availableRegisters.length() > 0 && availableRegisters.get(0) instanceof JSONArray) {

            for (int i = 0; i < availableRegisters.length(); i++) {
                JSONArray bank = availableRegisters.getJSONArray(i);
                for (int j = 0; j < bank.length(); j++) {
                    out.add(new BRegister(i, bank.getString(j)));
                }
            }

        } else {
            for (int i = 0; i < availableRegisters.length(); i++) {
                String s = availableRegisters.getString(i);
                out.add(BRegister.makeBRegister(s));
            }
        }
        return out;
    }

    @Override
    protected List<BRegister> getOutputRegisters() {
        if (config.has("filter")) {
            JSONObject filter = config.getJSONObject("filter");
            return filter.keySet().stream().map((String bank) -> {
                String[] strs = bank.split(":");
                if (strs.length != 2) throw new IllegalArgumentException("using Banked Kernel3D goal requires that" +
                        " filter registers are specified as '0:A' where 0 is the bank and A is the virtual" +
                        " Register. '" + bank + "; does not conform");
                return new BRegister(Integer.parseInt(strs[0]), strs[1]);
            }).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    protected List<BRegister> getInputRegisters() {
        return getRegisterArray(config.getJSONObject("registerAllocator").getJSONArray("initialRegisters"));
    }

    protected abstract int getBanks();

    @Override
    protected BankedRegisterAllocator<G, T, BRegister> makeRegisterAllocator() {
        JSONObject regAllocConf = config.getJSONObject("registerAllocator");
        switch (regAllocConf.getString("name")) {
            case "linearScan":
                printLn("\tMaking Linear Scan Register Allocator:");
                List<BRegister> availableRegisters = new ArrayList<>(getRegisterArray(regAllocConf.getJSONArray("availableRegisters")));
                printLn("Available registers  : " + availableRegisters.toString());

                List<BRegister> available = new ArrayList<>(getOutputRegisters());
                for (BRegister availableRegister : availableRegisters) {
                    if (!available.contains(availableRegister)) {
                        available.add(availableRegister);
                    }
                }
                List<BRegister> initRegisters = new ArrayList<>(getInputRegisters());
                printLn("Initial registers    : " + initRegisters.toString());
                return new BankedLinearScanRegisterAllocator<>(getBanks(), initRegisters, initialGoals, available);
            default:
                throw new IllegalArgumentException("Register Allocator Unknown");
        }

    }


}
