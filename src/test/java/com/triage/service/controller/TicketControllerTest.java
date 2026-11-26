package com.triage.service.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.triage.service.domain.TicketCategory;
import com.triage.service.domain.TicketPriority;
import com.triage.service.domain.TicketStatus;
import com.triage.service.dto.TicketResponse;
import com.triage.service.exception.InvalidStatusTransitionException;
import com.triage.service.exception.TicketNotFoundException;
import com.triage.service.service.TicketService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void createsTicketAndReturnsLocation() throws Exception {
        when(ticketService.createTicket(org.mockito.ArgumentMatchers.any()))
                .thenReturn(response(42L, TicketStatus.OPEN));

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Payment failed",
                                  "description": "Payment did not complete",
                                  "reporterEmail": "reporter@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/api/v1/tickets/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void rejectsInvalidCreatePayload() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "Details",
                                  "reporterEmail": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.reporterEmail").exists());
    }

    @Test
    void listsTicketsWithAndWithoutStatusFilter() throws Exception {
        when(ticketService.listTickets(null)).thenReturn(List.of(response(1L, TicketStatus.OPEN)));
        when(ticketService.listTickets(TicketStatus.CLOSED))
                .thenReturn(List.of(response(2L, TicketStatus.CLOSED)));

        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OPEN"));
        mockMvc.perform(get("/api/v1/tickets").param("status", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CLOSED"));

        verify(ticketService).listTickets(null);
        verify(ticketService).listTickets(TicketStatus.CLOSED);
    }

    @Test
    void getsTicketAndMapsMissingTicketToNotFound() throws Exception {
        when(ticketService.getTicketById(1L)).thenReturn(response(1L, TicketStatus.OPEN));
        when(ticketService.getTicketById(2L)).thenThrow(new TicketNotFoundException(2L));

        mockMvc.perform(get("/api/v1/tickets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(get("/api/v1/tickets/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void patchesStatusAndMapsInvalidTransitionToConflict() throws Exception {
        when(ticketService.updateTicketStatus(1L, TicketStatus.IN_PROGRESS))
                .thenReturn(response(1L, TicketStatus.IN_PROGRESS));
        when(ticketService.updateTicketStatus(2L, TicketStatus.OPEN))
                .thenThrow(new InvalidStatusTransitionException(TicketStatus.CLOSED, TicketStatus.OPEN));

        mockMvc.perform(patch("/api/v1/tickets/1/status")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(patch("/api/v1/tickets/2/status")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void rejectsUnknownStatusInPatchJson() throws Exception {
        mockMvc.perform(patch("/api/v1/tickets/1/status")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletesTicketAndMapsMissingTicketToNotFound() throws Exception {
        doNothing().when(ticketService).deleteTicket(1L);
        doThrow(new TicketNotFoundException(2L)).when(ticketService).deleteTicket(2L);

        mockMvc.perform(delete("/api/v1/tickets/1"))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/tickets/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(ticketService).deleteTicket(1L);
        verify(ticketService).deleteTicket(2L);
    }

    private TicketResponse response(Long id, TicketStatus status) {
        return new TicketResponse(
                id,
                "Example ticket",
                "Details",
                "reporter@example.com",
                status,
                TicketPriority.MEDIUM,
                TicketCategory.GENERAL,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
