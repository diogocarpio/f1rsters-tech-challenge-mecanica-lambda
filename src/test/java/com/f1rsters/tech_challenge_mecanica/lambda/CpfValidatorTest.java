package com.f1rsters.tech_challenge_mecanica.lambda;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CpfValidatorTest {
    
    private final CpfValidator validator = new CpfValidator();
    
    @Test
    void testValidCpf() {
        assertTrue(validator.isValid("12345678909"));
        assertTrue(validator.isValid("529.982.247-25"));
        assertTrue(validator.isValid("52998224725"));
    }
    
    @Test
    void testInvalidCpf() {
        assertFalse(validator.isValid("12345678900")); // Wrong check digits
        assertFalse(validator.isValid("11111111111")); // All same digits
        assertFalse(validator.isValid("1234567890"));  // Wrong length
        assertFalse(validator.isValid("123456789012")); // Wrong length
        assertFalse(validator.isValid(""));             // Empty
        assertFalse(validator.isValid(null));           // Null
        assertFalse(validator.isValid("abc.def.ghi-jk")); // Invalid characters
    }
    
    @Test
    void testCpfWithFormatting() {
        String formattedCpf = "529.982.247-25";
        assertTrue(validator.isValid(formattedCpf));
    }
}
