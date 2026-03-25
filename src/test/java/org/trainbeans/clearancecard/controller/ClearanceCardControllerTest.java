package org.trainbeans.clearancecard.controller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import org.trainbeans.clearancecard.exception.ClearanceCardNotFoundException;
import org.trainbeans.clearancecard.model.ClearanceCard;
import org.trainbeans.clearancecard.model.ClearanceCardRequest;
import org.trainbeans.clearancecard.service.ClearanceCardService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WebMvcTest(ClearanceCardController.class)
@TestPropertySource(properties = "app.railroad.name=Test Railroad")
class ClearanceCardControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private ClearanceCardService service;
    private static ClearanceCard sampleCard() {
        return new ClearanceCard(
                1L, "Middletown",
                LocalTime.of(10, 30),
                LocalDate.of(2026, 3, 24),
                "Extra 101 East", 3,
                "Extra 202 West", "J. Smith",
                List.of("12", "45", "87"));
    }
    @Test
    void createReturns201WithLocationHeader() throws Exception {
        when(service.create(any())).thenReturn(sampleCard());
        ClearanceCardRequest req = new ClearanceCardRequest(
                "Middletown", LocalTime.of(10, 30), LocalDate.of(2026, 3, 24),
                "Extra 101 East", 3, "Extra 202 West", "J. Smith",
                List.of("12", "45", "87"));
        mockMvc.perform(post("/api/clearance-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/clearance-cards/1")))
                .andExpect(jsonPath("$.station").value("Middletown"));
    }
    @Test
    void getByIdReturnsFullCard() throws Exception {
        when(service.findById(1L)).thenReturn(sampleCard());
        mockMvc.perform(get("/api/clearance-cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.station").value("Middletown"))
                .andExpect(jsonPath("$.trainNumber").value("Extra 101 East"))
                .andExpect(jsonPath("$.orderNumbers[0]").value("12"))
                .andExpect(jsonPath("$.orderNumbers[2]").value("87"));
    }
    @Test
    void getByIdReturns404WhenNotFound() throws Exception {
        when(service.findById(99L)).thenThrow(new ClearanceCardNotFoundException(99L));
        mockMvc.perform(get("/api/clearance-cards/99"))
                .andExpect(status().isNotFound());
    }
    @Test
    void listWithDateParamFilters() throws Exception {
        when(service.findAll(eq(LocalDate.of(2026, 3, 24)))).thenReturn(List.of(sampleCard()));
        mockMvc.perform(get("/api/clearance-cards?date=2026-03-24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].station").value("Middletown"));
    }
    @Test
    void listWithNoDateParamReturnsAll() throws Exception {
        when(service.findAll(null)).thenReturn(List.of(sampleCard()));
        mockMvc.perform(get("/api/clearance-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
    @Test
    void printReturnsHtmlWithRailroadNameAndOrderNumbers() throws Exception {
        when(service.findById(1L)).thenReturn(sampleCard());
        mockMvc.perform(get("/api/clearance-cards/1/print"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Test Railroad")))
                .andExpect(content().string(containsString("12")))
                .andExpect(content().string(containsString("CLEARANCE")));
    }
    @Test
    void printReturns404WhenCardNotFound() throws Exception {
        when(service.findById(99L)).thenThrow(new ClearanceCardNotFoundException(99L));
        mockMvc.perform(get("/api/clearance-cards/99/print"))
                .andExpect(status().isNotFound());
    }
}
