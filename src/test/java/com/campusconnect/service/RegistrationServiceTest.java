package com.campusconnect.service;

import com.campusconnect.dto.RegistrationRequest;
import com.campusconnect.dto.RegistrationResponse;
import com.campusconnect.entity.Event;
import com.campusconnect.entity.Registration;
import com.campusconnect.entity.User;
import com.campusconnect.exception.BadRequestException;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.RegistrationRepository;
import com.campusconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock private RegistrationRepository registrationRepository;
    @Mock private EventRepository        eventRepository;
    @Mock private UserRepository         userRepository;

    @InjectMocks
    private RegistrationService registrationService;

    private User   student;
    private Event  event;

    @BeforeEach
    void setUp() {
        User organizer = new User("Dr. Sharma", "sharma@campus.edu", User.Role.ORGANIZER);
        setId(organizer, 1L);

        student = new User("Anand", "anand@student.edu", User.Role.STUDENT);
        setId(student, 2L);

        event = new Event();
        setId(event, 10L);
        event.setTitle("Tech Fest");
        event.setCapacity(2);
        event.setStatus(Event.Status.UPCOMING);
        event.setStartTime(LocalDateTime.now().plusDays(10));
        event.setRegistrationDeadline(LocalDateTime.now().plusDays(8));
        event.setOrganizer(organizer);
        event.setCategory("Technology");
        event.setLocation("Main Hall");
    }

    // ── Test: successful registration ─────────────────────────

    @Test
    void register_whenSeatAvailable_returnsConfirmed() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByStudentIdAndEventIdAndStatusNot(2L, 10L, Registration.Status.CANCELLED))
                .thenReturn(false);
        when(registrationRepository.countConfirmedByEventId(10L)).thenReturn(0L);

        Registration saved = new Registration(student, event, Registration.Status.CONFIRMED, null);
        setId(saved, 100L);
        saved.setRegisteredAt(LocalDateTime.now());
        when(registrationRepository.save(any())).thenReturn(saved);

        RegistrationResponse response = registrationService.register(request(2L, 10L));

        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getWaitlistPosition()).isNull();
    }

    // ── Test: re-registration after cancellation ──────────────

    @Test
    void register_whenPreviouslyCancelled_reusesExistingRowAndConfirms() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByStudentIdAndEventIdAndStatusNot(2L, 10L, Registration.Status.CANCELLED))
                .thenReturn(false);
        when(registrationRepository.countConfirmedByEventId(10L)).thenReturn(0L);

        Registration cancelledReg = new Registration(student, event, Registration.Status.CANCELLED, null);
        setId(cancelledReg, 105L);
        when(registrationRepository.findByStudentIdAndEventIdAndStatus(2L, 10L, Registration.Status.CANCELLED))
                .thenReturn(Optional.of(cancelledReg));
        when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegistrationResponse response = registrationService.register(request(2L, 10L));

        assertThat(response.getId()).isEqualTo(105L);
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getWaitlistPosition()).isNull();
    }

    // ── Test: waitlist when event full ────────────────────────

    @Test
    void register_whenEventFull_returnsWaitlisted() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByStudentIdAndEventIdAndStatusNot(2L, 10L, Registration.Status.CANCELLED))
                .thenReturn(false);
        when(registrationRepository.countConfirmedByEventId(10L)).thenReturn(2L); // at capacity
        when(registrationRepository.countWaitlistedByEventId(10L)).thenReturn(0L);

        Registration saved = new Registration(student, event, Registration.Status.WAITLISTED, 1);
        setId(saved, 101L);
        saved.setRegisteredAt(LocalDateTime.now());
        when(registrationRepository.save(any())).thenReturn(saved);

        RegistrationResponse response = registrationService.register(request(2L, 10L));

        assertThat(response.getStatus()).isEqualTo("WAITLISTED");
        assertThat(response.getWaitlistPosition()).isEqualTo(1);
    }

    // ── Test: duplicate registration rejected ─────────────────

    @Test
    void register_whenAlreadyRegistered_throwsBadRequest() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByStudentIdAndEventIdAndStatusNot(2L, 10L, Registration.Status.CANCELLED))
                .thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request(2L, 10L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");
    }

    // ── Test: registration deadline passed ───────────────────

    @Test
    void register_whenDeadlinePassed_throwsBadRequest() {
        event.setRegistrationDeadline(LocalDateTime.now().minusHours(1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        // No stub for existsByStudentIdAndEventIdAndStatusNot —
        // the service throws at validateEventOpen() before reaching that call.

        assertThatThrownBy(() -> registrationService.register(request(2L, 10L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("deadline has passed");
    }

    // ── Test: cancel confirmed promotes waitlisted ────────────

    @Test
    void cancelRegistration_whenConfirmed_promotesFirstWaitlisted() {
        Registration confirmed = new Registration(student, event, Registration.Status.CONFIRMED, null);
        setId(confirmed, 200L);
        confirmed.setRegisteredAt(LocalDateTime.now().minusHours(1));

        User waiter = new User("Riya", "riya@student.edu", User.Role.STUDENT);
        setId(waiter, 3L);
        Registration waitlisted = new Registration(waiter, event, Registration.Status.WAITLISTED, 1);
        setId(waitlisted, 201L);
        waitlisted.setRegisteredAt(LocalDateTime.now().minusMinutes(30));

        when(registrationRepository.findById(200L)).thenReturn(Optional.of(confirmed));
        when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(registrationRepository.findFirstWaitlisted(10L)).thenReturn(Optional.of(waitlisted));
        when(registrationRepository.findByEventIdWithStudent(10L)).thenReturn(java.util.List.of());

        RegistrationResponse response = registrationService.cancelRegistration(200L);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        assertThat(waitlisted.getStatus()).isEqualTo(Registration.Status.CONFIRMED);
        assertThat(waitlisted.getWaitlistPosition()).isNull();
    }

    // ── Helpers ───────────────────────────────────────────────

    private RegistrationRequest request(Long studentId, Long eventId) {
        RegistrationRequest req = new RegistrationRequest();
        req.setStudentId(studentId);
        req.setEventId(eventId);
        return req;
    }

    /** Reflectively sets the id field (avoids needing a setter on entities). */
    private void setId(Object obj, Long id) {
        try {
            var field = obj.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(obj, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
