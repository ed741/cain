package uk.co.edstow.cain.scamp5;

public class Scamp5JssOutputFormatter implements Scamp5OutputFormatter {
  public final String simulatorName;

  public Scamp5JssOutputFormatter(String simulatorName) {
    this.simulatorName = simulatorName;
  }

  @Override
  public String NOR(String r1, String r2, String r3) {
    return String.format("%1$s.NOR(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s); ", simulatorName, r1, r2, r3);
  }


  @Override
  public String MOV(String r1, String r2) {
    return String.format("%1$s.NOR(%1$s.%2$s, %1$s.%3$s); ", simulatorName, r1, r2);
  }

  @Override
  public String CLR(String r1) {
    return String.format("%1$s.CLR(%1$s.%2$s); ", simulatorName, r1);
  }

  @Override
  public String CLR(String r1, String r2) {
    return String.format("%1$s.CLR(%1$s.%2$s, %1$s.%3$s); ", simulatorName, r1, r2);
  }

  @Override
  public String CLR(String r1, String r2, String r3) {
    return String.format("%1$s.CLR(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s); ", simulatorName, r1, r2, r3);
  }

  @Override
  public String CLR(String r1, String r2, String r3, String r4) {
    return String.format("%1$s.CLR(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s, %1$s.%5$s); ", simulatorName, r1, r2, r3, r4);
  }

  @Override
  public String SET(String r1) {
    return String.format("%1$s.SET(%1$s.%2$s); ", simulatorName, r1);
  }

  @Override
  public String DNEWS0(String r1, String r2) {
    return String.format("%1$s.DNEWS0(%1$s.%2$s, %1$s.%3$s); ", simulatorName, r1, r2);
  }
}
