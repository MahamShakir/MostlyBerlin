package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.config.SecurityConfig;
import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.exception.GlobalExceptionHandler;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.service.TradeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TradeController.class)
@Import({com.dbtraining.tradeflow.config.SecurityConfig.class,
        com.dbtraining.tradeflow.exception.GlobalExceptionHandler.class})
public class TradeControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @MockBean
    TradeService tradeService;

    // ------------------------------------------------------------------------
    // TICKET-I082 — happy-path POST
    // ------------------------------------------------------------------------
    @Test
    @WithMockUser(roles = "TRADER")
    void createTrade_validInput_returns201() throws Exception {
        TradeDto saved = sampleDto(42L, "TRD-2026-0001");
        when(tradeService.createTrade(any())).thenReturn(saved);

        String body = """
                {
                  "tradeRef": "TRD-2026-0001",
                  "instrumentId": 1,
                  "counterpartyId": 1,
                  "quantity": 100,
                  "price": 250.50,
                  "tradeDate": "2026-03-01"
                }
                """;

        mvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/trades/42"))
                .andExpect(jsonPath("$.tradeRef").value("TRD-2026-0001"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(tradeService).createTrade(any());
    }

    static TradeDto sampleDto(Long id, String ref) {
        return new TradeDto(
                id, ref, 1L, 1L,
                new BigDecimal("100"), new BigDecimal("250.50"),
                LocalDate.of(2026, 3, 1),
                TradeStatus.PENDING,
                Instant.now());
    }
}
