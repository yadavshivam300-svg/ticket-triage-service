package com.triage.service.domain.strategy;

import com.triage.service.domain.TicketCategory;
import com.triage.service.domain.TicketPriority;

public record TriageResult(TicketPriority priority, TicketCategory category) {
}
