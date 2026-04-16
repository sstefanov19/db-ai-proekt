package com.consult.controller;

import com.consult.dto.BookingDto;
import com.consult.dto.UserDto;
import com.consult.repo.UserRepository;
import com.consult.service.BookingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepo;
    private final BookingService bookingService;

    public AdminController(UserRepository userRepo, BookingService bookingService) {
        this.userRepo = userRepo;
        this.bookingService = bookingService;
    }

    @GetMapping("/users")
    public List<UserDto> users() {
        return userRepo.findAllByOrderByRoleAscFullNameAsc()
            .stream().map(UserDto::from).toList();
    }

    @GetMapping("/bookings")
    public List<BookingDto> bookings() {
        return bookingService.all().stream().map(BookingDto::from).toList();
    }
}
