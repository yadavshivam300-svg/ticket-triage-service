package com.triage.service.repository;

import com.triage.service.domain.Ticket;
import com.triage.service.domain.TicketStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(TicketStatus status);
}
