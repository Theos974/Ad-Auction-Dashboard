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
            String[] splitDatePart1 = date.split(" ")[0].split("-");
            String[] splitDatePart2 = date.split(" ")[1].split(":");
            String[] splitDate = (String[]) Stream.concat(Arrays.stream(splitDatePart1), Arrays.stream(splitDatePart2)).toArray();
            return new LogDate(Integer.parseInt(splitDate[0]),Integer.parseInt(splitDate[1]),Integer.parseInt(splitDate[2]),Integer.parseInt(splitDate[3]),Integer.parseInt(splitDate[4]),Integer.parseInt(splitDate[5]));

        } else {
            return null;
        }
    }
}
