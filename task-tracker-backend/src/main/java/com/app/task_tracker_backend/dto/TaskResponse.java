package com.app.task_tracker_backend.dto;

import com.app.task_tracker_backend.entity.Priority;
import com.app.task_tracker_backend.entity.TaskStatus;

import java.time.Instant;
import java.util.List;

public record TaskResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        Priority priority,
        Instant dueDate,
        TaskStatus status,
        TaskStatus blockedFromStatus,
        List<Long> blockingTaskIds,
        Instant createdAt,
        Instant updatedAt
) {
}