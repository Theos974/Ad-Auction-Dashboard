package com.example.ad_auction_dashboard.BoundaryAndPartitionTests;

import static org.junit.jupiter.api.Assertions.*;
import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DateBoundaryTest {

    @Test
    @DisplayName("28 Day Month Day is Invalid")
    void dayIsInvalid28Day(){
        LogDate log1 = new LogDate(2025, 2, 29, 10, 10, 10);
        LogDate log2 = new LogDate(2025, 2, 0, 10, 10, 10);
        assertEquals(-1, log1.getDay(), "Year should have not been changed to 29 from -1");
        assertEquals(-1, log2.getDay(), "Year should have not been changed to 0 from -1");
    }

    @Test
    @DisplayName("28 Day Month Day is Valid")
    void dayisValid28Day(){
        LogDate log1 = new LogDate(2025, 2, 28, 10, 10, 10);
        LogDate log2 = new LogDate(2025, 2, 1, 10, 10, 10);
        assertEquals(28, log1.getDay(), "Year should have been changed to 28 from -1");
        assertEquals(1, log2.getDay(), "Year should have been changed to 1 from -1");
    }

    @Test
    @DisplayName("Leap Year Day is Invalid")
    void leapYearDayisInvalid(){
        LogDate log1 = new LogDate(2024, 2, 30, 10, 10, 10);
        LogDate log2 = new LogDate(2024, 2, 0, 10, 10, 10);
        assertEquals(-1, log1.getDay(), "Year should have not been changed to 30 from -1");
        assertEquals(-1, log2.getDay(), "Year should have not been changed to 0 from -1");
    }

    @Test
    @DisplayName("Leap Year Day is Valid")
    void leapYearDayisValid(){
        LogDate log1 = new LogDate(2024, 1, 29, 10, 10, 10);
        LogDate log2 = new LogDate(2024, 5, 1, 10, 10, 10);
        assertEquals(29, log1.getDay(), "Year should have been changed to 29 from -1");
        assertEquals(1, log2.getDay(), "Year should have been changed to 1 from -1");
    }

}
