package com.carry1.models;
public class User {
    private String id;
    private String name;
    private Role role;
    private double balance;
    private String authToken;
    private String status; // ACTIVE or BANNED

    public User(String id, String name, Role role, double balance, String authToken, String status) {
        this.id = id; this.name = name; this.role = role; this.balance = balance; this.authToken = authToken; this.status = status;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public double getBalance() { return balance; }
    public String getAuthToken() { return authToken; }
    public String getStatus() { return status; }
}