package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class DecisionDtos {

    public record DecisionRequest(
            @NotNull Long playthroughId,
            @NotBlank @Size(min = 10) String rawInput,
            @NotBlank String impactLevel
    ) {}

    public record DecisionResponse(
            Long id,
            Long playthroughId,
            String playerTag,
            String sourceNodeCode,
            String resolvedNodeCode,
            String rawInput,
            String branchType,
            String impactLevel,
            String handlerUnit,
            String outcomeCode,
            String status,
            String playthroughStatus,
            Integer lucidity,
            Integer controlLevel,
            String endingCode,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record RealityLogResponse(
            Long id,
            Long decisionId,
            String recipientEmail,
            String subject,
            String logStatus,
            String errorMessage,
            Instant sentAt,
            Instant createdAt
    ) {}

    public record PageResponse<T>(
            List<T> content,
            long totalElements,
            int totalPages,
            int currentPage,
            int size
    ) {}

    private DecisionDtos() {}
}