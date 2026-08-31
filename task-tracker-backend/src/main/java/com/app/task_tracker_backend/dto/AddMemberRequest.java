package com.app.task_tracker_backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(
        @NotBlank String userEmail
) {
}