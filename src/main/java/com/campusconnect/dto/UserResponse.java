package com.campusconnect.dto;

import com.campusconnect.entity.User;

public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String role;

    public static UserResponse from(User user) {
        UserResponse dto = new UserResponse();
        dto.id    = user.getId();
        dto.name  = user.getName();
        dto.email = user.getEmail();
        dto.role  = user.getRole().name();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
