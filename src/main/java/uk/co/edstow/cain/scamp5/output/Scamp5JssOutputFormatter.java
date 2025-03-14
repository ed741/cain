package uk.co.edstow.cain.scamp5.output;

import uk.co.edstow.cain.regAlloc.Register;

public class Scamp5JssOutputFormatter implements Scamp5OutputFormatter {
  public final String simulatorName;

  public Scamp5JssOutputFormatter(String simulatorName) {
    this.simulatorName = simulatorName;
  }

  @Override
  public OutputCode NOR(String r1, String r2, String r3) {
    return new OutputCode().addOp(String.format("%1$s.NOR(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s); ", simulatorName, r1, r2, r3));
  }


  @Override
  public OutputCode MOV(String r1, String r2) {
    return new OutputCode().addOp(String.format("%1$s.MOV(%1$s.%2$s, %1$s.%3$s); ", simulatorName, r1, r2));
  }

  @Override
  public OutputCode CLR(String... rs) {
    assert rs.length > 0;
    assert rs.length < 5;
    StringBuilder sb = new StringBuilder(simulatorName).append(".CLR(");
    for (int i = 0; i < rs.length; i++) {
      String r = rs[i];
      if (i != 0) sb.append(", ");
      sb.append(simulatorName).append(".").append(r);
    }
    sb.append("); ");
    return new OutputCode().addOp(sb.toString());
  }

  @Override
  public OutputCode CLR(String r1) {
    return new OutputCode().addOp(String.format("%1$s.CLR(%1$s.%2$s); ", simulatorName, r1));
  }

  @Override
  public OutputCode CLR(String r1, String r2) {
    return new OutputCode().addOp(String.format("%1$s.CLR(%1$s.%2$s, %1$s.%3$s); ", simulatorName, r1, r2));
  }

  @Override
  public OutputCode CLR(String r1, String r2, String r3) {
    return new OutputCode().addOp(String.format("%1$s.CLR(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s); ", simulatorName, r1, r2, r3));
  }

  @Override
  public OutputCode CLR(String r1, String r2, String r3, String r4) {
    return new OutputCode().addOp(String.format("%1$s.CLR(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s, %1$s.%5$s); ", simulatorName, r1, r2, r3, r4));
  }

  @Override
  public OutputCode SET(String r1) {
    return new OutputCode().addOp(String.format("%1$s.SET(%1$s.%2$s); ", simulatorName, r1));
  }

  @Override
  public OutputCode SET(String r1, String r2) {
    return new OutputCode().addOp(String.format("%1$s.SET(%1$s.%2$s, %1$s.%3$s); ", simulatorName, r1, r2));
  }

  @Override
  public OutputCode NOT(String r1, String r2) {
    return new OutputCode().addOp(String.format("%1$s.NOT(%1$s.%2$s, %1$s.%3$s); ", simulatorName, r1, r2));
  }

  @Override
  public OutputCode OR(String r1, String[] rs) {
    assert rs.length > 1;
    assert rs.length < 5;
    StringBuilder sb = new StringBuilder(String.format("%1$s.OR(%1$s.%2$s", simulatorName, r1));
    for (String r : rs) {
      sb.append(", ").append(simulatorName).append(".").append(r);
    }
    sb.append("); ");
    return new OutputCode().addOp(sb.toString());
  }


  @Override
  public OutputCode DNEWS0(String r1, String r2) {
    return new OutputCode().addOp(String.format("%1$s.DNEWS0(%1$s.%2$s, %1$s.%3$s); ", simulatorName, r1, r2));
  }

  @Override
  public OutputCode add2x(Register y, Register x0, Register x1, String dir1, String dir2) {
    return new OutputCode().addOp(String.format("%1$s.add2x(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s, %5$s, %6$s); ", simulatorName, y, x0, x1, dir1, dir2));
  }

  @Override
  public OutputCode add(Register y, Register x0, Register x1) {
    return new OutputCode().addOp(String.format("%1$s.add(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s); ", simulatorName, y, x0, x1));
  }

  @Override
  public OutputCode add(Register y, Register x0, Register x1, Register x2) {
    return new OutputCode().addOp(String.format("%1$s.add(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s, %1$s.%5$s); ", simulatorName, y, x0, x1, x2));
  }

  @Override
  public OutputCode addx(Register y, Register x0, Register x1, String dir) {
    return new OutputCode().addOp(String.format("%1$s.addx(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s, %5$s); ", simulatorName, y, x0, x1, dir));
  }

  @Override
  public OutputCode diva(Register y0, Register y1, Register y2) {
    return new OutputCode().addOp(String.format("%1$s.diva(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s); ", simulatorName, y0, y1, y2));
  }

