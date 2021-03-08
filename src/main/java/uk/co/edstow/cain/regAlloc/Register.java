package uk.co.edstow.cain.regAlloc;

import java.util.Objects;

public class Register {

    public final String name;

    public static Register[] getRegisters(String... names) {
        Register[] out = new Register[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = new Register(names[i]);
        }
        return out;
    }

    public static Register[] getRegisters(int count) {
        Register[] out = new Register[count];
        for (int i = 0; i < count; i++) {
            out[i] = new Register(i + 1);
        }
        return out;
    }

    public Register(int i) {
        int c = i;
        StringBuilder sb = new StringBuilder();
        while (c > 26) {
            sb.insert(0, ((char) ('A' + ((c - 1) % 26))));
            c = (c - 1) / 26;
        }
        sb.insert(0, (char) ('@' + (c % 27)));
        this.name = sb.toString();
    }

    public Register(String name) {
        this.name = name;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Register register = (Register) o;
        return Objects.equals(name, register.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
