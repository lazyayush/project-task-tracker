package com.app.task_tracker_backend.dto;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String key,
        String name,
        String description,
        String ownerEmail,
        boolean archived,
        Instant createdAt,
        Instant updatedAt
) {
}