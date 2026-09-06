package com.teachquest.controller;

import com.teachquest.model.User;

public class AuthUserResponse {
    private Long id;
    private String username;
    private String universityId;
    private String contactNo;
    private String email;
    private String address;
    private String userType;
    private int totalPoints;

    public AuthUserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.universityId = user.getUniversityId();
        this.contactNo = user.getContactNo();
        this.email = user.getEmail();
        this.address = user.getAddress();
        this.userType = user.getUserType();
        this.totalPoints = user.getTotalPoints();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getUniversityId() { return universityId; }
    public String getContactNo() { return contactNo; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public String getUserType() { return userType; }
    public int getTotalPoints() { return totalPoints; }
}
