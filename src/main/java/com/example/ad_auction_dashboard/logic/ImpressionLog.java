package com.example.ad_auction_dashboard.logic;

public class ImpressionLog implements Log{
    private LogDate date;
    private String id;
    private Gender gender;
    private Age age;
    private Income income;
    private Context context;
    private Float impressionCost;

    public ImpressionLog(String date, String id, String gender, String age, String income, String context, String impressionCost){
        //this.setDate(date);
        //this.setId(id);
        //this.setGender(gender);
        //this.setAge(age);
        //this.setIncome(income);
        //this.setContext(context);
        //this.setImpressionCost(impressionCost);
    }

    public void setDate(LogDate date) {
        this.date = date;
    }

    public LogDate getDate() {
        return date;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Gender getGender() {
        return gender;
    }

    public void setAge(Age age) {
        this.age = age;
    }

    public Age getAge() {
        return age;
    }

    public void setIncome(Income income) {
        this.income = income;
    }

    public Income getIncome() {
        return income;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public void setImpressionCost(Float impressionCost) {
        this.impressionCost = impressionCost;
    }

    public Float getImpressionCost() {
        return impressionCost;
    }

    enum Gender {
        Male,
        Female
    }
    enum Age {
        A("<25"),
        B("25-34"),
        C("35-44"),
        D("45-54"),
        E(">54")
        ;

        private String range;

        Age(String range){
            this.range = range;
        }

        public String getRange() {
            return range;
        }
    }
    enum Income {
        Low,
        Medium,
        High
    }
    enum Context {
        News,
        Shopping,
        Social,
        Media,
        Blog,
        Hobbies,
        Travel
    }
}
