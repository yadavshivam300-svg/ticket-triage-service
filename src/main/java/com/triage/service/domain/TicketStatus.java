package com.triage.service.domain;

public enum TicketStatus {
    OPEN {
        @Override
        public boolean canTransitionTo(TicketStatus next) {
            return next == IN_PROGRESS || next == CLOSED;
        }
    },
    IN_PROGRESS {
        @Override
        public boolean canTransitionTo(TicketStatus next) {
            return next == RESOLVED || next == OPEN || next == CLOSED;
        }
    },
    RESOLVED {
        @Override
        public boolean canTransitionTo(TicketStatus next) {
            return next == CLOSED || next == IN_PROGRESS;
        }
    },
    CLOSED {
        @Override
        public boolean canTransitionTo(TicketStatus next) {
            return false;
        }
    };

    public abstract boolean canTransitionTo(TicketStatus next);
}
