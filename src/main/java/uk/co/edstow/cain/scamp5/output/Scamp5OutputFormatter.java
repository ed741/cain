package uk.co.edstow.cain.scamp5.output;

import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.regAlloc.RegisterAllocator;

import java.util.List;

public interface Scamp5OutputFormatter {
  String NOR(String r1, String r2, String r3);

  String MOV(String r1, String r2);

  String CLR(String... rs);

  default String CLR(String r1){return CLR(new String[]{r1});}

  default String CLR(String r1, String r2){return CLR(new String[]{r1, r2});};

  default String CLR(String r1, String r2, String r3){return CLR(new String[]{r1, r2, r3});};

  default String CLR(String r1, String r2, String r3, String r4){return CLR(new String[]{r1, r2, r3, r4});};

  String SET(String r1);

  String SET(String r1, String r2);

  String NOT(String r1, String r2);

  String OR(String r1, String... rs);

  default String OR(String r1, String r2, String r3, String r4, String r5) {
    return OR(r1, new String[]{r2, r3, r4, r5});
  }

  default String OR(String r1, String r2, String r3, String r4) {
    return OR(r1, new String[]{r2, r3, r4});
  }

  default String OR(String r1, String r2, String r3) {
    return OR(r1, new String[]{r2, r3});
  }


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


  String load_pattern(String r1, byte r, byte c, byte rx, byte cx);

  String select_pattern(byte r, byte c, byte rx, byte cx);

  String kernel_begin();
  String kernel_end();

  default String comment(String comment){
    return "/*"+comment+"*/";
  }
  default String newLine(){
    return "\n";
  }

  String all();
  String ALL();
  String where(Register a);
  String WHERE(String x);

  String in(Register a, int value);
}
