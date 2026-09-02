package com.app.task_tracker_backend.dto;

import com.app.task_tracker_backend.entity.HistoryEventType;

import java.time.Instant;

public record TaskHistoryResponse(
        Long id,
        String actorEmail,
        HistoryEventType eventType,
        String fieldName,
        String oldValue,
        String newValue,
        String commentText,
        Instant createdAt
) {
}