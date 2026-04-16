package com.consult.controller;

import com.consult.dto.TeacherDto;
import com.consult.model.Role;
import com.consult.repo.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final UserRepository userRepo;

    public TeacherController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<TeacherDto> list() {
        return userRepo.findAllByRoleOrderByFullNameAsc(Role.TEACHER)
            .stream().map(TeacherDto::from).toList();
    }
}
