package com.app.task_tracker_backend.service;

import com.app.task_tracker_backend.entity.Priority;
import com.app.task_tracker_backend.entity.TaskStatus;

public record TaskSearchCriteria(
        String searchTerm,
        Long projectId,
        TaskStatus status,
        String assigneeEmail,
        Priority priority,
        boolean overdueOnly,
        String sortBy,       // "dueDate" | "priority" | "updatedAt"
        String sortDirection, // "asc" | "desc"
        int page,
        int size
) {
}