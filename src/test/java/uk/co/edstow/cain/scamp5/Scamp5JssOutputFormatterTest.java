package uk.co.edstow.cain.scamp5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Scamp5JssOutputFormatterTest {

  @Test
  void NOR() {
    Scamp5JssOutputFormatter formatter = new Scamp5JssOutputFormatter("s");
    String nor = formatter.NOR("A", "B", "C");

    assertEquals("s.NOR(s.A, s.B, s.C); ", nor);
  }
}