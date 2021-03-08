package uk.co.edstow.cain.regAlloc;

import java.util.Objects;

public class BRegister extends Register {
    public final int bank;

    public BRegister(int bank, String name) {
        super(name);
        this.bank = bank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BRegister bRegister = (BRegister) o;
        return this.bank == bRegister.bank;
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
