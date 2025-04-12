package com.phonepe.tradingexchange.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class User {
    private final String userId;
    private String name;
    private String email;
    
    public static User createUser(String name, String email) {
        return User.builder()
                .userId(UUID.randomUUID().toString())
                .name(name)
                .email(email)
                .build();
    }
} 