package com.app.task_tracker_backend.dto;

import java.util.List;

public record BulkActionResponse(
        int totalRequested,
        int succeeded,
        int failed,
        List<BulkActionResult> results
) {
}