package com.example.ad_auction_dashboard.logic;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class LogDate {

    private int year = -1;
    private int month = -1;
    private int day = -1;
    private int hour = -1;
    private int minute = -1;
    private int second = -1;
    private Boolean exists = true;


    public LogDate (int year, int month, int day, int hour, int minute, int second){
        setYear(year);
        setMonth(month);
        setDay(day);
        setHour(hour);
        setMinute(minute);
        setSecond(second);
    }
    public LogDate(String invalid){
        if (Objects.equals(invalid, "n/a")){
            this.exists = Boolean.FALSE;
        } else {
            System.err.println("Invalid date");
        }
    }

    public String getDate(){
        return null;
    }

    private void setYear(int year){
        if (year >= 1000 && year <= 9999){
            this.year = year;
        } else {System.err.println("Invalid Year");}
    }
    public int getYear(){
        return this.year;
    }
    private void setMonth(int month){
        if (month >= 1 && month <= 12){
            this.month = month;
        } else {System.err.println("Invalid Month");}
    }
    public int getMonth(){
        return this.month;
    }
    private void setDay(int day){
        if (day <= 0) {System.err.println("Day Less than 0");}
        else if (Arrays.asList(1,3,5,7,8,10,12).contains(this.getMonth())){
            if (day <= 31) {
                this.day = day;
            }
        } else if (Arrays.asList(4,6,9,11).contains(this.getMonth())) {
            if (day <= 30) {
                this.day = day;
            }
        } else if (this.getMonth() == 2) {
            if (this.getYear() % 4 == 0){
                if (day <= 29){
                    this.day = day;
                }
            } else {
                if (day <= 28){
                    this.day = day;
                }
            }
        } else {
            System.err.println("Invalid Large Day");
        }
    }
    public int getDay(){
        return this.day;
    }
    private void setHour(int hour){
        if (hour >= 0 && hour <= 12) {
            this.hour = hour;
        } else {System.err.println("Invalid Hour");}
    }
    public int getHour(){
        return this.hour;
    }
    private void setMinute(int minute){
        if (minute >= 0 && minute <= 59){
            this.minute = minute;
        } else {System.err.println("Invalid Minute");}
    }
    public int getMinute(){
        return this.minute;
    }
    private void setSecond(int second){
        if (second >= 0 && second <= 59){
            this.second = second;
        } else {System.err.println("Invalid Second");}
    }
    public int getSecond(){
        return this.second;
    }

    public void setExists(Boolean exists) {
        this.exists = exists;
    }

    public Boolean getExists() {
        return exists;
    }
}
