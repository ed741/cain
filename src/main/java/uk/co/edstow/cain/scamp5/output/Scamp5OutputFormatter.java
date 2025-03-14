package uk.co.edstow.cain.scamp5.output;

import uk.co.edstow.cain.regAlloc.Register;

public interface Scamp5OutputFormatter {
  OutputCode NOR(String r1, String r2, String r3);

  OutputCode MOV(String r1, String r2);

  OutputCode CLR(String... rs);

  default OutputCode CLR(String r1){return CLR(new String[]{r1});}

  default OutputCode CLR(String r1, String r2){return CLR(new String[]{r1, r2});}

    default OutputCode CLR(String r1, String r2, String r3){return CLR(new String[]{r1, r2, r3});}

    default OutputCode CLR(String r1, String r2, String r3, String r4){return CLR(new String[]{r1, r2, r3, r4});}

    OutputCode SET(String r1);

  OutputCode SET(String r1, String r2);

  OutputCode NOT(String r1, String r2);

  OutputCode OR(String r1, String... rs);

  default OutputCode OR(String r1, String r2, String r3, String r4, String r5) {
    return OR(r1, new String[]{r2, r3, r4, r5});
  }

  default OutputCode OR(String r1, String r2, String r3, String r4) {
    return OR(r1, new String[]{r2, r3, r4});
  }

  default OutputCode OR(String r1, String r2, String r3) {
    return OR(r1, new String[]{r2, r3});
  }


  OutputCode DNEWS0(String r1, String r2);


  OutputCode add2x(Register y, Register x0, Register x1, String dir1, String dir2);

  OutputCode add(Register y, Register x0, Register x1);

  OutputCode add(Register y, Register x0, Register x1, Register x2);

  OutputCode addx(Register y, Register x0, Register x1, String dir);

  OutputCode diva(Register y0, Register y1, Register y2);

  OutputCode div(Register y0, Register y1, Register y2);

  OutputCode div(Register y0, Register y1, Register y2, Register x0);

  OutputCode divq(Register y0, Register x0);

  OutputCode mov(Register y, Register x0);

  OutputCode mov2x(Register y, Register x0, String dir1, String dir2);

  OutputCode movx(Register y, Register x0, String dir);

  OutputCode neg(Register y, Register x0);

  OutputCode res(Register a);

  OutputCode res(Register a, Register b);

  OutputCode sub(Register y, Register x0, Register x1);

  OutputCode sub2x(Register y, Register x0, String dir1, String dir2, Register x1);

  OutputCode subx(Register y, Register x0, String dir1, Register x1);


  OutputCode load_pattern(String r1, byte r, byte c, byte rx, byte cx);

  OutputCode select_pattern(byte r, byte c, byte rx, byte cx);

  OutputCode kernel_begin();
  OutputCode kernel_end();

  default OutputCode comment(String comment){
    return new OutputCode().addComment("/*"+comment+"*/");
  }
  default OutputCode newLine(){
    return new OutputCode().addComment("\n");
  }

  OutputCode all();
  OutputCode ALL();
  OutputCode where(Register a);
  OutputCode WHERE(String x);

  OutputCode in(Register a, int value);
}
