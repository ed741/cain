package uk.co.edstow.cain.scamp5;

public class Scamp5DefaultOutputFormatter implements Scamp5OutputFormatter {
  @Override
  public String nor(String r1, String r2, String r3) {
    return String.format("NOR(%s, %s, %s); ", r1, r2, r3);
  }

  @Override
  public String mov(String r1, String r2) {
    return String.format("MOV(%s, %s); ", r1, r2);
  }

  @Override
  public String clr(String... regs) {
    // Regs must be non-empty
    StringBuilder sb = new StringBuilder("CLR(");
    for (int i = 0; i < regs.length - 1; i++) {
      sb.append(String.format("%s, ", regs[i]));
    }
    String lastReg = regs[regs.length - 1];
    sb.append(lastReg);
    sb.append("); ");

    return sb.toString();
  }

  @Override
  public String set(String r1) {
    return String.format("SET(%s); ", r1);
  }

  @Override
  public String dnews0(String r1, String r2) {
    return String.format("DNEWS0(%s, %s); ", r1, r2);
  }
}
