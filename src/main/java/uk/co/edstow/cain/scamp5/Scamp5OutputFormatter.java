package uk.co.edstow.cain.scamp5;

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
}
