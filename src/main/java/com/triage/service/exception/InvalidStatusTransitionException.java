package com.triage.service.exception;

import com.triage.service.domain.TicketStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(TicketStatus currentStatus, TicketStatus nextStatus) {
        super("Cannot transition ticket status from " + currentStatus + " to " + nextStatus);
    }
}
