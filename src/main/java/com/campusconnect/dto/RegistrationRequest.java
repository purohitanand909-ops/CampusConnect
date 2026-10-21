package com.campusconnect.dto;

import jakarta.validation.constraints.NotNull;

/** Write DTO for creating a registration (student registering for an event). */
public class RegistrationRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Event ID is required")
    private Long eventId;

    // ── Getters & Setters ─────────────────────────────────────
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
}
