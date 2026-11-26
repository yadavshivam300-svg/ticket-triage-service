package com.triage.service;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TicketTriageServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void createsListsReadsAndDeletesTicket() throws Exception {
		var createResult = mockMvc.perform(post("/api/v1/tickets")
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "title": "Payment failed",
								  "description": "The payment did not complete.",
								  "reporterEmail": "customer@example.com"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andExpect(jsonPath("$.priority").value("MEDIUM"))
				.andExpect(jsonPath("$.category").value("BILLING"))
				.andExpect(jsonPath("$.createdAt").exists())
				.andReturn();

		String ticketPath = URI.create(createResult.getResponse().getHeader("Location")).getPath();
		mockMvc.perform(get("/api/v1/tickets").param("status", "OPEN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].title").value(hasItem("Payment failed")));
		mockMvc.perform(get(ticketPath))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reporterEmail").value("customer@example.com"));
		mockMvc.perform(delete(ticketPath))
				.andExpect(status().isNoContent());
		mockMvc.perform(get(ticketPath))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void returnsFieldErrorsForInvalidCreateRequest() throws Exception {
		mockMvc.perform(post("/api/v1/tickets")
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "title": "",
								  "description": "Issue details",
								  "reporterEmail": "not-an-email"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors.title").exists())
				.andExpect(jsonPath("$.fieldErrors.reporterEmail").exists());
	}

	@Test
	void updatesStatusAndRejectsInvalidTransitions() throws Exception {
		var createResult = mockMvc.perform(post("/api/v1/tickets")
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "title": "Feature request",
								  "description": "Add a new report",
								  "reporterEmail": "customer@example.com"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String ticketPath = URI.create(createResult.getResponse().getHeader("Location")).getPath();

		mockMvc.perform(patch(ticketPath + "/status")
						.contentType(APPLICATION_JSON)
						.content("{\"status\":\"IN_PROGRESS\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"));

		mockMvc.perform(patch(ticketPath + "/status")
						.contentType(APPLICATION_JSON)
						.content("{\"status\":\"INVALID\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));

		mockMvc.perform(patch(ticketPath + "/status")
						.contentType(APPLICATION_JSON)
						.content("{\"status\":\"OPEN\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("OPEN"));
	}

	@Test
	void rejectsClosedStatusTransitionsWithConflict() throws Exception {
		var createResult = mockMvc.perform(post("/api/v1/tickets")
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "title": "Security breach",
								  "description": "Unauthorized access reported",
								  "reporterEmail": "customer@example.com"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.priority").value("CRITICAL"))
				.andExpect(jsonPath("$.category").value("SECURITY"))
				.andReturn();
		String ticketPath = URI.create(createResult.getResponse().getHeader("Location")).getPath();

		mockMvc.perform(patch(ticketPath + "/status")
						.contentType(APPLICATION_JSON)
						.content("{\"status\":\"CLOSED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOSED"));

		mockMvc.perform(patch(ticketPath + "/status")
						.contentType(APPLICATION_JSON)
						.content("{\"status\":\"OPEN\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}
}
