package uk.co.edstow.cain;

import org.junit.jupiter.api.Test;
import uk.co.edstow.cain.regAlloc.RegisterAllocator;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegisterAllocatorRegisterTest {

    @Test
    void newRegisters(){
        RegisterAllocator.Register[] array = RegisterAllocator.Register.getRegisters(10000);
        Set<RegisterAllocator.Register> regs = new HashSet<>();
        for (RegisterAllocator.Register register : array) {
            regs.add(register);
        }
        assertEquals(array.length, regs.size());
        assertEquals("A", array[0].toString());
        assertEquals("B", array[1].toString());
        assertEquals("Z", array[25].toString());
        assertEquals("AA", array[26].toString());
        assertEquals("AZ", array[51].toString());
        assertEquals("CV", array[99].toString());

        for (int i = 1; i < array.length; i++) {
            char first = array[i-1].name.charAt(array[i-1].name.length()-1);
            char second = array[i].name.charAt(array[i].name.length()-1);
            if(first=='Z'){assertEquals('A', second);}
            else {assertEquals(first+1, second);}
        }
    }

}