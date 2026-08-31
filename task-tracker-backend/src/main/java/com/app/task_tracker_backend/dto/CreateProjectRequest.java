package com.app.task_tracker_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9]{2,10}$", message = "Key must be 2-10 uppercase letters/digits")
        String key,

        @NotBlank @Size(max = 255) String name,

        @Size(max = 2000) String description,

        @NotBlank String ownerEmail
) {
}