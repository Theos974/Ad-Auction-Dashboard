package com.example.ad_auction_dashboard.logic;

import java.util.Objects;

public interface Log {

    public static LogDate convertDate(String date){
        if (Objects.equals(date, "n/a")){
            return new LogDate(date);
        } else{
            return null;
        }
    }
}
