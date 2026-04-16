package com.consult.dto;

import com.consult.model.Role;
import com.consult.model.User;

public record MeResponse(Long id, String fullName, String email, Role role) {
    public static MeResponse from(User u) {
        return new MeResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole());
    }
}
