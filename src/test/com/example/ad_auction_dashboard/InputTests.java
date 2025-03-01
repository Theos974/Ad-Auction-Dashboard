package com.example.ad_auction_dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.ad_auction_dashboard.logic.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class InputTests {

    //formatting for all csv types
    //Validating Inputs
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
    @DisplayName("Month is invalid")
    void monthIsInvalid(){
        LogDate log1 = new LogDate(2025, 13, 10, 10, 10, 10);
        LogDate log2 = new LogDate(2025, 0, 10, 10, 10, 10);
        assertEquals(-1, log1.getMonth(), "Year should have not been changed to 13 from -1");
        assertEquals(-1, log2.getMonth(), "Year should have not been changed to 0 from -1");
    }
    @Test
    @DisplayName("Month is valid")
    void monthIsValid(){
        LogDate log1 = new LogDate(2025, 12, 10, 10, 10, 10);
        LogDate log2 = new LogDate(2025, 1, 10, 10, 10, 10);
        assertEquals(12, log1.getMonth(), "Year should have been changed to 12 from -1");
        assertEquals(1, log2.getMonth(), "Year should have been changed to 1 from -1");
    }
    @Test
    @DisplayName("31 Day Month Day is Invalid")
    void dayIsInvalid31Day(){
        LogDate log1 = new LogDate(2025, 1, 32, 10, 10, 10);
        LogDate log2 = new LogDate(2025, 5, 0, 10, 10, 10);
        assertEquals(-1, log1.getDay(), "Year should have not been changed to 32 from -1");
        assertEquals(-1, log2.getDay(), "Year should have not been changed to 0 from -1");
    }
    @Test
    @DisplayName("31 Day Month Day is Valid")
    void dayIsValid31Day(){
        LogDate log1 = new LogDate(2025, 1, 31, 10, 10, 10);
        LogDate log2 = new LogDate(2025, 5, 1, 10, 10, 10);
        assertEquals(31, log1.getDay(), "Year should have been changed to 31 from -1");
        assertEquals(1, log2.getDay(), "Year should have been changed to 1 from -1");
    }
    @Test
    @DisplayName("30 Day Month Day is Invalid")
    void dayIsInvalid30Day(){
        LogDate log1 = new LogDate(2025, 4, 31, 10, 10, 10);
        LogDate log2 = new LogDate(2025, 6, 0, 10, 10, 10);
        assertEquals(-1, log1.getDay(), "Year should have not been changed to 31 from -1");
        assertEquals(-1, log2.getDay(), "Year should have not been changed to 0 from -1");
    }
    @Test
    @DisplayName("30 Day Month Day is Valid")
    void dayIsValid30Day(){
        LogDate log1 = new LogDate(2025, 4, 30, 10, 10, 10);
        LogDate log2 = new LogDate(2025, 6, 1, 10, 10, 10);
        assertEquals(30, log1.getDay(), "Year should have been changed to 30 from -1");
        assertEquals(1, log2.getDay(), "Year should have been changed to 1 from -1");
    }
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
    @Test
    @DisplayName("Hour is Invalid")
    void hourIsInvalid(){
        LogDate log1 = new LogDate(2025, 2, 14, 24, 10, 10);
        LogDate log2 = new LogDate(2025, 4, 15, -2, 10, 10);
        assertEquals(-1, log1.getHour(), "Year should have not been changed to 13 from -1");
        assertEquals(-1, log2.getHour(), "Year should have not been changed to -2 from -1");
    }
    @Test
    @DisplayName("Hour is Valid")
    void hourIsValid(){
        LogDate log1 = new LogDate(2025, 3, 14, 23, 10, 10);
        LogDate log2 = new LogDate(2025, 4, 15, 0, 10, 10);
        assertEquals(23, log1.getHour(), "Year should have been changed to 12 from -1");
        assertEquals(0, log2.getHour(), "Year should have been changed to 0 from -1");
    }
    @Test
    @DisplayName("Minute is Invalid")
    void minuteIsInvalid(){
        LogDate log1 = new LogDate(2025, 2, 14, 10, 60, 10);
        LogDate log2 = new LogDate(2025, 4, 15, 10, -2, 10);
        assertEquals(-1, log1.getMinute(), "Year should have not been changed to 60 from -1");
        assertEquals(-1, log2.getMinute(), "Year should have not been changed to -2 from -1");
    }
    @Test
    @DisplayName("Minute is Valid")
    void minuteIsValid(){
        LogDate log1 = new LogDate(2025, 2, 14, 10, 59, 10);
        LogDate log2 = new LogDate(2025, 4, 15, 10, 0, 10);
        assertEquals(59, log1.getMinute(), "Year should have been changed to 59 from -1");
        assertEquals(0, log2.getMinute(), "Year should have been changed to 0 from -1");
    }
    @Test
    @DisplayName("Second is Invalid")
    void secondIsInvalid(){
        LogDate log1 = new LogDate(2025, 3, 14, 10, 30, 60);
        LogDate log2 = new LogDate(2025, 4, 15, 10, 30, -2);
        assertEquals(-1, log1.getSecond(), "Year should have not been changed to 60 from -1");
        assertEquals(-1, log2.getSecond(), "Year should have not been changed to -2 from -1");
    }
    @Test
    @DisplayName("Second is Valid")
    void secondIsValid(){
        LogDate log1 = new LogDate(2025, 2, 14, 10, 30, 59);
        LogDate log2 = new LogDate(2025, 4, 15, 10, 30, 0);
        assertEquals(59, log1.getSecond(), "Year should have been changed to 59 from -1");
        assertEquals(0, log2.getSecond(), "Year should have been changed to 0 from -1");
    }
    @Test
    @DisplayName("Date Validation")
    void datevalidation(){
        LogDate log1 = new LogDate("no");
        LogDate log2 = new LogDate("n/a");
        LogDate log3 = new LogDate(2025, 3, 16, 5, 28, 6);
        assertEquals("", log1.getDate(), "no is not a valid Date");
        assertEquals("n/a", log2.getDate(), "n/a is valid as an 'invalid' date");
        assertEquals("2025-03-16 05:28:06", log3.getDate(), "Is a valid Date");
    }
    @Test
    @DisplayName("ID Validation")
    void idValidation(){
        ImpressionLog log1 = new ImpressionLog("2015-01-01 12:00:04", "-1", "Male", "35-44", "Medium", "Social Media", "0.001632");
        ImpressionLog log2 = new ImpressionLog("2015-01-01 12:00:04", "One", "Male", "35-44", "Medium", "Social Media", "0.001632");
        ImpressionLog log3 = new ImpressionLog("2015-01-01 12:00:04", Integer.toString(5), "Male", "35-44", "Medium", "Social Media", "0.001632");
        ImpressionLog log4 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "35-44", "Medium", "Social Media", "0.001632");
        assertEquals("",log1.getId(), "-1 Should not be a valid ID");
        assertEquals("", log2.getId(), "One should not be accepted, ID's must be in numerical form");
        assertEquals("5", log3.getId(), "5 Should be accepted as an ID");
        assertEquals("4620864431353617408", log4.getId(), "4620864431353617408 should be accepted as an ID");

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

    @Test
    @DisplayName("Age Validation")
    void ageValidation(){
        ImpressionLog log1 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Medium", "Social Media", "0.001632");
        ImpressionLog log2 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "25-34", "Medium", "Social Media", "0.001632");
        ImpressionLog log3 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "35-44", "Medium", "Social Media", "0.001632");
        ImpressionLog log4 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "45-54", "Medium", "Social Media", "0.001632");
        ImpressionLog log5 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", ">54", "Medium", "Social Media", "0.001632");
        ImpressionLog log6 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "20", "Medium", "Social Media", "0.001632");
        ImpressionLog log7 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<54", "Medium", "Social Media", "0.001632");
        assertEquals("<25", log1.getAge(), "<25 is a valid age range");
        assertEquals("25-34", log2.getAge(), "25-34 is a valid age range");
        assertEquals("35-44", log3.getAge(), "35-44 is a valid age range");
        assertEquals("45-54", log4.getAge(), "45-54 is a valid age range");
        assertEquals(">54", log5.getAge(), ">54 is a valid age range");
        assertEquals("", log6.getAge(), "20 is not a valid age range");
        assertEquals("", log7.getAge(), "<54 is not a valid age range");
    }
    @Test
    @DisplayName("Income Validation")
    void incomeValidation(){
        ImpressionLog log1 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Low", "Social Media", "0.001632");
        ImpressionLog log2 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Medium", "Social Media", "0.001632");
        ImpressionLog log3 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "High", "Social Media", "0.001632");
        ImpressionLog log4 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Middle", "Social Media", "0.001632");
        assertEquals("Low", log1.getIncome(), "Low is a valid income");
        assertEquals("Medium", log2.getIncome(), "Medium is a valid income");
        assertEquals("High", log3.getIncome(), "High is a valid income");
        assertEquals("", log4.getIncome(), "Middle is not a valid income");
    }
    @Test
    @DisplayName("Context Validation")
    void contextValidation(){
        ImpressionLog log1 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Low", "News", "0.001632");
        ImpressionLog log2 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Low", "Shopping", "0.001632");
        ImpressionLog log3 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Low", "Social Media", "0.001632");
        ImpressionLog log4 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Low", "Blog", "0.001632");
        ImpressionLog log5 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Low", "Hobbies", "0.001632");
        ImpressionLog log6 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Low", "Travel", "0.001632");
        ImpressionLog log7 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Low", "Food", "0.001632");
        ImpressionLog log8 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Low", "55676", "0.001632");
        assertEquals("News", log1.getContext(), "News is a valid context");
        assertEquals("Shopping", log2.getContext(), "Shopping is a valid context");
        assertEquals("Social Media", log3.getContext(), "Social Media is a valid context");
        assertEquals("Blog", log4.getContext(), "Blog is a valid context");
        assertEquals("Hobbies", log5.getContext(), "Hobbies is a valid context");
        assertEquals("Travel", log6.getContext(), "Travel is a valid context");
        assertEquals("", log7.getContext(), "Food is not a valid context");
        assertEquals("", log8.getContext(), "55676 is not a valid context");
    }
    @Test
    @DisplayName("Impression Cost Validation")
    void impressionCostValidation(){
        ImpressionLog log1 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Low", "News", "-0.0065");
        ImpressionLog log2 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Low", "News", "0.055.7764");
        ImpressionLog log3 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Low", "News", "Zero");
        ImpressionLog log4 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Low", "News", "0.000000");
        ImpressionLog log5 = new ImpressionLog("2015-01-01 12:00:04", "4620864431353617408", "Male", "<25", "Low", "News", "0.001632");
        assertEquals(-1, log1.getImpressionCost(), "Impression cost cannot be negative");
        assertEquals(-1, log2.getImpressionCost(), "Impression cost must be a valid float");
        assertEquals(-1, log3.getImpressionCost(), "Impression cost must be a float");
        assertEquals(0, log4.getImpressionCost(), "Impression cost can be 0");
        assertEquals("0.001632", log5.getImpressionCost().toString(), "0.001632 is a valid impression cost");
    }
    @Test
    @DisplayName("Click Cost Validation")
    void clickCostValidation(){
        ClickLog log1 = new ClickLog("2015-01-01 12:00:04", "4620864431353617408", "-12.546");
        ClickLog log2 = new ClickLog("2015-01-01 12:00:04", "4620864431353617408", "16.325.76");
        ClickLog log3 = new ClickLog("2015-01-01 12:00:04", "4620864431353617408", "Eleven");
        ClickLog log4 = new ClickLog("2015-01-01 12:00:04", "4620864431353617408", "0.000000");
        ClickLog log5 = new ClickLog("2015-01-01 12:00:04", "4620864431353617408", "9.340521");
        assertEquals(-1, log1.getClickCost(), "Click cost cannot be negative");
        assertEquals(-1, log2.getClickCost(), "Click cost must be a valid float");
        assertEquals(-1, log3.getClickCost(), "Click cost must be a float");
        assertEquals(0, log4.getClickCost(), "Click cost can be 0");
        assertEquals("9.340521", log5.getClickCost().toString(), "9.340521 is a valid click cost");
    }
    @Test
    @DisplayName("Pages Viewed Validation")
    void pagesViewedValidation(){
        ServerLog log1 = new ServerLog("2015-01-01 12:00:04", "4620864431353617408", "2015-01-01 12:05:13", "0", "No");
        ServerLog log2 = new ServerLog("2015-01-01 12:00:04", "4620864431353617408", "2015-01-01 12:05:13", "-2", "No");
        ServerLog log3 = new ServerLog("2015-01-01 12:00:04", "4620864431353617408", "2015-01-01 12:05:13", "a", "No");
        ServerLog log4 = new ServerLog("2015-01-01 12:00:04", "4620864431353617408", "2015-01-01 12:05:13", "1.56", "No");
        ServerLog log5 = new ServerLog("2015-01-01 12:00:04", "4620864431353617408", "2015-01-01 12:05:13", "7", "No");
        assertEquals(-1, log1.getPagesViewed(), "Pages viewed needs to be at least 1");
        assertEquals(-1, log2.getPagesViewed(), "Pages viewed cannot be negative (-1 is an empty state)");
        assertEquals(-1, log3.getPagesViewed(), "Pages viewed needs to be an integer");
        assertEquals(-1, log4.getPagesViewed(), "Pages viewed needs to be an integer");
        assertEquals(7, log5.getPagesViewed(), "7 is a valid number for pages viewed");
    }
    @Test
    @DisplayName("Conversion Validation")
    void conversionValidation(){
        ServerLog log1 = new ServerLog("2015-01-01 12:00:04", "4620864431353617408", "2015-01-01 12:05:13", "7", "True");
        ServerLog log2 = new ServerLog("2015-01-01 12:00:04", "4620864431353617408", "2015-01-01 12:05:13", "7", "40");
        ServerLog log3 = new ServerLog("2015-01-01 12:00:04", "4620864431353617408", "2015-01-01 12:05:13", "7", "Yes");
        ServerLog log4 = new ServerLog("2015-01-01 12:00:04", "4620864431353617408", "2015-01-01 12:05:13", "7", "No");
        assertEquals(null, log1.getConversion(), "Conversion value needs to be Yes or No");
        assertEquals(null, log2.getConversion(), "Conversion value needs to be a Yes or No");
        assertEquals(Boolean.TRUE, log3.getConversion(), "Conversion value can be Yes");
        assertEquals(Boolean.FALSE, log4.getConversion(), "Conversion value can be No");
    }
    @Test
    @DisplayName(".zip Opening Validation")
    void zipOpenValidation(){
        try {
            System.out.println(System.getProperty("user.dir"));
            StringBuilder sb = new StringBuilder();
            sb.append("Date,ID,Gender,Age,Income,Context,Impression Cost\n" +
                    "2015-01-01 12:00:02,4620864431353617408,Male,25-34,High,Blog,0.001713");

            File f = new File("src/test/com/example/ad_auction_dashboard/test.zip");
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
            ZipEntry e = new ZipEntry("impression_log.csv");
            out.putNextEntry(e);

            byte[] data = sb.toString().getBytes();
            out.write(data, 0, data.length);
            out.closeEntry();

            sb = new StringBuilder();
            sb.append("Date,ID,Click Cost\n" +
                    "2015-01-01 12:01:21,8895519749317550080,11.794442");
            e = new ZipEntry("click_log.csv");
            out.putNextEntry(e);
            data = sb.toString().getBytes();
            out.write(data, 0, data.length);
            out.closeEntry();

            sb = new StringBuilder();
            sb.append("Entry Date,ID,Exit Date,Pages Viewed,Conversion\n" +
                    "2015-01-01 12:01:21,8895519749317550080,2015-01-01 12:05:13,7,No");
            e = new ZipEntry("server_log.csv");
            out.putNextEntry(e);
            data = sb.toString().getBytes();
            out.write(data, 0, data.length);
            out.closeEntry();
            out.close();

            String[] expected = {"Date,ID,Gender,Age,Income,Context,Impression Cost\n" +
                    "2015-01-01 12:00:02,4620864431353617408,Male,25-34,High,Blog,0.001713", "Date,ID,Click Cost\n" +
                    "2015-01-01 12:01:21,8895519749317550080,11.794442", "Entry Date,ID,Exit Date,Pages Viewed,Conversion\n" +
                    "2015-01-01 12:01:21,8895519749317550080,2015-01-01 12:05:13,7,No"};

            FileHandler fileHandler = new FileHandler();
            String [] output = fileHandler.readFromZip("src/test/com/example/ad_auction_dashboard/test.zip");
            f.delete();
            assertEquals(expected[2], output[2], "Should open and read .zip file in order");


        } catch (Exception e) {
            System.err.println(e);
        }
    }
    @Test
    @DisplayName(".csv Opening Validation")
    void csvOpenValidation(){
        //opens csv, gets contents on csv in a list
        //handles non-csv addresses
        try {
            File f = new File("src/test/com/example/ad_auction_dashboard/impression_log.csv");
            FileWriter fileWriter = new FileWriter("src/test/com/example/ad_auction_dashboard/impression_log.csv");
            fileWriter.write("Date,ID,Gender,Age,Income,Context,Impression Cost\n" +
                    "2015-01-01 12:00:02,4620864431353617408,Male,25-34,High,Blog,0.001713");
            fileWriter.close();
            FileHandler fileHandler = new FileHandler();
            String output = fileHandler.readFromCsv("src/test/com/example/ad_auction_dashboard/impression_log.csv");
            f.delete();
            assertEquals("Date,ID,Gender,Age,Income,Context,Impression Cost\n" +
                    "2015-01-01 12:00:02,4620864431353617408,Male,25-34,High,Blog,0.001713", output, "Should be a valid csv");
        } catch (Exception e){
            System.err.println(e);
        }
    }
    @Test
    @DisplayName("CSV Splitting")
    void csvSplitting(){
        String csv1 = "Date,ID,Gender,Age,Income,Context,Impression Cost\n2015-01-01 12:00:02,4620864431353617408,Male,25-34,High,Blog,0.001713";
        String[] expected = {"Date,ID,Gender,Age,Income,Context,Impression Cost","2015-01-01 12:00:02,4620864431353617408,Male,25-34,High,Blog,0.001713"};
        FileHandler fileHandler = new FileHandler();
        assertEquals(expected[0], fileHandler.splitCsv(csv1)[0]);
        assertEquals(expected[1], fileHandler.splitCsv(csv1)[1]);
    }
    @Test
    @DisplayName("Impression Log Splitting Validation")
    void impressionSplittingValidation(){
        String[] impression1 = {"Date,ID,Gender,Age,Income,Context,Impression Cost","2015-01-01 12:00:02,4620864431353617408,Male,25-34,High,Blog,0.001713"};
        String[] impression2 = {"Date,ID,Gender,Age,Income,Context,Impression Cost","2015-01-01 12:00:02,4620864431353617408,Male,25-34,High,Low,Blog,0.001713"};
        FileHandler fileHandler = new FileHandler();
        String[][] expected1 = {{"2015-01-01 12:00:02","4620864431353617408","Male","25-34","High","Blog","0.001713"}};
        assertEquals(expected1[0][3], fileHandler.splitImpressions(impression1)[0][3], "Is a valid impression Log");
        assertNull(fileHandler.splitImpressions(impression2), "A second income field is not a valid field in an impression log");
    }
    @Test
    @DisplayName("Click Log Splitting Validation")
    void clickSplittingValidation(){
        String[] click1 = {"Date,ID,Click Cost","2015-01-01 12:01:21,8895519749317550080,11.794442"};
        String[] click2 = {"Date,ID,Click Cost","2015-01-01 12:01:21,8895519749317550080,11.794442,4534"};
        FileHandler fileHandler = new FileHandler();
        String[][] expected1 = {{"2015-01-01 12:01:21","8895519749317550080","11.794442"}};
        assertEquals(expected1[0][1], fileHandler.splitClick(click1)[0][1], "Is a valid click Log");
        assertNull(fileHandler.splitImpressions(click2), "A click Log should only have 3 fields");
    }
    @Test
    @DisplayName("Server Log Splitting Validation")
    void serverSplittingValidation(){
        String[] server1 = {"Entry Date,ID,Exit Date,Pages Viewed,Conversion","2015-01-01 12:01:21,8895519749317550080,2015-01-01 12:05:13,7,No"};
        String[] server2 = {"Entry Date,ID,Exit Date,Pages Viewed,Conversion","2015-01-01 12:01:21,8895519749317550080,2015-01-01 12:05:13,7,No,Yes"};
        FileHandler fileHandler = new FileHandler();
        String[][] expected1 = {{"2015-01-01 12:01:21","8895519749317550080","2015-01-01 12:05:13","7","No"}};
        assertEquals(expected1[0][4], fileHandler.splitServer(server1)[0][4], "Is a valid server Log");
        assertNull(fileHandler.splitImpressions(server2), "Server log should not have 2 conversion fields");
    }
    @Test
    @DisplayName("Impression Log Formatting Validation")
    void impressionFormattingValidation(){
        String[][] impression = {{"2015-01-01 12:00:02","4620864431353617408","Male","25-34","High","Blog","0.001713"}};
        FileHandler fileHandler = new FileHandler();
        ImpressionLog[] expected = {new ImpressionLog("2015-01-01 12:00:02","4620864431353617408","Male","25-34","High","Blog","0.001713")};
        assertEquals(expected[0].getLogAsString(), fileHandler.formatImpressions(impression)[0].getLogAsString());
    }
    @Test
    @DisplayName("Click Log Formatting Validation")
    void clickFormattingValidation(){
        String[][] click = {{"2015-01-01 12:01:21","8895519749317550080","11.794442"}};
        FileHandler fileHandler = new FileHandler();
        ClickLog[] expected = {new ClickLog("2015-01-01 12:01:21","8895519749317550080","11.794442")};
        assertEquals(expected[0].getLogAsString(), fileHandler.formatClick(click)[0].getLogAsString());
    }
    @Test
    @DisplayName("Server Log Formatting Validation")
    void serverFormattingValidation(){
        String[][] server = {{"2015-01-01 12:01:21","8895519749317550080","2015-01-01 12:05:13","7","No"}};
        FileHandler fileHandler = new FileHandler();
        ServerLog[] expected = {new ServerLog("2015-01-01 12:01:21","8895519749317550080","2015-01-01 12:05:13","7","No")};
        assertEquals(expected[0].getLogAsString(), fileHandler.formatServer(server)[0].getLogAsString());
    }
}


