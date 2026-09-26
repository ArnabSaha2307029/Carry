package com.carry1.models;

public class ComplaintMessage {
    private int id;
    private int complaintId;
    private String senderId;
    private String messageText;
    private long timestamp;

    public ComplaintMessage(int id, int complaintId, String senderId, String messageText, long timestamp) {
        this.id = id;
        this.complaintId = complaintId;
        this.senderId = senderId;
        this.messageText = messageText;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public int getComplaintId() { return complaintId; }
    public String getSenderId() { return senderId; }
    public String getMessageText() { return messageText; }
    public long getTimestamp() { return timestamp; }
}
