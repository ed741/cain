package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.regAlloc.Register;

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

  @Override
  public String add2x(Register y, Register x0, Register x1, String dir1, String dir2) {
    return String.format("add2x(%s, %s, %s, %s, %s);", y, x0, x1, dir1, dir2);
  }

  @Override
  public String add(Register y, Register x0, Register x1) {
    return String.format("add(%s, %s, %s);", y, x0, x1);
  }

  @Override
  public String add(Register y, Register x0, Register x1, Register x2) {
    return String.format("add(%s, %s, %s, %s);", y, x0, x1, x2);
  }

  @Override
  public String addx(Register y, Register x0, Register x1, String dir) {
    return String.format("addx(%s, %s, %s, %s);", y, x0, x1, dir);
  }

  @Override
  public String diva(Register y0, Register y1, Register y2) {
    return String.format("diva(%s, %s, %s);", y0, y1, y2);
  }

  @Override
  public String div(Register y0, Register y1, Register y2) {
    return String.format("div(%s, %s, %s);", y0, y1, y2);
  }

  @Override
  public String div(Register y0, Register y1, Register y2, Register x0) {
    return String.format("div(%s, %s, %s, %s);", y0, y1, y2, x0);
  }

  @Override
  public String divq(Register y0, Register x0) {
    return String.format("divq(%s, %s);", y0, x0);
  }

  @Override
  public String mov(Register y, Register x0) {
    return String.format("mov(%s, %s);", y, x0);
  }

  @Override
  public String mov2x(Register y, Register x0, String dir1, String dir2) {
    return String.format("mov2x(%s, %s, %s, %s);", y, x0, dir1, dir2);
  }

  @Override
  public String movx(Register y, Register x0, String dir) {
    return String.format("movx(%s, %s, %s);", y, x0, dir);
  }

  @Override
  public String neg(Register y, Register x0) {
    return String.format("neg(%s, %s);", y, x0);
  }

  @Override
  public String res(Register a) {
    return String.format("res(%s);", a);
  }

  @Override
  public String res(Register a, Register b) {
    return String.format("res(%s, %s);", a, b);
  }

  @Override
  public String sub(Register y, Register x0, Register x1) {
    return String.format("sub(%s, %s, %s);", y, x0, x1);
  }

  @Override
  public String sub2x(Register y, Register x0, String dir1, String dir2, Register x1) {
    return String.format("sub2x(%s, %s, %s, %s, %s);", y, x0, dir1, dir2, x1);
  }

  @Override
  public String subx(Register y, Register x0, String dir1, Register x1) {
    return String.format("subx(%s, %s, %s, %s);", y, x0, dir1, x1);
  }
}
