package com.example.ad_auction_dashboard.logic;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public interface LogFile {

    public static LogDate convertDate(String date){
        //2015-01-01 12:01:21
        if (Objects.equals(date, "n/a")){
            return new LogDate(date);
        } else if (date.matches("([0-9]{4})-([0-9]{2})-([0-9]{2}) ([0-9]{2}):([0-9]{2}):([0-9]{2})")){
            String[] splitDate1 = date.split(" ")[0].split("-");
            String[] splitDate2 = date.split(" ")[1].split(":");
            Integer a = Integer.parseInt(splitDate1[0]);
            Integer b = Integer.parseInt(splitDate1[1]);
            Integer c = Integer.parseInt(splitDate1[2]);
            Integer d = Integer.parseInt(splitDate2[0]);
            Integer e = Integer.parseInt(splitDate2[1]);
            Integer f = Integer.parseInt(splitDate2[2]);
            return new LogDate(Integer.parseInt(splitDate1[0]),Integer.parseInt(splitDate1[1]),Integer.parseInt(splitDate1[2]),Integer.parseInt(splitDate2[0]),Integer.parseInt(splitDate2[1]),Integer.parseInt(splitDate2[2]));

        } else {
            return null;
        }
    }
}
