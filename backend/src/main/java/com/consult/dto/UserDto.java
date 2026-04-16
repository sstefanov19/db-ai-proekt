package com.consult.dto;

import com.consult.model.Role;
import com.consult.model.User;

public record UserDto(Long id, String fullName, String email, Role role) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getRole());
    }
}
