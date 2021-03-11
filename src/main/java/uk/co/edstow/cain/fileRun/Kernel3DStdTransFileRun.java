package uk.co.edstow.cain.fileRun;

import org.json.JSONArray;
import org.json.JSONObject;
import uk.co.edstow.cain.goals.Kernel3DGoal;
import uk.co.edstow.cain.regAlloc.LinearScanRegisterAllocator;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.regAlloc.RegisterAllocator;
import uk.co.edstow.cain.transformations.StandardTransformation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Kernel3DStdTransFileRun<G extends Kernel3DGoal<G>, T extends StandardTransformation> extends Kernel3DFileRun<G, T, Register> {
    public Kernel3DStdTransFileRun(JSONObject config) {
        super(config);
    }

    private List<Register> getRegisterArray(JSONArray availableRegisters) {
        ArrayList<Register> out = new ArrayList<>(availableRegisters.length());
        for (int i = 0; i < availableRegisters.length(); i++) {
            out.add(new Register(availableRegisters.getString(i)));
        }
        return out;
    }

    @Override
    protected List<Register> getInputRegisters() {
        return getRegisterArray(config.getJSONObject("registerAllocator").getJSONArray("initialRegisters"));
    }

    @Override
    protected List<Register> getOutputRegisters() {
        if (config.has("filter")) {
            JSONObject filter = config.getJSONObject("filter");
            return filter.keySet().stream().map(Register::new).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    protected RegisterAllocator<G, T, Register> makeRegisterAllocator() {
        JSONObject regAllocConf = config.getJSONObject("registerAllocator");
        switch (regAllocConf.getString("name")) {
            case "linearScan":
                printLn("\tMaking Linear Scan Register Allocator:");
                List<Register> availableRegisters = new ArrayList<>();
            {
                JSONArray regArray = config.getJSONObject("registerAllocator").getJSONArray("availableRegisters");
                for (int i = 0; i < regArray.length(); i++) {
                    availableRegisters.add(new Register(regArray.getString(i)));
                }
            }
            printLn("Available registers  : " + availableRegisters.toString());

            List<Register> available = new ArrayList<>(getOutputRegisters());
            for (Register availableRegister : availableRegisters) {
                if (!available.contains(availableRegister)) {
                    available.add(availableRegister);
                }
            }
            List<Register> initRegisters = new ArrayList<>(getInputRegisters());
            printLn("Initial registers    : " + initRegisters.toString());
            return new LinearScanRegisterAllocator<>(initRegisters, initialGoals, available);
            default:
                throw new IllegalArgumentException("Register Allocator Unknown");
        }

    }

}
