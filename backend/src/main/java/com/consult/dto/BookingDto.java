package com.consult.dto;

import com.consult.model.Booking;
import com.consult.model.BookingStatus;

import java.time.LocalDateTime;

public record BookingDto(
    Long id,
    SlotDto slot,
    Long studentId,
    String studentName,
    BookingStatus status,
    LocalDateTime createdAt
) {
    public static BookingDto from(Booking b) {
        return new BookingDto(
            b.getId(),
            SlotDto.from(b.getSlot(), b.getStudent().getFullName()),
            b.getStudent().getId(),
            b.getStudent().getFullName(),
            b.getStatus(),
            b.getCreatedAt()
        );
    }
}
