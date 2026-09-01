package com.app.task_tracker_backend.dto;

import com.app.task_tracker_backend.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateTaskRequest(
        @NotBlank String title,
        String description,
        @NotNull Priority priority,
        Instant dueDate
) {
}