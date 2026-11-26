package com.triage.service.service;

import com.triage.service.domain.Ticket;
import com.triage.service.domain.TicketStatus;
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

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public TicketResponse createTicket(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setReporterEmail(request.reporterEmail());
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
