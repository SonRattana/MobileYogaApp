package com.example.yogaclass;

public class ClassInstance {
    private String id;
    private String yogaClassId;
    private String date;
    private String teacher;
    private String additionalComments;
    private double price;


    public ClassInstance(String id, String yogaClassId, String date, String teacher, String additionalComments, double price) {
        this.id = id;
        this.yogaClassId = yogaClassId;
        this.date = date;
        this.teacher = teacher;
        this.additionalComments = additionalComments;
        this.price = price;
    }


    public String getInstanceId() {
        return id;
    }

    public void setInstanceId(String id) {
        this.id = id;
    }

    public String getYogaClassId() {
        return yogaClassId;
    }

    public void setYogaClassId(String yogaClassId) {
        this.yogaClassId = yogaClassId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getAdditionalComments() {
        return additionalComments;
    }

    public void setAdditionalComments(String additionalComments) {
        this.additionalComments = additionalComments;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
