package uk.co.edstow.cain.pairgen;


public interface Config {

    interface ConfigWithRegs extends Config{
        int totalAvailableRegisters();
    }
}
