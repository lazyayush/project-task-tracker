package com.app.task_tracker_backend.dto;

public record BulkActionResult(
        Long taskId,
        boolean success,
        String message
) {
}