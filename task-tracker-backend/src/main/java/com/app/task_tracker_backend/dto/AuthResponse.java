package com.app.task_tracker_backend.dto;

import com.app.task_tracker_backend.entity.Role;

public record AuthResponse(
        String token,
        String email,
        Role role
) {
}