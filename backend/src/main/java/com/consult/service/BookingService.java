package com.consult.service;

import com.consult.exception.ConflictException;
import com.consult.exception.ForbiddenException;
import com.consult.exception.NotFoundException;
import com.consult.model.Booking;
import com.consult.model.BookingStatus;
import com.consult.model.ConsultationSlot;
import com.consult.model.Role;
import com.consult.model.SlotStatus;
import com.consult.model.User;
import com.consult.repo.BookingRepository;
import com.consult.repo.ConsultationSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookingService {

    private final ConsultationSlotRepository slotRepo;
    private final BookingRepository bookingRepo;

    public BookingService(ConsultationSlotRepository slotRepo, BookingRepository bookingRepo) {
        this.slotRepo = slotRepo;
        this.bookingRepo = bookingRepo;
    }

    @Transactional
    public Booking book(User student, Long slotId) {
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Само студент може да се записва.");
        }
        ConsultationSlot slot = slotRepo.findByIdForUpdate(slotId)
            .orElseThrow(() -> new NotFoundException("Слотът не съществува."));
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new ConflictException("Слотът вече не е свободен.");
        }
        slot.setStatus(SlotStatus.BOOKED);
        Booking booking = new Booking(slot, student);
        return bookingRepo.save(booking);
    }

    @Transactional
    public void cancel(User student, Long bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Записването не съществува."));
        if (!booking.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("Не можете да отменяте чуждо записване.");
        }
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new ConflictException("Записването вече е отменено.");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.getSlot().setStatus(SlotStatus.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public List<Booking> myBookings(User student) {
        return bookingRepo.findMyBookings(student);
    }

    @Transactional(readOnly = true)
    public List<Booking> all() {
        return bookingRepo.findAllWithJoins();
    }
}
