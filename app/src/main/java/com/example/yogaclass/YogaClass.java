package com.example.yogaclass;

public class YogaClass {
    private String id;
    private String dayOfWeek;
    private String time;
    private int quantity;
    private int duration;
    private String type;
    private double price;
    private String description;

    // Constructor không đối số (no-argument constructor)
    public YogaClass() {
        // Constructor mặc định để Firebase có thể khởi tạo đối tượng
    }

    // Constructor có đối số để khởi tạo đối tượng với dữ liệu
    public YogaClass(String id, String dayOfWeek, String time, int quantity, int duration, String type, double price, String description) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.time = time;
        this.quantity = quantity;
        this.duration = duration;
        this.type = type;
        this.price = price;

        this.description = description;
    }

    // Getter và Setter cho các trường dữ liệu
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
