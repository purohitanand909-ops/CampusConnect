package com.campusconnect.service;

import com.campusconnect.dto.EventRequest;
import com.campusconnect.dto.EventResponse;
import com.campusconnect.entity.Event;
import com.campusconnect.entity.User;
import com.campusconnect.exception.BadRequestException;
import com.campusconnect.exception.ResourceNotFoundException;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository        eventRepository;
    @Mock private UserRepository         userRepository;
    @Mock private RegistrationRepository registrationRepository;

    @InjectMocks
    private EventService eventService;

    private User  organizer;
    private Event sampleEvent;

    @BeforeEach
    void setUp() {
        organizer = new User("Dr. Priya Sharma", "priya.sharma@campus.edu", User.Role.ORGANIZER);
        setId(organizer, 1L);

        sampleEvent = new Event();
        setId(sampleEvent, 10L);
        sampleEvent.setTitle("Campus Hackathon");
        sampleEvent.setDescription("Annual coding hackathon");
        sampleEvent.setCategory("Technology");
        sampleEvent.setLocation("Engineering Hall");
        sampleEvent.setCapacity(50);
        sampleEvent.setStatus(Event.Status.UPCOMING);
        sampleEvent.setStartTime(LocalDateTime.now().plusDays(5));
        sampleEvent.setRegistrationDeadline(LocalDateTime.now().plusDays(3));
        sampleEvent.setOrganizer(organizer);
    }

    @Test
    void createEvent_success() {
        EventRequest request = sampleRequest(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            setId(e, 10L);
            return e;
        });

        EventResponse response = eventService.createEvent(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Campus Hackathon");
        assertThat(response.getCapacity()).isEqualTo(50);
        assertThat(response.getAvailableSpots()).isEqualTo(50);
    }

    @Test
    void createEvent_invalidDates_throwsBadRequest() {
        EventRequest request = sampleRequest(1L);
        // Deadline is after start time
        request.setRegistrationDeadline(LocalDateTime.now().plusDays(6));
        request.setStartTime(LocalDateTime.now().plusDays(5));
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Registration deadline must be before");
    }

    @Test
    void createEvent_nonOrganizer_throwsBadRequest() {
        User studentUser = new User("Student", "student@campus.edu", User.Role.STUDENT);
        setId(studentUser, 2L);
        EventRequest request = sampleRequest(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(studentUser));

        assertThatThrownBy(() -> eventService.createEvent(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is not an organizer");
    }

    @Test
    void updateEvent_whenCancelled_throwsBadRequest() {
        sampleEvent.setStatus(Event.Status.CANCELLED);
        when(eventRepository.findByIdWithOrganizer(10L)).thenReturn(Optional.of(sampleEvent));

        EventRequest request = sampleRequest(1L);

        assertThatThrownBy(() -> eventService.updateEvent(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot update a cancelled event");
    }

    @Test
    void cancelEvent_success() {
        when(eventRepository.findByIdWithOrganizer(10L)).thenReturn(Optional.of(sampleEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponse response = eventService.cancelEvent(10L);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        assertThat(sampleEvent.getStatus()).isEqualTo(Event.Status.CANCELLED);
    }

    @Test
    void cancelEvent_alreadyCancelled_throwsBadRequest() {
        sampleEvent.setStatus(Event.Status.CANCELLED);
        when(eventRepository.findByIdWithOrganizer(10L)).thenReturn(Optional.of(sampleEvent));

        assertThatThrownBy(() -> eventService.cancelEvent(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void listEvents_calculatesAvailableSpotsCorrectly() {
        when(eventRepository.search("Hackathon", null)).thenReturn(List.of(sampleEvent));
        // Simulate 20 confirmed registrations
        Object[] groupRow = new Object[]{10L, 20L};
        List<Object[]> groupedResults = Collections.singletonList(groupRow);
        when(registrationRepository.countConfirmedRegistrationsGrouped()).thenReturn(groupedResults);

        List<EventResponse> results = eventService.listEvents("Hackathon", null);

        assertThat(results).hasSize(1);
        EventResponse resp = results.get(0);
        assertThat(resp.getCapacity()).isEqualTo(50);
        assertThat(resp.getRegisteredCount()).isEqualTo(20);
        assertThat(resp.getAvailableSpots()).isEqualTo(30);
    }

    @Test
    void getEvent_calculatesAvailableSpotsCorrectly() {
        when(eventRepository.findByIdWithOrganizer(10L)).thenReturn(Optional.of(sampleEvent));
        when(registrationRepository.countConfirmedByEventIdReadOnly(10L)).thenReturn(15L);

        EventResponse response = eventService.getEvent(10L);

        assertThat(response.getRegisteredCount()).isEqualTo(15);
        assertThat(response.getAvailableSpots()).isEqualTo(35);
    }

    // ── Helpers ───────────────────────────────────────────────

    private EventRequest sampleRequest(Long organizerId) {
        EventRequest req = new EventRequest();
        req.setTitle("Campus Hackathon");
        req.setDescription("Annual coding hackathon");
        req.setCategory("Technology");
        req.setLocation("Engineering Hall");
        req.setCapacity(50);
        req.setStartTime(LocalDateTime.now().plusDays(5));
        req.setRegistrationDeadline(LocalDateTime.now().plusDays(3));
        req.setOrganizerId(organizerId);
        return req;
    }

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
