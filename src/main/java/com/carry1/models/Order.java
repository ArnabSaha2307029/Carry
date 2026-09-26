package com.carry1.models;

public class Order {
    private String orderId;
    private String customerId;
    private String travelerId;

    private String itemType;
    private String itemName; // as Product Description
    private double weight;

    private String senderName;
    private String senderPhone;
    private String pickupLocation;
    private String pickupInfo;

    private String receiverName;
    private String receiverPhone;
    private String dropoffLocation;
    private String dropoffInfo;

    private double distanceKm;
    private double rewardAmount; // Final Delivery Fee
    private OrderStatus status;

    public Order(String orderId, String customerId, String travelerId, String itemType, String itemName, double weight,
                 String senderName, String senderPhone, String pickupLocation, String pickupInfo,
                 String receiverName, String receiverPhone, String dropoffLocation, String dropoffInfo,
                 double distanceKm, double rewardAmount, OrderStatus status) {
        this.orderId = orderId; this.customerId = customerId; this.travelerId = travelerId;
        this.itemType = itemType; this.itemName = itemName; this.weight = weight;
        this.senderName = senderName; this.senderPhone = senderPhone; this.pickupLocation = pickupLocation; this.pickupInfo = pickupInfo;
        this.receiverName = receiverName; this.receiverPhone = receiverPhone; this.dropoffLocation = dropoffLocation; this.dropoffInfo = dropoffInfo;
        this.distanceKm = distanceKm; this.rewardAmount = rewardAmount; this.status = status;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getTravelerId() { return travelerId; }
    public String getItemType() { return itemType; }
    public String getItemName() { return itemName; }
    public double getWeight() { return weight; }
    public String getSenderName() { return senderName; }
    public String getSenderPhone() { return senderPhone; }
    public String getPickupLocation() { return pickupLocation; }
    public String getPickupInfo() { return pickupInfo; }
    public String getReceiverName() { return receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public String getDropoffLocation() { return dropoffLocation; }
    public String getDropoffInfo() { return dropoffInfo; }
    public double getDistanceKm() { return distanceKm; }
    public double getRewardAmount() { return rewardAmount; }
    public OrderStatus getStatus() { return status; }
}