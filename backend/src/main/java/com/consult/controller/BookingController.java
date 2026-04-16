package com.consult.controller;

import com.consult.dto.BookingDto;
import com.consult.dto.CreateBookingRequest;
import com.consult.security.AppUserDetails;
import com.consult.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public List<BookingDto> mine(@AuthenticationPrincipal AppUserDetails principal) {
        return bookingService.myBookings(principal.getUser())
            .stream().map(BookingDto::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public BookingDto book(@Valid @RequestBody CreateBookingRequest req,
                           @AuthenticationPrincipal AppUserDetails principal) {
        return BookingDto.from(bookingService.book(principal.getUser(), req.slotId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> cancel(@PathVariable Long id,
                                       @AuthenticationPrincipal AppUserDetails principal) {
        bookingService.cancel(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }
}
