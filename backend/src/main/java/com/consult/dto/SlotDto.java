package com.consult.dto;

import com.consult.model.ConsultationSlot;
import com.consult.model.SlotStatus;

import java.time.LocalDateTime;

public record SlotDto(
    Long id,
    Long teacherId,
    String teacherName,
    LocalDateTime startAt,
    LocalDateTime endAt,
    String location,
    SlotStatus status,
    String bookedByName
) {
    public static SlotDto from(ConsultationSlot s, String bookedByName) {
        return new SlotDto(
            s.getId(),
            s.getTeacher().getId(),
            s.getTeacher().getFullName(),
            s.getStartAt(),
            s.getEndAt(),
            s.getLocation(),
            s.getStatus(),
            bookedByName
        );
    }

    public static SlotDto from(ConsultationSlot s) {
        return from(s, null);
    }
}
