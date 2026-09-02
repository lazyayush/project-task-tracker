package com.app.task_tracker_backend.dto;

import java.util.List;
import java.util.Map;

public record DashboardResponse(
        long openTasks,
        long overdueTasks,
        long dueThisWeek,
        long completedThisWeek,
        Map<String, Long> byStatus,
        Map<String, Long> byAssignee,
        List<WeeklyCompletion> completionsLast8Weeks
) {
    public record WeeklyCompletion(String weekStart, long count) {
    }
}