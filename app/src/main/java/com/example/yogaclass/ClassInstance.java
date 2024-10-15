package com.example.yogaclass;

public class ClassInstance {
    private String instanceId;
    private String yogaClassId;
    private String date;
    private String teacher;
    private String additionalComments;
    private double price; // Thêm trường price

    // Constructor mặc định cho Firebase
    public ClassInstance() {
    }

    // Constructor đầy đủ với 6 tham số bao gồm cả price
    public ClassInstance(String instanceId, String yogaClassId, String date, String teacher, String additionalComments, double price) {
        this.instanceId = instanceId;
        this.yogaClassId = yogaClassId;
        this.date = date;
        this.teacher = teacher;
        this.additionalComments = additionalComments;
        this.price = price;
    }

    // Các getter và setter cho tất cả các trường
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
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

