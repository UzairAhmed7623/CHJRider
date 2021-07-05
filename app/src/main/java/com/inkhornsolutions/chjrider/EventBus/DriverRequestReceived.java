package com.inkhornsolutions.chjrider.EventBus;

public class DriverRequestReceived {
    private String key;
    private String pickupLocation, pickupLocationString;
    private String destinationLocation, destinationLocationString;
    private String dropOffUserId;
    private String orderId;

    public DriverRequestReceived() {
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getDropOffUserId() {
        return dropOffUserId;
    }

    public void setDropOffUserId(String dropOffUserId) {
        this.dropOffUserId = dropOffUserId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getPickupLocationString() {
        return pickupLocationString;
    }

    public void setPickupLocationString(String pickupLocationString) {
        this.pickupLocationString = pickupLocationString;
    }

    public String getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(String destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public String getDestinationLocationString() {
        return destinationLocationString;
    }

    public void setDestinationLocationString(String destinationLocationString) {
        this.destinationLocationString = destinationLocationString;
    }
}
