package com.f1rsters.tech_challenge_mecanica.lambda;

public class CpfValidator {
    
    public boolean isValid(String cpf) {
        if (cpf == null || cpf.isEmpty()) {
            return false;
        }
        
        // Remove non-numeric characters
        cpf = cpf.replaceAll("[^0-9]", "");
        
        // Check length
        if (cpf.length() != 11) {
            return false;
        }
        
        // Check if all digits are the same
        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }
        
        // Validate check digits
        return validateCheckDigits(cpf);
    }
    
    private boolean validateCheckDigits(String cpf) {
        // Calculate first check digit
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
        }
        int remainder = sum % 11;
        int firstCheckDigit = remainder < 2 ? 0 : 11 - remainder;
        
        if (firstCheckDigit != Character.getNumericValue(cpf.charAt(9))) {
            return false;
        }
        
        // Calculate second check digit
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
        }
        remainder = sum % 11;
        int secondCheckDigit = remainder < 2 ? 0 : 11 - remainder;
        
        return secondCheckDigit == Character.getNumericValue(cpf.charAt(10));
    }
}
