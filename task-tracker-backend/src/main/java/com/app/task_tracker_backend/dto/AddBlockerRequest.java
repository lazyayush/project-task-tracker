package com.app.task_tracker_backend.dto;

import jakarta.validation.constraints.NotNull;

public record AddBlockerRequest(
        @NotNull Long blockingTaskId
) {
}