  @Override
  public OutputCode div(Register y0, Register y1, Register y2) {
    return new OutputCode().addOp(String.format("%1$s.div(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s); ", simulatorName, y0, y1, y2));
  }

  @Override
  public OutputCode div(Register y0, Register y1, Register y2, Register x0) {
    return new OutputCode().addOp(String.format("%1$s.div(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s, %1$s.%5$s); ", simulatorName, y0, y1, y2, x0));
  }

  @Override
  public OutputCode divq(Register y0, Register x0) {
    return new OutputCode().addOp(String.format("%1$s.divq(%1$s.%2$s, %1$s.%3$s); ", simulatorName, y0, x0));
  }

  @Override
  public OutputCode mov(Register y, Register x0) {
    return new OutputCode().addOp(String.format("%1$s.mov(%1$s.%2$s, %1$s.%3$s); ", simulatorName, y, x0));
  }

  @Override
  public OutputCode mov2x(Register y, Register x0, String dir1, String dir2) {
    return new OutputCode().addOp(String.format("%1$s.mov2x(%1$s.%2$s, %1$s.%3$s, %4$s, %5$s); ", simulatorName, y, x0, dir1, dir2));
  }

  @Override
  public OutputCode movx(Register y, Register x0, String dir) {
    return new OutputCode().addOp(String.format("%1$s.movx(%1$s.%2$s, %1$s.%3$s, %4$s); ", simulatorName, y, x0, dir));
  }

  @Override
  public OutputCode neg(Register y, Register x0) {
    return new OutputCode().addOp(String.format("%1$s.neg(%1$s.%2$s, %1$s.%3$s); ", simulatorName, y, x0));
  }

  @Override
  public OutputCode res(Register a) {
    return new OutputCode().addOp(String.format("%1$s.res(%1$s.%2$s); ", simulatorName, a));
  }

  @Override
  public OutputCode res(Register a, Register b) {
    return new OutputCode().addOp(String.format("%1$s.res(%1$s.%2$s, %1$s.%3$s); ", simulatorName, a, b));
  }

  @Override
  public OutputCode sub(Register y, Register x0, Register x1) {
    return new OutputCode().addOp(String.format("%1$s.sub(%1$s.%2$s, %1$s.%3$s, %1$s.%4$s); ", simulatorName, y, x0, x1));
  }

  @Override
  public OutputCode sub2x(Register y, Register x0, String dir1, String dir2, Register x1) {
    return new OutputCode().addOp(String.format("%1$s.sub2x(%1$s.%2$s, %1$s.%3$s, %4$s, %5$s, %1$s.%6$s); ", simulatorName, y, x0, dir1, dir2, x1));
  }

  @Override
  public OutputCode subx(Register y, Register x0, String dir1, Register x1) {
    return new OutputCode().addOp(String.format("%1$s.subx(%1$s.%2$s, %1$s.%3$s, %4$s, %1$s.%5$s); ", simulatorName, y, x0, dir1, x1));
  }


  @Override
  public OutputCode load_pattern(String r1, byte r, byte c, byte rx, byte cx) {
    return new OutputCode().addSelect(String.format("%1$s.scamp5_load_pattern(%1$s.%2$s, 0x%3$02x, 0x%4$02x, 0x%5$02x, 0x%6$02x); ", simulatorName, r1, r, c, rx, cx));
  }

  @Override
  public OutputCode select_pattern(byte r, byte c, byte rx, byte cx) {
    return new OutputCode().addSelect(String.format("%1$s.scamp5_select_pattern(0x%2$02x, 0x%3$02x, 0x%4$02x, 0x%5$02x); ", simulatorName, r, c, rx, cx));
  }

  @Override
  public OutputCode kernel_begin() {
    return new OutputCode().addOther("/*Kernel Begin*/");
  }

  @Override
  public OutputCode kernel_end() {
    return new OutputCode().addOther("/*Kernel End*/");
  }

  @Override
  public OutputCode all() {
    return new OutputCode().addOp(String.format("%1$s.all(); ", simulatorName));
  }

  @Override
  public OutputCode ALL() {
    return new OutputCode().addOp(String.format("%1$s.ALL(); ", simulatorName));
  }

  @Override
  public OutputCode where(Register a) {
    return new OutputCode().addOp(String.format("%1$s.where(%1$s.%2$s); ", simulatorName, a));
  }

  @Override
  public OutputCode WHERE(String x) {
    return new OutputCode().addOp(String.format("%1$s.WHERE(%1$s.%2$s); ", simulatorName, x));
  }

  @Override
  public OutputCode in(Register a, int value) {
    return new OutputCode().addOp(String.format("%1$s.scamp5_in(%1$s.%2$s, %3$s); ", simulatorName, a, value));
  }
}
