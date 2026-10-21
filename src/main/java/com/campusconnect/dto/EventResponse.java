package com.campusconnect.dto;

import com.campusconnect.entity.Event;
import java.time.LocalDateTime;

/** Read DTO returned to the client for an Event. */
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime registrationDeadline;
    private int capacity;
    private String status;
    private Long organizerId;
    private String organizerName;
    private LocalDateTime createdAt;

    public static EventResponse from(Event e) {
        EventResponse dto = new EventResponse();
        dto.id                   = e.getId();
        dto.title                = e.getTitle();
        dto.description          = e.getDescription();
        dto.category             = e.getCategory();
        dto.location             = e.getLocation();
        dto.startTime            = e.getStartTime();
        dto.registrationDeadline = e.getRegistrationDeadline();
        dto.capacity             = e.getCapacity();
        dto.status               = e.getStatus().name();
        dto.organizerId          = e.getOrganizer().getId();
        dto.organizerName        = e.getOrganizer().getName();
        dto.createdAt            = e.getCreatedAt();
        return dto;
    }

    // ── Getters ───────────────────────────────────────────────
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public int getCapacity() { return capacity; }
    public String getStatus() { return status; }
    public Long getOrganizerId() { return organizerId; }
    public String getOrganizerName() { return organizerName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
