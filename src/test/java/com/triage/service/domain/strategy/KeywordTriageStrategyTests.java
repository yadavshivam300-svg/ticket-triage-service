package com.triage.service.domain.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.triage.service.domain.TicketCategory;
import com.triage.service.domain.TicketPriority;
import org.junit.jupiter.api.Test;

class KeywordTriageStrategyTests {

    private final KeywordTriageStrategy strategy = new KeywordTriageStrategy();

    @Test
    void securityKeywordsTakePrecedenceAcrossBothFields() {
        TriageResult result = strategy.triage("Payment outage", "A SECURITY breach was reported");

        assertEquals(TicketPriority.CRITICAL, result.priority());
        assertEquals(TicketCategory.SECURITY, result.category());
    }

    @Test
    void matchesTechnicalKeywordsCaseInsensitivelyWithNullTitle() {
        TriageResult result = strategy.triage(null, "The service is DOWN");

        assertEquals(TicketPriority.HIGH, result.priority());
        assertEquals(TicketCategory.TECHNICAL, result.category());
    }

    @Test
    void nullInputsUseGeneralLowFallback() {
        TriageResult result = strategy.triage(null, null);

        assertEquals(TicketPriority.LOW, result.priority());
        assertEquals(TicketCategory.GENERAL, result.category());
    }
}
