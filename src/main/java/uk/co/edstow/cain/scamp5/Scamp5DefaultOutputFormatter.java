package uk.co.edstow.cain.scamp5;

public class Scamp5DefaultOutputFormatter implements Scamp5OutputFormatter {
  @Override
  public String NOR(String r1, String r2, String r3) {
    return String.format("NOR(%s, %s, %s); ", r1, r2, r3);
  }

  @Override
  public String MOV(String r1, String r2) {
    return String.format("MOV(%s, %s); ", r1, r2);
  }

  @Override
  public String CLR(String r1) {
    return String.format("CLR(%s); ", r1);
  }

  @Override
  public String CLR(String r1, String r2) {
    return String.format("CLR(%s, %s); ", r1, r2);
  }

  @Override
  public String CLR(String r1, String r2, String r3) {
    return String.format("CLR(%s, %s, %s); ", r1, r2, r3);
  }

  @Override
  public String CLR(String r1, String r2, String r3, String r4) {
    return String.format("CLR(%s, %s, %s, %s); ", r1, r2, r3, r4);
  }

  @Override
  public String SET(String r1) {
    return String.format("SET(%s); ", r1);
  }

  @Override
  public String DNEWS0(String r1, String r2) {
    return String.format("DNEWS0(%s, %s); ", r1, r2);
  }
}
