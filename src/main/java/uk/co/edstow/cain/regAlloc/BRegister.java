package uk.co.edstow.cain.regAlloc;

import java.util.Objects;

public class BRegister extends Register {
    public final int bank;

    public BRegister(int bank, String name) {
        super(name);
        this.bank = bank;
    }

    public static BRegister makeBRegister(String code) {
        String[] strs = code.split(":");
        if (strs.length != 2) throw new IllegalArgumentException("using Banked Kernel3D goal requires that" +
                " registers are specified as '0:A' where 0 is the bank and A is the virtual" +
                " Register. '" + code + "' does not conform");
        return new BRegister(Integer.parseInt(strs[0]), strs[1]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BRegister bRegister = (BRegister) o;
        return this.bank == bRegister.bank && this.name.equals(bRegister.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bank);
    }

    @Override
    public String toString() {
        return this.bank + "." + this.name;
    }
}
