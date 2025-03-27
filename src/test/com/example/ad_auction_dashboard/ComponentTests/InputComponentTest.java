package com.example.ad_auction_dashboard.ComponentTests;

import static org.junit.jupiter.api.Assertions.*;
import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class InputComponentTest {

    @Test
    @DisplayName("Year not 4 digits")
    void yearNot2Digits(){
        LogDate log1 = new LogDate(999, 10, 10, 10, 10, 10);
        LogDate log2 = new LogDate(10000, 10, 10, 10, 10, 10);
        assertEquals(-1, log1.getYear(), "Year should have not been changed to 999 from -1");
        assertEquals(-1, log2.getYear(), "Year should have not been changed to 10000 from -1");
    }

    @Test
    @DisplayName("Year is valid")
    void yearIsValid(){
        LogDate log1 = new LogDate(1000, 10, 10, 10, 10, 10);
        LogDate log2 = new LogDate(9999, 10, 10, 10, 10, 10);
        assertEquals(1000, log1.getYear(), "Year should have been changed from -1 to 1000");
        assertEquals(9999, log2.getYear(), "Year should have been changed from -1 to 9999");
    }

    @Test
    @DisplayName("Gender Validation")
    void genderValidation(){
        ImpressionLog log1 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "35-44", "Medium", "Social Media", "0.001632");
        ImpressionLog log2 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Female", "35-44", "Medium", "Social Media", "0.001632");
        ImpressionLog log3 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Mole", "35-44", "Medium", "Social Media", "0.001632");
        assertEquals("Male", log1.getGender(), "Male Should be a valid Gender");
        assertEquals("Female", log2.getGender(), "Female Should be a valid Gender");
        assertEquals("", log3.getGender(), "Mole is not a valid gender");
    }

}