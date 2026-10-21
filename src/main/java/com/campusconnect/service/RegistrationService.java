package com.campusconnect.service;

import com.campusconnect.dto.RegistrationRequest;
import com.campusconnect.dto.RegistrationResponse;
import com.campusconnect.entity.Event;
import com.campusconnect.entity.Registration;
import com.campusconnect.entity.User;
import com.campusconnect.exception.BadRequestException;
import com.campusconnect.exception.ResourceNotFoundException;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.RegistrationRepository;
import com.campusconnect.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository        eventRepository;
    private final UserRepository         userRepository;

    public RegistrationService(RegistrationRepository registrationRepository,
                               EventRepository eventRepository,
                               UserRepository userRepository) {
        this.registrationRepository = registrationRepository;
        this.eventRepository        = eventRepository;
        this.userRepository         = userRepository;
    }

    // ── Register a student for an event ──────────────────────

    /**
     * Registers a student for an event.
     *  - Validates event is UPCOMING and deadline has not passed.
     *  - Prevents duplicate active registrations.
     *  - Enforces capacity with a pessimistic DB lock.
     *  - Adds student to waitlist if event is full.
     */
    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        User student = findStudentOrThrow(request.getStudentId());
        // Lock the event row to serialize concurrent registrations
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found with id: " + request.getEventId()));

        validateEventOpen(event);

        // Check for existing active (non-cancelled) registration
        boolean alreadyRegistered = registrationRepository
                .existsByStudentIdAndEventIdAndStatusNot(
                        student.getId(), event.getId(), Registration.Status.CANCELLED);
        if (alreadyRegistered) {
            throw new BadRequestException("You are already registered or waitlisted for this event.");
        }

        // Count confirmed seats with a pessimistic write lock
        long confirmedCount = registrationRepository.countConfirmedByEventId(event.getId());

        Registration registration;
        if (confirmedCount < event.getCapacity()) {
            // Seat available
            registration = new Registration(student, event, Registration.Status.CONFIRMED, null);
        } else {
            // Event full — add to waitlist
            long waitlistCount = registrationRepository.countWaitlistedByEventId(event.getId());
            registration = new Registration(
                    student, event,
                    Registration.Status.WAITLISTED,
                    (int) (waitlistCount + 1));
        }

        return RegistrationResponse.from(registrationRepository.save(registration));
    }

    // ── Cancel a registration ────────────────────────────────

    /**
     * Cancels a registration.
     *  - If the cancelled registration was CONFIRMED, promotes the first
     *    waitlisted student and renumbers the remaining waitlist.
     */
    @Transactional
    public RegistrationResponse cancelRegistration(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registration not found with id: " + registrationId));

        if (registration.getStatus() == Registration.Status.CANCELLED) {
            throw new BadRequestException("Registration is already cancelled.");
        }

        boolean wasConfirmed = registration.getStatus() == Registration.Status.CONFIRMED;
        registration.setStatus(Registration.Status.CANCELLED);
        registration.setWaitlistPosition(null);
        registrationRepository.save(registration);

        // If a confirmed seat was freed, promote the first waitlisted student
        if (wasConfirmed) {
            promoteFirstWaitlisted(registration.getEvent().getId());
        }

        return RegistrationResponse.from(registration);
    }

    // ── View event participants ───────────────────────────────

    @Transactional(readOnly = true)
    public List<RegistrationResponse> getEventRegistrations(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }
        return registrationRepository.findByEventIdWithStudent(eventId)
                .stream()
                .map(RegistrationResponse::from)
                .toList();
    }

    // ── View student registrations ───────────────────────────

    @Transactional(readOnly = true)
    public List<RegistrationResponse> getStudentRegistrations(Long studentId) {
        if (!userRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
        return registrationRepository.findByStudentIdWithEvent(studentId)
                .stream()
                .map(RegistrationResponse::from)
                .toList();
    }

    // ── Private helpers ──────────────────────────────────────

    private User findStudentOrThrow(Long studentId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found with id: " + studentId));
        if (user.getRole() != User.Role.STUDENT) {
            throw new BadRequestException("User " + studentId + " is not a student.");
        }
        return user;
    }

    private void validateEventOpen(Event event) {
        if (event.getStatus() == Event.Status.CANCELLED) {
            throw new BadRequestException("Cannot register for a cancelled event.");
        }
        if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new BadRequestException("Registration deadline has passed for this event.");
        }
    }

    /**
     * Promotes the earliest waitlisted student to CONFIRMED after a seat is freed.
     * Also decrements the waitlist position of all remaining waitlisted students
     * so positions stay contiguous.
     */
    private void promoteFirstWaitlisted(Long eventId) {
        registrationRepository.findFirstWaitlisted(eventId).ifPresent(first -> {
            first.setStatus(Registration.Status.CONFIRMED);
            first.setWaitlistPosition(null);
            registrationRepository.save(first);

            // Renumber remaining waitlisted registrations
            List<Registration> remaining = registrationRepository
                    .findByEventIdWithStudent(eventId)
                    .stream()
                    .filter(r -> r.getStatus() == Registration.Status.WAITLISTED)
                    .sorted(java.util.Comparator.comparingInt(Registration::getWaitlistPosition))
                    .toList();

            int position = 1;
            for (Registration r : remaining) {
                r.setWaitlistPosition(position++);
                registrationRepository.save(r);
            }
        });
    }
}
