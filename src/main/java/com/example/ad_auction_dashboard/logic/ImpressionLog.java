package com.example.ad_auction_dashboard.logic;

import java.util.Objects;


public class ImpressionLog implements LogFile {
    private LogDate date;
    private String id;
    private Gender gender;
    private Age age;
    private Income income;
    private Context context;
    private Float impressionCost;

    public ImpressionLog(String date, String id, String gender, String age, String income, String context, String impressionCost){
        this.setDate(date);
        this.setId(id);
        this.setGender(gender);
        this.setAge(age);
        this.setIncome(income);
        this.setContext(context);
        this.setImpressionCost(impressionCost);
    }

    public void setDate(String date) {
        this.date = LogFile.convertDate(date);
    }

    public LogDate getDate() {
        return date;
    }

    public void setId(String id) {
        if (id.matches("[0-9]*")){
            this.id = id;
        } else {
            this.id = "";
        }
    }

    public String getId() {
        return id;
    }

    public void setGender(String gender) {
        if (Objects.equals(gender, "Male")){
            this.gender = Gender.Male;
        } else if (Objects.equals(gender, "Female")){
            this.gender = Gender.Female;
        } else {
            this.gender = Gender.Invalid;
        }
    }

    public String getGender() {
        if (this.gender == Gender.Male){
            return "Male";
        } else if (this.gender == Gender.Female){
            return "Female";
        } else {
            return "";
        }
    }

    public void setAge(String age) {
        switch (age){
            case "<25":
                this.age = Age.A;
                break;
            case "25-34":
                this.age = Age.B;
                break;
            case "35-44":
                this.age = Age.C;
                break;
            case "45-54":
                this.age = Age.D;
                break;
            case ">54":
                this.age = Age.E;
                break;
            default:
                this.age = Age.Invalid;
                break;
        }
    }

    public String getAge() {
        switch (this.age){
            case A -> {
                return "<25";
            }
            case B -> {return "25-34";}
            case C -> {return "35-44";}
            case D -> {return "45-54";}
            case E -> {return ">54";}
            case Invalid -> {return "";}
            default -> {return "";}
        }
    }

    public void setIncome(String income) {
        switch (income){
            case "Low":
                this.income = Income.Low;
                break;
            case "Medium":
                this.income = Income.Medium;
                break;
            case "High":
                this.income = Income.High;
                break;
            default:
                this.income = Income.Invalid;
                break;
        }
    }

    public String getIncome() {
        switch (this.income){
            case Low -> {return "Low";}
            case Medium -> {return "Medium";}
            case High -> {return "High";}
            case Invalid -> {return "";}
            default -> {return "";}
        }
    }

    public void setContext(String context) {
        switch (context){
            case "News":
                this.context = Context.News;
                break;
            case "Shopping":
                this.context = Context.Shopping;
                break;
            case "Social Media":
                this.context = Context.Social;
                break;
            case "Media":
                this.context = Context.Media;
                break;
            case "Blog":
                this.context = Context.Blog;
                break;
            case "Hobbies":
                this.context = Context.Hobbies;
                break;
            case "Travel":
                this.context = Context.Travel;
                break;
            default:
                this.context = Context.Invalid;
                break;
        }
    }

    public String getContext() {
        switch (this.context){
            case News -> {return "News";}
            case Shopping -> {return "Shopping";}
            case Social -> {return "Social Media";}
            case Media -> {return "Media";}
            case Blog -> {return "Blog";}
            case Hobbies -> {return "Hobbies";}
            case Travel -> {return "Travel";}
            case Invalid -> {return "";}
            default -> {return "";}
        }
    }

    public void setImpressionCost(String impressionCost) {
        if (impressionCost.matches("[0-9]\\.[0-9]{6}")){
            this.impressionCost = Float.parseFloat(impressionCost);
        } else {
            this.impressionCost = (float) -1;
        }
    }

    public Float getImpressionCost() {
        return impressionCost;
    }

    public String getLogAsString(){
        return this.getDate().getDate() + "," + this.getId() + "," + this.getGender() + "," + this.getAge() + "," + this.getIncome() + "," + this.getContext() + "," + Float.toString(this.getImpressionCost());
    }

    enum Gender {
        Male,
        Female,
        Invalid
    }
    enum Age {
        A("<25"),
        B("25-34"),
        C("35-44"),
        D("45-54"),
        E(">54"),
        Invalid("")
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
        High,
        Invalid
    }
    enum Context {
        News,
        Shopping,
        Social,
        Media,
        Blog,
        Hobbies,
        Travel,
        Invalid
    }
}
