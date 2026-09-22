package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class UserDtos {

    public record UserResponse(
            Long id,
            String email,
            String displayName,
            String role,
            Instant createdAt
    ) {}

    public record RoleUpdateRequest(
            @NotBlank String role
    ) {}

    private UserDtos() {}
}