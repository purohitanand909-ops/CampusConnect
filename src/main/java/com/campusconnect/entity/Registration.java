package com.campusconnect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registrations",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_reg_active", columnNames = {"student_id", "event_id"})
        },
        indexes = {
            @Index(name = "idx_reg_event",   columnList = "event_id"),
            @Index(name = "idx_reg_student", columnList = "student_id"),
            @Index(name = "idx_reg_status",  columnList = "status")
        })
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.CONFIRMED;

    @Column(name = "waitlist_position")
    private Integer waitlistPosition;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    public enum Status {
        CONFIRMED, WAITLISTED, CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        this.registeredAt = LocalDateTime.now();
    }

    // ── Constructors ──────────────────────────────────────────
    public Registration() {}

    public Registration(User student, Event event, Status status, Integer waitlistPosition) {
        this.student = student;
        this.event = event;
        this.status = status;
        this.waitlistPosition = waitlistPosition;
    }

    // ── Getters & Setters ─────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Integer getWaitlistPosition() { return waitlistPosition; }
    public void setWaitlistPosition(Integer waitlistPosition) { this.waitlistPosition = waitlistPosition; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}
