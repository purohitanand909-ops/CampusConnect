package com.campusconnect.repository;

import com.campusconnect.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    /** Check if a non-cancelled registration exists for this student+event pair. */
    boolean existsByStudentIdAndEventIdAndStatusNot(
            Long studentId, Long eventId, Registration.Status status);

    /** Count confirmed registrations for an event (used for capacity check). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    long countConfirmedByEventId(@Param("eventId") Long eventId);

    /** Count confirmed registrations without lock (for read-only queries and display). */
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    long countConfirmedByEventIdReadOnly(@Param("eventId") Long eventId);

    /** Grouped counts of confirmed registrations per event for bulk listing. */
    @Query("SELECT r.event.id, COUNT(r) FROM Registration r WHERE r.status = 'CONFIRMED' GROUP BY r.event.id")
    List<Object[]> countConfirmedRegistrationsGrouped();

    /** Get all registrations for an event, ordered for display. */
    @Query("""
            SELECT r FROM Registration r
            JOIN FETCH r.student
            WHERE r.event.id = :eventId
            ORDER BY r.status ASC, r.waitlistPosition ASC, r.registeredAt ASC
            """)
    List<Registration> findByEventIdWithStudent(@Param("eventId") Long eventId);

    /** Get all registrations for a student. */
    @Query("""
            SELECT r FROM Registration r
            JOIN FETCH r.event
            WHERE r.student.id = :studentId
            ORDER BY r.registeredAt DESC
            """)
    List<Registration> findByStudentIdWithEvent(@Param("studentId") Long studentId);

    /** Find the first waitlisted registration for an event (lowest position). */
    @Query("""
            SELECT r FROM Registration r
            WHERE r.event.id = :eventId AND r.status = 'WAITLISTED'
            ORDER BY r.waitlistPosition ASC
            LIMIT 1
            """)
    Optional<Registration> findFirstWaitlisted(@Param("eventId") Long eventId);

    /** Count waitlisted registrations for an event (to assign next position). */
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.event.id = :eventId AND r.status = 'WAITLISTED'")
    long countWaitlistedByEventId(@Param("eventId") Long eventId);

    /** Find an existing non-cancelled registration for a student+event. */
    Optional<Registration> findByStudentIdAndEventIdAndStatusNot(
            Long studentId, Long eventId, Registration.Status status);

    /** Find an existing registration by status for a student+event (used for re-registration). */
    Optional<Registration> findByStudentIdAndEventIdAndStatus(
            Long studentId, Long eventId, Registration.Status status);
}
