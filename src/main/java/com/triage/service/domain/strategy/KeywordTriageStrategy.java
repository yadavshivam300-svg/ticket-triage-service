package com.triage.service.domain.strategy;

import com.triage.service.domain.TicketCategory;
import com.triage.service.domain.TicketPriority;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class KeywordTriageStrategy implements TriageStrategy {

    @Override
    public TriageResult triage(String title, String description) {
        String text = ((title == null ? "" : title) + " "
                + (description == null ? "" : description)).toLowerCase(Locale.ROOT);

        if (containsAny(text, "breach", "unauthorized", "vulnerability", "hacked", "leaked")) {
            return new TriageResult(TicketPriority.CRITICAL, TicketCategory.SECURITY);
        }
        if (containsAny(text, "outage", "down", "crash", "500", "timeout")) {
            return new TriageResult(TicketPriority.HIGH, TicketCategory.TECHNICAL);
        }
        if (containsAny(text, "invoice", "refund", "payment", "charge", "subscription")) {
            return new TriageResult(TicketPriority.MEDIUM, TicketCategory.BILLING);
        }
        return new TriageResult(TicketPriority.LOW, TicketCategory.GENERAL);
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
