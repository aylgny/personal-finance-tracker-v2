package com.subtrack.backend.auth;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@Service
@RequestScope
public class CurrentUserService {

    private Long userId;
    private String email;

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public void setCurrentUser(Long userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public boolean isAuthenticated() {
        return userId != null;
    }
}