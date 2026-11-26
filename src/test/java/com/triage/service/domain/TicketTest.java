package com.triage.service.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.triage.service.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

class TicketTest {

    @Test
    void startsWithDefaultStatusPriorityAndCategory() {
        Ticket ticket = new Ticket();

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertEquals(TicketPriority.MEDIUM, ticket.getPriority());
        assertEquals(TicketCategory.GENERAL, ticket.getCategory());
    }

    @Test
    void transitionsToAnAllowedStatus() {
        Ticket ticket = new Ticket();

        ticket.transitionTo(TicketStatus.IN_PROGRESS);

        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
    }

    @Test
    void rejectsIllegalAndNullTransitionsWithoutChangingStatus() {
        Ticket ticket = new Ticket();

        assertThrows(InvalidStatusTransitionException.class,
                () -> ticket.transitionTo(TicketStatus.RESOLVED));
        assertThrows(InvalidStatusTransitionException.class, () -> ticket.transitionTo(null));
        assertEquals(TicketStatus.OPEN, ticket.getStatus());
    }
}
