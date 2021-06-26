package com.inkhornsolutions.chjrider.Models;

public class DriverInfoModel {
    private String firstName, lastName, phoneNumber, driverProfileImage;
    private double rating;

    public DriverInfoModel() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getDriverProfileImage() {
        return driverProfileImage;
    }

    public void setDriverProfileImage(String driverProfileImage) {
        this.driverProfileImage = driverProfileImage;
    }
}