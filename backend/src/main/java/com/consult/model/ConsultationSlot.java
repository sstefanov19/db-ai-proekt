package com.consult.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultation_slots")
public class ConsultationSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SlotStatus status;

    @Version
    private Long version;

    public ConsultationSlot() {}

    public ConsultationSlot(User teacher, LocalDateTime startAt, LocalDateTime endAt, String location) {
        this.teacher = teacher;
        this.startAt = startAt;
        this.endAt = endAt;
        this.location = location;
        this.status = SlotStatus.AVAILABLE;
    }

    public Long getId() { return id; }
    public User getTeacher() { return teacher; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public String getLocation() { return location; }
    public SlotStatus getStatus() { return status; }
    public Long getVersion() { return version; }

    public void setTeacher(User teacher) { this.teacher = teacher; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public void setLocation(String location) { this.location = location; }
    public void setStatus(SlotStatus status) { this.status = status; }
}
