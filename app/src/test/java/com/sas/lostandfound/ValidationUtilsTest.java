package com.sas.lostandfound;

import org.junit.Test;
import static org.junit.Assert.*;

public class ValidationUtilsTest {

    @Test
    public void testIsValidPhone_Bangladesh() {
        // Valid forms
        assertTrue(ValidationUtils.isValidPhone("+880", "01712345678"));
        assertTrue(ValidationUtils.isValidPhone("+880", "1712345678"));
        assertTrue(ValidationUtils.isValidPhone("+880", "01312345678"));
        assertTrue(ValidationUtils.isValidPhone("+880", "1912345678"));
        
        // Invalid forms
        assertFalse(ValidationUtils.isValidPhone("+880", "01212345678")); // Invalid operator code
        assertFalse(ValidationUtils.isValidPhone("+880", "1212345678"));
        assertFalse(ValidationUtils.isValidPhone("+880", "0171234567")); // Too short
        assertFalse(ValidationUtils.isValidPhone("+880", "017123456789")); // Too long
    }

    @Test
    public void testIsValidPhone_International() {
        // Valid forms (6 to 14 digits)
        assertTrue(ValidationUtils.isValidPhone("+1", "2025550143"));
        assertTrue(ValidationUtils.isValidPhone("+44", "2079460958"));
        
        // Invalid forms
        assertFalse(ValidationUtils.isValidPhone("+1", "12345")); // Too short
        assertFalse(ValidationUtils.isValidPhone("+1", "123456789012345")); // Too long
    }

    @Test
    public void testExtractCountryCode() {
        assertEquals("+880", ValidationUtils.extractCountryCode("🇧🇩 +880"));
        assertEquals("+1", ValidationUtils.extractCountryCode("🇺🇸 +1"));
        assertEquals("+880", ValidationUtils.extractCountryCode(""));
        assertEquals("+880", ValidationUtils.extractCountryCode(null));
    }

    @Test
    public void testParsePhoneNumber() {
        // Legacy BD numbers without prefix
        assertArrayEquals(new String[]{"+880", "1712345678"}, ValidationUtils.parsePhoneNumber("01712345678"));
        assertArrayEquals(new String[]{"+880", "1712345678"}, ValidationUtils.parsePhoneNumber("1712345678"));
        
        // Proper prefixed numbers
        assertArrayEquals(new String[]{"+880", "1712345678"}, ValidationUtils.parsePhoneNumber("+8801712345678"));
        assertArrayEquals(new String[]{"+1", "2025550143"}, ValidationUtils.parsePhoneNumber("+12025550143"));
    }
}
