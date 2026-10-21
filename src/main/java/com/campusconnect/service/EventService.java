package com.campusconnect.service;

import com.campusconnect.dto.EventRequest;
import com.campusconnect.dto.EventResponse;
import com.campusconnect.entity.Event;
import com.campusconnect.entity.User;
import com.campusconnect.exception.BadRequestException;
import com.campusconnect.exception.ResourceNotFoundException;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository  userRepository;

    public EventService(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository  = userRepository;
    }

    // ── List / Search ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EventResponse> listEvents(String keyword, String category) {
        // Pass null when the param is blank so the JPQL IS NULL condition matches
        String kw  = (keyword  == null || keyword.isBlank())  ? null : keyword.trim();
        String cat = (category == null || category.isBlank()) ? null : category.trim();
        return eventRepository.search(kw, cat)
                .stream()
                .map(EventResponse::from)
                .toList();
    }

    // ── Get by ID ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EventResponse getEvent(Long id) {
        Event event = findEventOrThrow(id);
        return EventResponse.from(event);
    }

    // ── Create ────────────────────────────────────────────────

    @Transactional
    public EventResponse createEvent(EventRequest request) {
        User organizer = findOrganizerOrThrow(request.getOrganizerId());
        validateEventDates(request.getRegistrationDeadline(), request.getStartTime());

        Event event = new Event();
        applyRequest(event, request, organizer);
        return EventResponse.from(eventRepository.save(event));
    }

    // ── Update ────────────────────────────────────────────────

    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = findEventOrThrow(id);
        if (event.getStatus() == Event.Status.CANCELLED) {
            throw new BadRequestException("Cannot update a cancelled event.");
        }
        User organizer = findOrganizerOrThrow(request.getOrganizerId());
        validateEventDates(request.getRegistrationDeadline(), request.getStartTime());

        applyRequest(event, request, organizer);
        return EventResponse.from(eventRepository.save(event));
    }

    // ── Cancel ────────────────────────────────────────────────

    @Transactional
    public EventResponse cancelEvent(Long id) {
        Event event = findEventOrThrow(id);
        if (event.getStatus() == Event.Status.CANCELLED) {
            throw new BadRequestException("Event is already cancelled.");
        }
        event.setStatus(Event.Status.CANCELLED);
        return EventResponse.from(eventRepository.save(event));
    }

    // ── Helpers ───────────────────────────────────────────────

    private Event findEventOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
    }

    private User findOrganizerOrThrow(Long organizerId) {
        User user = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + organizerId));
        if (user.getRole() != User.Role.ORGANIZER) {
            throw new BadRequestException("User " + organizerId + " is not an organizer.");
        }
        return user;
    }

    private void validateEventDates(LocalDateTime deadline, LocalDateTime startTime) {
        if (!deadline.isBefore(startTime)) {
            throw new BadRequestException("Registration deadline must be before the event start time.");
        }
    }

    private void applyRequest(Event event, EventRequest request, User organizer) {
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setLocation(request.getLocation());
        event.setStartTime(request.getStartTime());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setCapacity(request.getCapacity());
        event.setOrganizer(organizer);
    }
}
