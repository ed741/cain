package uk.co.edstow.cain.scamp5;

import uk.co.edstow.cain.RegisterAllocator;

import java.util.List;

public interface Scamp5OutputFormatter {
  String nor(RegisterAllocator.Register r1, RegisterAllocator.Register r2, RegisterAllocator.Register r3);
  String mov(RegisterAllocator.Register r1, RegisterAllocator.Register r2);
  String clr(List<RegisterAllocator.Register> regs);
  String set(RegisterAllocator.Register r1);
  String dnews0(RegisterAllocator.Register r1);
}
