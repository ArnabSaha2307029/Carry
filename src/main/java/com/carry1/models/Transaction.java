package com.carry1.models;

public class Transaction {
    private int id;
    private String orderId;
    private String senderId;
    private String receiverId;
    private double amount;
    private String type;
    private String timestamp;

    public Transaction(int id, String orderId, String senderId, String receiverId, double amount, String type, String timestamp) {
        this.id = id;
        this.orderId = orderId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public String getTimestamp() { return timestamp; }
}
