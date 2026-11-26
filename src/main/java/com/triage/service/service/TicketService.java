package com.triage.service.service;

import com.triage.service.domain.Ticket;
import com.triage.service.domain.TicketStatus;
import com.triage.service.domain.strategy.TriageResult;
import com.triage.service.domain.strategy.TriageStrategy;
import com.triage.service.dto.CreateTicketRequest;
import com.triage.service.dto.TicketResponse;
import com.triage.service.exception.TicketNotFoundException;
import com.triage.service.repository.TicketRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TriageStrategy triageStrategy;

    public TicketService(TicketRepository ticketRepository, TriageStrategy triageStrategy) {
        this.ticketRepository = ticketRepository;
        this.triageStrategy = triageStrategy;
    }

    public TicketResponse createTicket(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setReporterEmail(request.reporterEmail());
        TriageResult triageResult = triageStrategy.triage(request.title(), request.description());
        ticket.setPriority(triageResult.priority());
        ticket.setCategory(triageResult.category());
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        return toResponse(findTicketById(id));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets(TicketStatus optionalStatus) {
        List<Ticket> tickets = optionalStatus == null
                ? ticketRepository.findAll()
                : ticketRepository.findByStatus(optionalStatus);
        return tickets.stream().map(TicketService::toResponse).toList();
    }

    public void deleteTicket(Long id) {
        ticketRepository.delete(findTicketById(id));
    }

    @Transactional
    public TicketResponse updateTicketStatus(Long id, TicketStatus newStatus) {
        Ticket ticket = findTicketById(id);
        ticket.transitionTo(newStatus);
        return toResponse(ticket);
    }

    private Ticket findTicketById(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    private static TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getReporterEmail(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCategory(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
