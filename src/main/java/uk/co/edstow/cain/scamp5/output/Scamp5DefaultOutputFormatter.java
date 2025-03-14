package uk.co.edstow.cain.scamp5.output;

import uk.co.edstow.cain.regAlloc.Register;

public class Scamp5DefaultOutputFormatter implements Scamp5OutputFormatter {

  private final boolean refreshDNEWS;

  public Scamp5DefaultOutputFormatter(boolean refreshDNEWS) {
    this.refreshDNEWS = refreshDNEWS;
  }

  @Override
  public OutputCode NOR(String r1, String r2, String r3) {
    return new OutputCode().addOp(String.format("NOR(%s, %s, %s); ", r1, r2, r3));
  }

  @Override
  public OutputCode MOV(String r1, String r2) {
    return new OutputCode().addOp(String.format("MOV(%s, %s); ", r1, r2));
  }

  @Override
  public OutputCode CLR(String... rs) {
    assert rs.length > 0;
    assert rs.length < 5;
    StringBuilder sb = new StringBuilder("CLR(");
    for (int i = 0; i < rs.length; i++) {
      String r = rs[i];
      if (i != 0) sb.append(", ");
      sb.append(r);
    }
    sb.append("); ");
    return new OutputCode().addOp(sb.toString());
  }

  @Override
  public OutputCode SET(String r1) {
    return new OutputCode().addOp(String.format("SET(%s); ", r1));
  }

  @Override
  public OutputCode SET(String r1, String r2) {
    return new OutputCode().addOp(String.format("SET(%s, %s); ", r1, r2));
  }

  @Override
  public OutputCode NOT(String r1, String r2) {
    return new OutputCode().addOp(String.format("NOT(%s, %s); ", r1, r2));
  }

  @Override
  public OutputCode OR(String r1, String[] rs) {
    assert rs.length > 1;
    assert rs.length < 5;
    StringBuilder sb = new StringBuilder("OR(").append(r1);
    for (String r : rs) {
      sb.append(", ").append(r);
    }
    sb.append("); ");
    return new OutputCode().addOp(sb.toString());
  }

  @Override
  public OutputCode DNEWS0(String r1, String r2) {
    OutputCode op = new OutputCode().addOp(String.format("DNEWS0(%s, %s); ", r1, r2));
    if(refreshDNEWS){
      op.addOp(String.format("REFRESH(%s); ", r1));
    }
    return op;
  }

  @Override
  public OutputCode add2x(Register y, Register x0, Register x1, String dir1, String dir2) {
    return new OutputCode().addOp(String.format("add2x(%s, %s, %s, %s, %s); ", y, x0, x1, dir1, dir2));
  }

  @Override
  public OutputCode add(Register y, Register x0, Register x1) {
    return new OutputCode().addOp(String.format("add(%s, %s, %s); ", y, x0, x1));
  }

  @Override
  public OutputCode add(Register y, Register x0, Register x1, Register x2) {
    return new OutputCode().addOp(String.format("add(%s, %s, %s, %s); ", y, x0, x1, x2));
  }

  @Override
  public OutputCode addx(Register y, Register x0, Register x1, String dir) {
    return new OutputCode().addOp(String.format("addx(%s, %s, %s, %s); ", y, x0, x1, dir));
  }

  @Override
  public OutputCode diva(Register y0, Register y1, Register y2) {
    return new OutputCode().addOp(String.format("diva(%s, %s, %s); ", y0, y1, y2));
  }

  @Override
  public OutputCode div(Register y0, Register y1, Register y2) {
    return new OutputCode().addOp(String.format("div(%s, %s, %s); ", y0, y1, y2));
  }

  @Override
  public OutputCode div(Register y0, Register y1, Register y2, Register x0) {
    return new OutputCode().addOp(String.format("div(%s, %s, %s, %s); ", y0, y1, y2, x0));
  }

  @Override
  public OutputCode divq(Register y0, Register x0) {
    return new OutputCode().addOp(String.format("divq(%s, %s); ", y0, x0));
  }

  @Override
  public OutputCode mov(Register y, Register x0) {
    return new OutputCode().addOp(String.format("mov(%s, %s); ", y, x0));
  }

  @Override
  public OutputCode mov2x(Register y, Register x0, String dir1, String dir2) {
    return new OutputCode().addOp(String.format("mov2x(%s, %s, %s, %s); ", y, x0, dir1, dir2));
  }

  @Override
  public OutputCode movx(Register y, Register x0, String dir) {
    return new OutputCode().addOp(String.format("movx(%s, %s, %s); ", y, x0, dir));
  }

  @Override
  public OutputCode neg(Register y, Register x0) {
    return new OutputCode().addOp(String.format("neg(%s, %s); ", y, x0));
  }

  @Override
  public OutputCode res(Register a) {
    return new OutputCode().addOp(String.format("res(%s); ", a));
  }

  @Override
  public OutputCode res(Register a, Register b) {
    return new OutputCode().addOp(String.format("res(%s, %s); ", a, b));
  }

  @Override
  public OutputCode sub(Register y, Register x0, Register x1) {
    return new OutputCode().addOp(String.format("sub(%s, %s, %s); ", y, x0, x1));
  }

  @Override
  public OutputCode sub2x(Register y, Register x0, String dir1, String dir2, Register x1) {
    return new OutputCode().addOp(String.format("sub2x(%s, %s, %s, %s, %s); ", y, x0, dir1, dir2, x1));
  }

  @Override
  public OutputCode subx(Register y, Register x0, String dir1, Register x1) {
    return new OutputCode().addOp(String.format("subx(%s, %s, %s, %s); ", y, x0, dir1, x1));
  }

  @Override
  public OutputCode load_pattern(String r1, byte r, byte c, byte rx, byte cx) {
    return new OutputCode().addSelect(String.format("scamp5_load_pattern(%s, 0x%02x, 0x%02x, 0x%02x, 0x%02x); ", r1, r, c, rx, cx));
  }

  @Override
  public OutputCode select_pattern(byte r, byte c, byte rx, byte cx) {
    return new OutputCode().addSelect(String.format("scamp5_select_pattern(0x%02x, 0x%02x, 0x%02x, 0x%02x); ", r, c, rx, cx));
  }

  @Override
  public OutputCode kernel_begin() {
    return new OutputCode().addOther("scamp5_kernel_begin(); ");
  }

  @Override
  public OutputCode kernel_end() {
    return new OutputCode().addOther("scamp5_kernel_end(); ");
  }

  @Override
  public OutputCode all() {
    return new OutputCode().addOp("all(); ");
  }

  @Override
  public OutputCode ALL() {
    return new OutputCode().addOp("ALL(); ");
  }

  @Override
  public OutputCode where(Register a) {
    return new OutputCode().addOp(String.format("where(%1$s); ", a));
  }

  @Override
  public OutputCode WHERE(String x) {
    return new OutputCode().addOp(String.format("WHERE(%1$s); ", x));
  }

  @Override
  public OutputCode in(Register a, int value) {
    return new OutputCode().addOp(String.format("scamp5_in(%1$s, %2$s); ", a, value));
  }
}
