package com.campusconnect.dto;

import com.campusconnect.entity.Registration;
import java.time.LocalDateTime;

/** Read DTO for a Registration, used in event participant lists and student history. */
public class RegistrationResponse {

    private Long id;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long eventId;
    private String eventTitle;
    private String status;
    private Integer waitlistPosition;
    private LocalDateTime registeredAt;

    public static RegistrationResponse from(Registration r) {
        RegistrationResponse dto = new RegistrationResponse();
        dto.id               = r.getId();
        dto.studentId        = r.getStudent().getId();
        dto.studentName      = r.getStudent().getName();
        dto.studentEmail     = r.getStudent().getEmail();
        dto.eventId          = r.getEvent().getId();
        dto.eventTitle       = r.getEvent().getTitle();
        dto.status           = r.getStatus().name();
        dto.waitlistPosition = r.getWaitlistPosition();
        dto.registeredAt     = r.getRegisteredAt();
        return dto;
    }

    // ── Getters ───────────────────────────────────────────────
    public Long getId() { return id; }
    public Long getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getStudentEmail() { return studentEmail; }
    public Long getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public String getStatus() { return status; }
    public Integer getWaitlistPosition() { return waitlistPosition; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
}
