package com.example.ad_auction_dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.ad_auction_dashboard.logic.LogDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
public class InputTests {

    //formatting for all csv types



    //Validating Inputs
    @Test
    @DisplayName("Year not 4 digits")
    void yearNot2Digits(){
        LogDate log1 = new LogDate(999, 10, 10, 10, 10, 10);
        LogDate log2 = new LogDate(10000, 10, 10, 10, 10, 10);
        assertEquals(log1.getYear(), -1, "Year should have not been changed to 999 from -1");
        assertEquals(log2.getYear(), -1, "Year should have not been changed to 10000 from -1");
    }

    @Test
    @DisplayName("Year is valid")
    void yearIsValid(){
        LogDate log1 = new LogDate(1000, 10, 10, 10, 10, 10);
        LogDate log2 = new LogDate(9999, 10, 10, 10, 10, 10);
        assertEquals(log1.getYear(), 1000, "Year should have been changed from -1 to 1000");
        assertEquals(log2.getYear(), 9999, "Year should have been changed from -1 to 9999");
    }

    @Test
    @DisplayName("Month is invalid")
    void monthIsInvalid(){

    }
    @Test
    @DisplayName("Month is valid")
    void monthIsValid(){

    }
    @Test
    @DisplayName("31 Day Month Day is Invalid")
    void dayIsInvalid31Day(){

    }
    @Test
    @DisplayName("31 Day Month Day is Valid")
    void dayIsValid31Day(){

    }
    @Test
    @DisplayName("30 Day Month Day is Invalid")
    void dayIsInvalid30Day(){

    }
    @Test
    @DisplayName("30 Day Month Day is Valid")
    void dayIsValid30Day(){

    }
    @Test
    @DisplayName("28 Day Month Day is Invalid")
    void dayIsInvalid28Day(){

    }
    @Test
    @DisplayName("28 Day Month Day is Valid")
    void dayisValid28Day(){

    }
    @Test
    @DisplayName("Leap Year Day is Invalid")
    void leapYearDayisInvalid(){

    }
    @Test
    @DisplayName("Leap Year Day is Valid")
    void leapYearDayisValid(){

    }
    @Test
    @DisplayName("Hour is Invalid")
    void hourIsInvalid(){

    }
    @Test
    @DisplayName("Hour is Valid")
    void hourIsValid(){

    }
    @Test
    @DisplayName("Minute is Invalid")
    void minuteIsInvalid(){

    }
    @Test
    @DisplayName("Minute is Valid")
    void minuteIsValid(){

    }
    @Test
    @DisplayName("Second is Invalid")
    void secondIsInvalid(){

    }
    @Test
    @DisplayName("Second is Valid")
    void secondIsValid(){

    }

}


