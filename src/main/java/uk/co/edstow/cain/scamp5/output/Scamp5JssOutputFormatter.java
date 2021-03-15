package uk.co.edstow.cain.scamp5.output;

import uk.co.edstow.cain.regAlloc.Register;

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
  public String CLR(String... rs) {
    assert rs.length > 0;
    assert rs.length < 5;
    StringBuilder sb = new StringBuilder(simulatorName).append(".CLR(");
    for (int i = 0; i < rs.length; i++) {
      String r = rs[i];
      if (i != 0) sb.append(", ");
      sb.append(simulatorName).append(".").append(r);
    }
    sb.append("); ");
    return sb.toString();
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
  public String SET(String r1, String r2) {
    return String.format("%1$s.SET(%1$s.%2$s, %1$s.%3$s); ", simulatorName, r1, r2);
  }

  @Override
  public String NOT(String r1, String r2) {
    return String.format("%1$s.NOT(%1$s.%2$s, %1$s.%3$s); ", simulatorName, r1, r2);
  }

  @Override
  public String OR(String r1, String[] rs) {
    assert rs.length > 1;
    assert rs.length < 5;
    StringBuilder sb = new StringBuilder(String.format("%1$s.OR(%1$s.%2$s", simulatorName, r1));
    for (String r : rs) {
      sb.append(", ").append(simulatorName).append(".").append(r);
    }
    sb.append("); ");
    return sb.toString();
  }


  @Override
  public String DNEWS0(String r1, String r2) {
    return String.format("%1$s.DNEWS0(%1$s.%2$s, %1$s.%3$s); ", simulatorName, r1, r2);
  }

  @Override
  public String add2x(Register y, Register x0, Register x1, String dir1, String dir2) {
    return String.format("%1$s.add2x(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s, %5$s, %6$s); ", simulatorName, y, x0, x1, dir1, dir2);
  }

  @Override
  public String add(Register y, Register x0, Register x1) {
    return String.format("%1$s.add(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s); ", simulatorName, y, x0, x1);
  }

  @Override
  public String add(Register y, Register x0, Register x1, Register x2) {
    return String.format("%1$s.add(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s, %1$s.%5$s); ", simulatorName, y, x0, x1, x2);
  }

  @Override
  public String addx(Register y, Register x0, Register x1, String dir) {
    return String.format("%1$s.addx(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s, %5$s); ", simulatorName, y, x0, x1, dir);
  }

  @Override
  public String diva(Register y0, Register y1, Register y2) {
    return String.format("%1$s.diva(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s); ", simulatorName, y0, y1, y2);
  }

  @Override
  public String div(Register y0, Register y1, Register y2) {
    return String.format("%1$s.div(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s); ", simulatorName, y0, y1, y2);
  }

  @Override
  public String div(Register y0, Register y1, Register y2, Register x0) {
    return String.format("%1$s.div(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s, %1$s.%5$s); ", simulatorName, y0, y1, y2, x0);
  }

  @Override
  public String divq(Register y0, Register x0) {
    return String.format("%1$s.divq(%1$s.%2$s, %1$s.%3$s); ", simulatorName, y0, x0);
  }

  @Override
  public String mov(Register y, Register x0) {
    return String.format("%1$s.mov(%1$s.%2$s, %1$s.%3$s); ", simulatorName, y, x0);
  }

  @Override
  public String mov2x(Register y, Register x0, String dir1, String dir2) {
    return String.format("%1$s.mov2x(%1$s.%2$s, %1$s.%3$s, %4$s, %5$s); ", simulatorName, y, x0, dir1, dir2);
  }

  @Override
  public String movx(Register y, Register x0, String dir) {
    return String.format("%1$s.movx(%1$s.%2$s, %1$s.%3$s, %4$s); ", simulatorName, y, x0, dir);
  }

  @Override
  public String neg(Register y, Register x0) {
    return String.format("%1$s.neg(%1$s.%2$s, %1$s.%3$s); ", simulatorName, y, x0);
  }

  @Override
  public String res(Register a) {
    return String.format("%1$s.res(%1$s.%2$s); ", simulatorName, a);
  }

  @Override
  public String res(Register a, Register b) {
    return String.format("%1$s.res(%1$s.%2$s, %1$s.%3$s); ", simulatorName, a, b);
  }

  @Override
  public String sub(Register y, Register x0, Register x1) {
    return String.format("%1$s.sub(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s); ", simulatorName, y, x0, x1);
  }

  @Override
  public String sub2x(Register y, Register x0, String dir1, String dir2, Register x1) {
    return String.format("%1$s.sub2x(%1$s.%2$s, %1$s.%3$s, %4$s, %5$s, %1$s.%6$s); ", simulatorName, y, x0, dir1, dir2, x1);
  }

  @Override
  public String subx(Register y, Register x0, String dir1, Register x1) {
    return String.format("%1$s.subx(%1$s.%2$s, %1$s.%3$s, %4$s, %1$s.%5$s); ", simulatorName, y, x0, dir1, x1);
  }


  @Override
  public String load_pattern(String r1, byte r, byte c, byte rx, byte cx) {
    return String.format("%1$s.load_pattern(%1$s.%2$s, 0x%3$02x, 0x%4$02x, 0x%5$02x, 0x%6$02x); ", simulatorName, r1, r, c, rx, cx);
  }

  @Override
  public String select_pattern(byte r, byte c, byte rx, byte cx) {
    return String.format("%1$s.load_pattern(0x%2$02x, 0x%3$02x, 0x%4$02x, 0x%5$02x); ", simulatorName, r, c, rx, cx);
  }
}
