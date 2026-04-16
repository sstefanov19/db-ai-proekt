package com.consult.repo;

import com.consult.model.Booking;
import com.consult.model.BookingStatus;
import com.consult.model.ConsultationSlot;
import com.consult.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStudentOrderByCreatedAtDesc(User student);

    Optional<Booking> findBySlotAndStatus(ConsultationSlot slot, BookingStatus status);

    @Query("""
        select b from Booking b
         join fetch b.slot s
         join fetch s.teacher
         join fetch b.student
         order by b.createdAt desc
    """)
    List<Booking> findAllWithJoins();

    @Query("""
        select b from Booking b
         join fetch b.slot s
         join fetch s.teacher
         join fetch b.student
         where b.student = :student
         order by s.startAt desc
    """)
    List<Booking> findMyBookings(@Param("student") User student);
}
