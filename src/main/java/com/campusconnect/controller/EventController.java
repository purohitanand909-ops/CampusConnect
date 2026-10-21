package com.campusconnect.controller;

import com.campusconnect.dto.ApiResponse;
import com.campusconnect.dto.EventRequest;
import com.campusconnect.dto.EventResponse;
import com.campusconnect.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * GET /api/v1/events
     * Optional query params: keyword, category
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> listEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        List<EventResponse> events = eventService.listEvents(keyword, category);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    /**
     * GET /api/v1/events/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getEvent(id)));
    }

    /**
     * POST /api/v1/events
     */
    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody EventRequest request) {
        EventResponse created = eventService.createEvent(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created));
    }

    /**
     * PUT /api/v1/events/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.updateEvent(id, request)));
    }

    /**
     * DELETE /api/v1/events/{id}
     * Soft-delete: sets status to CANCELLED.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> cancelEvent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.cancelEvent(id)));
    }
}
