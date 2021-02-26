package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.RegisterAllocator;

import java.util.List;

public class Scamp5DefaultOutputFormatter implements Scamp5OutputFormatter {
  @Override
  public String nor(RegisterAllocator.Register r1, RegisterAllocator.Register r2, RegisterAllocator.Register r3) {
    return String.format("NOR(%s, %s, %s); ", r1, r2, r3);
  }

  @Override
  public String mov(RegisterAllocator.Register r1, RegisterAllocator.Register r2) {
    return String.format("MOV(%s, %s); ", r1, r2);
  }

  @Override
  public String clr(List<RegisterAllocator.Register> regs) {
    // Regs must be non-empty
    StringBuilder sb = new StringBuilder("CLR(");
    for (int i = 0; i < regs.size(); i++) {
      sb.append(String.format("%s, ", regs.get(i)));
    }
    RegisterAllocator.Register lastReg = regs.get(regs.size() - 1);
    sb.append(lastReg);
    sb.append("); ");

    return sb.toString();
  }

  @Override
  public String set(RegisterAllocator.Register r1) {
    return String.format("SET(%s); ", r1);
  }

  @Override
  public String dnews0(RegisterAllocator.Register r1) {
    return String.format("DNEWS0(%s, %s); ", r1);
  }
}
