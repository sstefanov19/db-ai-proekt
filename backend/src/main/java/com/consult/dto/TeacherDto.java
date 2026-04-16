package com.consult.dto;

import com.consult.model.User;

public record TeacherDto(Long id, String fullName, String email) {
    public static TeacherDto from(User u) {
        return new TeacherDto(u.getId(), u.getFullName(), u.getEmail());
    }
}
