package com.tuckersoft.branchengine.event;

import java.time.Instant;

public record DecisionCommittedEvent(
        Long decisionId,
        String recipientEmail,
        String displayName,
        String playerTag,
        String branchType,
        String impactLevel,
        String handlerUnit,
        String outcomeCode,
        String sourceNodeCode,
        String resolvedNodeCode,
        String playthroughStatus,
        Integer lucidity,
        Integer controlLevel,
        String endingCode,
        String rawInput,
        Instant createdAt,
        boolean simulateMailFailure
) {}