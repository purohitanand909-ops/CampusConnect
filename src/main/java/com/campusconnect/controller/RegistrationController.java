package com.campusconnect.controller;

import com.campusconnect.dto.ApiResponse;
import com.campusconnect.dto.RegistrationRequest;
import com.campusconnect.dto.RegistrationResponse;
import com.campusconnect.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * POST /api/v1/registrations
     * Register a student for an event.
     */
    @PostMapping("/registrations")
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(
            @Valid @RequestBody RegistrationRequest request) {
        RegistrationResponse result = registrationService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(result));
    }

    /**
     * DELETE /api/v1/registrations/{id}
     * Cancel a registration (triggers waitlist promotion if applicable).
     */
    @DeleteMapping("/registrations/{id}")
    public ResponseEntity<ApiResponse<RegistrationResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.cancelRegistration(id)));
    }

    /**
     * GET /api/v1/events/{id}/registrations
     * List all participants (confirmed + waitlisted) for an event.
     */
    @GetMapping("/events/{id}/registrations")
    public ResponseEntity<ApiResponse<List<RegistrationResponse>>> eventRegistrations(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.getEventRegistrations(id)));
    }

    /**
     * GET /api/v1/students/{id}/registrations
     * List all registrations for a student.
     */
    @GetMapping("/students/{id}/registrations")
    public ResponseEntity<ApiResponse<List<RegistrationResponse>>> studentRegistrations(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(registrationService.getStudentRegistrations(id)));
    }
}
