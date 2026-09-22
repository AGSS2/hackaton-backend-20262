package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class PlaythroughDtos {

    public record PlaythroughRequest(
            @NotBlank @Size(min = 2, max = 40) String playerTag,
            @NotBlank String startNodeCode
    ) {}

    public record PlaythroughResponse(
            Long id,
            String playerTag,
            String ownerEmail,
            String startNodeCode,
            String currentNodeCode,
            Integer lucidity,
            Integer controlLevel,
            String status,
            String endingCode,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record PathStep(
            int order,
            Long decisionId,
            String fromNodeCode,
            String toNodeCode,
            String branchType,
            String impactLevel,
            Instant createdAt
    ) {}

    public record PathResponse(
            Long playthroughId,
            String playerTag,
            String status,
            String endingCode,
            String startNodeCode,
            String currentNodeCode,
            List<PathStep> steps
    ) {}

    private PlaythroughDtos() {}
}