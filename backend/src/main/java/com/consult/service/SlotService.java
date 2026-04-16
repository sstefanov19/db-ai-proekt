package com.consult.service;

import com.consult.exception.BadRequestException;
import com.consult.exception.ConflictException;
import com.consult.exception.ForbiddenException;
import com.consult.exception.NotFoundException;
import com.consult.model.BookingStatus;
import com.consult.model.ConsultationSlot;
import com.consult.model.Role;
import com.consult.model.SlotStatus;
import com.consult.model.User;
import com.consult.repo.BookingRepository;
import com.consult.repo.ConsultationSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SlotService {

    private final ConsultationSlotRepository slotRepo;
    private final BookingRepository bookingRepo;

    public SlotService(ConsultationSlotRepository slotRepo, BookingRepository bookingRepo) {
        this.slotRepo = slotRepo;
        this.bookingRepo = bookingRepo;
    }

    @Transactional
    public ConsultationSlot create(User teacher, LocalDateTime startAt, LocalDateTime endAt, String location) {
        if (teacher.getRole() != Role.TEACHER) {
            throw new ForbiddenException("Само преподавател може да създава слотове.");
        }
        if (!endAt.isAfter(startAt)) {
            throw new BadRequestException("endAt трябва да е след startAt.");
        }
        List<ConsultationSlot> overlapping = slotRepo.findOverlapping(teacher, startAt, endAt);
        if (!overlapping.isEmpty()) {
            throw new ConflictException("Слотът се припокрива с друг ваш слот.");
        }
        ConsultationSlot slot = new ConsultationSlot(teacher, startAt, endAt, location);
        return slotRepo.save(slot);
    }

    @Transactional
    public void delete(User teacher, Long slotId) {
        ConsultationSlot slot = slotRepo.findById(slotId)
            .orElseThrow(() -> new NotFoundException("Слотът не съществува."));
        if (!slot.getTeacher().getId().equals(teacher.getId())) {
            throw new ForbiddenException("Не можете да изтриете чужд слот.");
        }
        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new ConflictException("Слотът е резервиран и не може да бъде изтрит.");
        }
        slotRepo.delete(slot);
    }

    @Transactional(readOnly = true)
    public List<ConsultationSlot> listAvailable(Long teacherId, LocalDateTime from, LocalDateTime to) {
        return slotRepo.search(SlotStatus.AVAILABLE, teacherId, from, to);
    }

    @Transactional(readOnly = true)
    public List<ConsultationSlot> listForTeacher(Long teacherId) {
        return slotRepo.findByTeacherId(teacherId);
    }

    @Transactional(readOnly = true)
    public String bookedByName(ConsultationSlot slot) {
        if (slot.getStatus() != SlotStatus.BOOKED) return null;
        return bookingRepo.findBySlotAndStatus(slot, BookingStatus.ACTIVE)
            .map(b -> b.getStudent().getFullName())
            .orElse(null);
    }
}
