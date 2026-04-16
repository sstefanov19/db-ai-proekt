package com.consult.repo;

import com.consult.model.ConsultationSlot;
import com.consult.model.SlotStatus;
import com.consult.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConsultationSlotRepository extends JpaRepository<ConsultationSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ConsultationSlot s where s.id = :id")
    Optional<ConsultationSlot> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        select s from ConsultationSlot s
         where s.teacher = :teacher
           and s.status <> com.consult.model.SlotStatus.CANCELLED
           and s.startAt < :endAt
           and s.endAt > :startAt
    """)
    List<ConsultationSlot> findOverlapping(
        @Param("teacher") User teacher,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt
    );

    @Query("""
        select s from ConsultationSlot s
         where s.teacher.id = :teacherId
         order by s.startAt asc
    """)
    List<ConsultationSlot> findByTeacherId(@Param("teacherId") Long teacherId);

    @Query("""
        select s from ConsultationSlot s
         where s.status = :status
           and (:teacherId is null or s.teacher.id = :teacherId)
           and (:from is null or s.startAt >= :from)
           and (:to is null or s.startAt < :to)
         order by s.startAt asc
    """)
    List<ConsultationSlot> search(
        @Param("status") SlotStatus status,
        @Param("teacherId") Long teacherId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}
