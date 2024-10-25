package com.example.yogaclass;

public class User {
    private String id; // ID của người dùng
    private String name;
    private String email;
    private String password; // Nếu cần thiết
    private String role;     // Thêm thuộc tính role

    public User() {
        // Constructor mặc định cho Firebase
    }

    public User(String id, String name, String email, String password, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;  // Gán giá trị role
    }

    // Getter và Setter cho các thuộc tính
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    // Getter và Setter cho role
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
