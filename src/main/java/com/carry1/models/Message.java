package com.carry1.models;

public class Message {
    private int id;
    private String orderId;
    private String senderId;
    private String receiverId;
    private String text;
    private long timestamp;
    private boolean isRead;

    public Message(int id, String orderId, String senderId, String receiverId, String text, long timestamp, boolean isRead) {
        this.id = id;
        this.orderId = orderId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public int getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
}