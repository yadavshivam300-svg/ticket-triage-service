package com.triage.service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.triage.service.domain.strategy.KeywordTriageStrategy;
import com.triage.service.domain.strategy.TriageResult;
import org.junit.jupiter.api.Test;

class KeywordTriageStrategyTest {

    private final KeywordTriageStrategy strategy = new KeywordTriageStrategy();

    @Test
    void securityTakesPrecedenceOverTechnicalBillingAndGeneralKeywords() {
        assertTriage("unauthorized outage refund", "routine question",
                TicketPriority.CRITICAL, TicketCategory.SECURITY);
    }

    @Test
    void technicalTakesPrecedenceOverBillingAndGeneralKeywords() {
        assertTriage("service outage and payment refund", "routine question",
                TicketPriority.HIGH, TicketCategory.TECHNICAL);
    }

    @Test
    void billingTakesPrecedenceOverGeneral() {
        assertTriage("question about my invoice", "routine question",
                TicketPriority.MEDIUM, TicketCategory.BILLING);
    }

    @Test
    void matchesKeywordsWithoutCaseSensitivityAcrossTitleAndDescription() {
        assertTriage("routine", "Possible VuLNeRaBiLiTy", TicketPriority.CRITICAL, TicketCategory.SECURITY);
        assertTriage("The SERVICE is DoWn", null, TicketPriority.HIGH, TicketCategory.TECHNICAL);
        assertTriage(null, "Please REFUND this", TicketPriority.MEDIUM, TicketCategory.BILLING);
    }

    @Test
    void handlesNullTitleAndDescriptionAsGeneral() {
        assertTriage(null, null, TicketPriority.LOW, TicketCategory.GENERAL);
    }

    private void assertTriage(
            String title,
            String description,
            TicketPriority expectedPriority,
            TicketCategory expectedCategory) {
        TriageResult result = strategy.triage(title, description);

        assertEquals(expectedPriority, result.priority());
        assertEquals(expectedCategory, result.category());
    }
}
