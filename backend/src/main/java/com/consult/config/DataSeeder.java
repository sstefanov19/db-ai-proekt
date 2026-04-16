package com.consult.config;

import com.consult.model.ConsultationSlot;
import com.consult.model.Role;
import com.consult.model.User;
import com.consult.repo.ConsultationSlotRepository;
import com.consult.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final ConsultationSlotRepository slotRepo;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository userRepo,
                      ConsultationSlotRepository slotRepo,
                      PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.slotRepo = slotRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        String pwd = encoder.encode("password");

        User admin = userRepo.save(new User("Админ Админов", "admin@consult.bg", pwd, Role.ADMIN));
        User t1 = userRepo.save(new User("Иван Петров", "teacher1@consult.bg", pwd, Role.TEACHER));
        User t2 = userRepo.save(new User("Мария Георгиева", "teacher2@consult.bg", pwd, Role.TEACHER));
        User s1 = userRepo.save(new User("Георги Иванов", "student1@consult.bg", pwd, Role.STUDENT));
        User s2 = userRepo.save(new User("Петя Димитрова", "student2@consult.bg", pwd, Role.STUDENT));
        User s3 = userRepo.save(new User("Николай Стоянов", "student3@consult.bg", pwd, Role.STUDENT));

        LocalDate today = LocalDate.now();
        slotRepo.save(new ConsultationSlot(t1,
            LocalDateTime.of(today.plusDays(1), LocalTime.of(10, 0)),
            LocalDateTime.of(today.plusDays(1), LocalTime.of(10, 30)),
            "Зала 301"));
        slotRepo.save(new ConsultationSlot(t1,
            LocalDateTime.of(today.plusDays(1), LocalTime.of(10, 30)),
            LocalDateTime.of(today.plusDays(1), LocalTime.of(11, 0)),
            "Зала 301"));
        slotRepo.save(new ConsultationSlot(t1,
            LocalDateTime.of(today.plusDays(2), LocalTime.of(14, 0)),
            LocalDateTime.of(today.plusDays(2), LocalTime.of(14, 30)),
            "Zoom"));
        slotRepo.save(new ConsultationSlot(t2,
            LocalDateTime.of(today.plusDays(1), LocalTime.of(13, 0)),
            LocalDateTime.of(today.plusDays(1), LocalTime.of(13, 30)),
            "Зала 205"));
        slotRepo.save(new ConsultationSlot(t2,
            LocalDateTime.of(today.plusDays(3), LocalTime.of(9, 0)),
            LocalDateTime.of(today.plusDays(3), LocalTime.of(9, 30)),
            "Зала 205"));
    }
}
