package com.triage.service.dto;

import com.triage.service.domain.TicketCategory;
import com.triage.service.domain.TicketPriority;
import com.triage.service.domain.TicketStatus;
import java.time.Instant;

public record TicketResponse(
        Long id,
        String title,
        String description,
        String reporterEmail,
        TicketStatus status,
        TicketPriority priority,
        TicketCategory category,
        Instant createdAt,
        Instant updatedAt) {
}
