package com.example.yogaclass;

public class Booking {
    private String UserEmail;
    private SeasonDetails SeasonDetails;

    public Booking() {
        // Default constructor required for calls to DataSnapshot.getValue(Booking.class)
    }

    public Booking(String userEmail, SeasonDetails seasonDetails) {
        UserEmail = userEmail;
        SeasonDetails = seasonDetails;
    }

    public String getUserEmail() {
        return UserEmail;
    }

    public void setUserEmail(String userEmail) {
        UserEmail = userEmail;
    }

    public SeasonDetails getSeasonDetails() {
        return SeasonDetails;
    }

    public void setSeasonDetails(SeasonDetails seasonDetails) {
        SeasonDetails = seasonDetails;
    }
}

class SeasonDetails {
    private String Id;
    private String Date;
    private String Teacher;
    private double Price;
    private String AdditionalComments;

    public SeasonDetails() {
        // Default constructor required for calls to DataSnapshot.getValue(SeasonDetails.class)
    }

    public SeasonDetails(String id, String date, String teacher, double price, String additionalComments) {
        Id = id;
        Date = date;
        Teacher = teacher;
        Price = price;
        AdditionalComments = additionalComments;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getDate() {
        return Date;
    }

    public void setDate(String date) {
        Date = date;
    }

    public String getTeacher() {
        return Teacher;
    }

    public void setTeacher(String teacher) {
        Teacher = teacher;
    }

    public double getPrice() {
        return Price;
    }

    public void setPrice(double price) {
        Price = price;
    }

    public String getAdditionalComments() {
        return AdditionalComments;
    }

    public void setAdditionalComments(String additionalComments) {
        AdditionalComments = additionalComments;
    }
}
