package com.app.task_tracker_backend.dto;

import com.app.task_tracker_backend.entity.TaskStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record BulkActionRequest(
        @NotEmpty List<Long> taskIds,
        @NotNull BulkActionType actionType,
        TaskStatus newStatus,       // required if actionType == STATUS_CHANGE
        String newAssigneeEmail,    // required if actionType == ASSIGNEE_CHANGE
        Instant newDueDate          // required if actionType == DUE_DATE_CHANGE
) {
    public enum BulkActionType {
        STATUS_CHANGE,
        ASSIGNEE_CHANGE,
        DUE_DATE_CHANGE
    }
}