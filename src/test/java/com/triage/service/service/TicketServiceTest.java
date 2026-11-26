package com.triage.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.triage.service.domain.Ticket;
import com.triage.service.domain.TicketCategory;
import com.triage.service.domain.TicketPriority;
import com.triage.service.domain.TicketStatus;
import com.triage.service.domain.strategy.TriageResult;
import com.triage.service.domain.strategy.TriageStrategy;
import com.triage.service.dto.CreateTicketRequest;
import com.triage.service.dto.TicketResponse;
import com.triage.service.exception.InvalidStatusTransitionException;
import com.triage.service.exception.TicketNotFoundException;
import com.triage.service.repository.TicketRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TriageStrategy triageStrategy;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void createTicketAppliesTriageBeforeSaving() {
        CreateTicketRequest request = new CreateTicketRequest("Payment outage", "Details", "reporter@example.com");
        when(triageStrategy.triage(request.title(), request.description()))
                .thenReturn(new TriageResult(TicketPriority.HIGH, TicketCategory.TECHNICAL));
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse response = ticketService.createTicket(request);

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        InOrder inOrder = inOrder(triageStrategy, ticketRepository);
        inOrder.verify(triageStrategy).triage(request.title(), request.description());
        inOrder.verify(ticketRepository).save(ticketCaptor.capture());
        Ticket savedTicket = ticketCaptor.getValue();
        assertEquals(request.title(), savedTicket.getTitle());
        assertEquals(request.description(), savedTicket.getDescription());
        assertEquals(request.reporterEmail(), savedTicket.getReporterEmail());
        assertEquals(TicketStatus.OPEN, savedTicket.getStatus());
        assertEquals(TicketPriority.HIGH, savedTicket.getPriority());
        assertEquals(TicketCategory.TECHNICAL, savedTicket.getCategory());
        assertEquals(TicketStatus.OPEN, response.status());
        assertEquals(TicketPriority.HIGH, response.priority());
        assertEquals(TicketCategory.TECHNICAL, response.category());
    }

    @Test
    void getTicketByIdReturnsFoundTicket() {
        Ticket ticket = ticket(TicketStatus.OPEN);
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.getTicketById(7L);

        assertEquals(ticket.getTitle(), response.title());
        assertEquals(ticket.getStatus(), response.status());
    }

    @Test
    void getTicketByIdThrowsWhenMissing() {
        when(ticketRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketService.getTicketById(7L));
    }

    @Test
    void listTicketsUsesAllTicketsWhenStatusIsNotProvided() {
        List<Ticket> tickets = List.of(ticket(TicketStatus.OPEN), ticket(TicketStatus.CLOSED));
        when(ticketRepository.findAll()).thenReturn(tickets);

        List<TicketResponse> responses = ticketService.listTickets(null);

        assertEquals(2, responses.size());
        verify(ticketRepository).findAll();
        verify(ticketRepository, org.mockito.Mockito.never())
                .findByStatus(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listTicketsFiltersByStatusWhenProvided() {
        when(ticketRepository.findByStatus(TicketStatus.RESOLVED))
                .thenReturn(List.of(ticket(TicketStatus.RESOLVED)));

        List<TicketResponse> responses = ticketService.listTickets(TicketStatus.RESOLVED);

        assertEquals(1, responses.size());
        assertEquals(TicketStatus.RESOLVED, responses.get(0).status());
        verify(ticketRepository).findByStatus(TicketStatus.RESOLVED);
        verify(ticketRepository, org.mockito.Mockito.never()).findAll();
    }

    @Test
    void updateTicketStatusTransitionsAFoundTicket() {
        Ticket ticket = ticket(TicketStatus.OPEN);
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.updateTicketStatus(7L, TicketStatus.IN_PROGRESS);

        assertEquals(TicketStatus.IN_PROGRESS, response.status());
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
    }

    @Test
    void updateTicketStatusRejectsAnInvalidTransition() {
        Ticket ticket = ticket(TicketStatus.OPEN);
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));

        assertThrows(InvalidStatusTransitionException.class,
                () -> ticketService.updateTicketStatus(7L, TicketStatus.RESOLVED));

        assertEquals(TicketStatus.OPEN, ticket.getStatus());
    }

    @Test
    void deleteTicketDeletesFoundTicket() {
        Ticket ticket = ticket(TicketStatus.OPEN);
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));

        ticketService.deleteTicket(7L);

        verify(ticketRepository).delete(ticket);
    }

    @Test
    void deleteTicketThrowsWhenMissing() {
        when(ticketRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketService.deleteTicket(7L));

        verifyNoInteractions(triageStrategy);
    }

    private Ticket ticket(TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setTitle("Example ticket");
        ticket.setDescription("Details");
        ticket.setReporterEmail("reporter@example.com");
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setCategory(TicketCategory.GENERAL);
        if (status != TicketStatus.OPEN) {
            ticket.transitionTo(status == TicketStatus.RESOLVED ? TicketStatus.IN_PROGRESS : status);
            if (status == TicketStatus.RESOLVED) {
                ticket.transitionTo(TicketStatus.RESOLVED);
            }
        }
        return ticket;
    }
}
