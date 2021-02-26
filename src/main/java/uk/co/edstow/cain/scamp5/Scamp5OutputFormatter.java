package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.regAlloc.RegisterAllocator;

import java.util.List;

public interface Scamp5OutputFormatter {
  String nor(String r1, String r2, String r3);

  String mov(String r1, String r2);

  String clr(String... regs);

  String set(String r1);

  String dnews0(String r1, String r2);
}
