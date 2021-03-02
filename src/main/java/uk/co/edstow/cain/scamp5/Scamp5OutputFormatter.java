package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.regAlloc.RegisterAllocator;

import java.util.List;

public interface Scamp5OutputFormatter {
  String NOR(String r1, String r2, String r3);

  String MOV(String r1, String r2);

  String CLR(String r1);

  String CLR(String r1, String r2);

  String CLR(String r1, String r2, String r3);

  String CLR(String r1, String r2, String r3, String r4);

  String SET(String r1);

  String DNEWS0(String r1, String r2);


  String add2x(Register y, Register x0, Register x1, String dir1, String dir2);

  String add(Register y, Register x0, Register x1);

  String add(Register y, Register x0, Register x1, Register x2);

  String addx(Register y, Register x0, Register x1, String dir);

  String diva(Register y0, Register y1, Register y2);

  String div(Register y0, Register y1, Register y2);

  String div(Register y0, Register y1, Register y2, Register x0);

  String divq(Register y0, Register x0);

  String mov(Register y, Register x0);

  String mov2x(Register y, Register x0, String dir1, String dir2);

  String movx(Register y, Register x0, String dir);

  String neg(Register y, Register x0);

  String res(Register a);

  String res(Register a, Register b);

  String sub(Register y, Register x0, Register x1);

  String sub2x(Register y, Register x0, String dir1, String dir2, Register x1);

  String subx(Register y, Register x0, String dir1, Register x1);
}
