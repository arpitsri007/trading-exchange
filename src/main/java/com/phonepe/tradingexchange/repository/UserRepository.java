package com.phonepe.tradingexchange.repository;

import com.phonepe.tradingexchange.model.User;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepository {
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    
    private static UserRepository INSTANCE;
    
    private UserRepository() {
    }
    
    public static UserRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (UserRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UserRepository();
                }
            }
        }
        return INSTANCE;
    }
    
    public void addUser(User user) {
        users.put(user.getUserId(), user);
    }
    
    public boolean existsById(String userId) {
        return users.containsKey(userId);
    }
    
} 