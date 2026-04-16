package com.consult.controller;

import com.consult.dto.CreateSlotRequest;
import com.consult.dto.SlotDto;
import com.consult.model.ConsultationSlot;
import com.consult.model.Role;
import com.consult.security.AppUserDetails;
import com.consult.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping
    public List<SlotDto> list(
        @RequestParam(required = false) Long teacherId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @AuthenticationPrincipal AppUserDetails principal
    ) {
        List<ConsultationSlot> slots;
        if (principal.getUser().getRole() == Role.TEACHER) {
            slots = slotService.listForTeacher(principal.getUser().getId());
        } else {
            slots = slotService.listAvailable(teacherId, from, to);
        }
        return slots.stream()
            .map(s -> SlotDto.from(s, slotService.bookedByName(s)))
            .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public SlotDto create(@Valid @RequestBody CreateSlotRequest req,
                          @AuthenticationPrincipal AppUserDetails principal) {
        ConsultationSlot slot = slotService.create(
            principal.getUser(), req.startAt(), req.endAt(), req.location());
        return SlotDto.from(slot);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal AppUserDetails principal) {
        slotService.delete(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }
}
