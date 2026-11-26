package com.triage.service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TicketStatusTest {

    @Test
    void allowsOnlyDefinedTransitions() {
        for (TicketStatus current : TicketStatus.values()) {
            for (TicketStatus next : TicketStatus.values()) {
                boolean expected = switch (current) {
                    case OPEN -> next == TicketStatus.IN_PROGRESS || next == TicketStatus.CLOSED;
                    case IN_PROGRESS -> next == TicketStatus.RESOLVED
                            || next == TicketStatus.OPEN
                            || next == TicketStatus.CLOSED;
                    case RESOLVED -> next == TicketStatus.CLOSED || next == TicketStatus.IN_PROGRESS;
                    case CLOSED -> false;
                };
                assertEquals(expected, current.canTransitionTo(next),
                        () -> current + " -> " + next);
            }
            assertEquals(false, current.canTransitionTo(null), () -> current + " -> null");
        }
    }
}
