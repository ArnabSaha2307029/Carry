package com.carry1.models;

public class Complaint {
    private int id;
    private String orderId;
    private String creatorId;
    private String againstId;
    private String status;

    public Complaint(int id, String orderId, String creatorId, String againstId, String status) {
        this.id = id;
        this.orderId = orderId;
        this.creatorId = creatorId;
        this.againstId = againstId;
        this.status = status;
    }

    public int getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getCreatorId() { return creatorId; }
    public String getAgainstId() { return againstId; }
    public String getStatus() { return status; }
}
