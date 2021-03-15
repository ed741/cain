package uk.co.edstow.cain.scamp5;

import org.junit.jupiter.api.Test;
import uk.co.edstow.cain.regAlloc.Register;
import uk.co.edstow.cain.scamp5.output.Scamp5JssOutputFormatter;

import static org.junit.jupiter.api.Assertions.*;

class Scamp5JssOutputFormatterTest {

  @Test
  void NOR() {
    Scamp5JssOutputFormatter formatter = new Scamp5JssOutputFormatter("s");
    String nor = formatter.NOR("A", "B", "C");

    assertEquals("s.NOR(s.A, s.B, s.C); ", nor);
  }

  @Test
  void subx() {
    Scamp5JssOutputFormatter formatter = new Scamp5JssOutputFormatter("sim");
    String subx = formatter.subx(new Register("A"), new Register("B"), "north", new Register("C"));

    assertEquals("sim.subx(sim.A, sim.B, north, sim.C); ", subx);
  }
}