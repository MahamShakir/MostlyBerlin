package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.model.*;
import com.dbtraining.tradeflow.repository.ReconResultDAO;
import com.dbtraining.tradeflow.repository.TradeDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ReconciliationServiceMockTest — TICKET-I051 (mock TradeDAO + verify findAll())
 *                                + TICKET-I052 (ArgumentCaptor on ReconResultDAO.insert()).
 *
 * Interaction tests kept in a sibling class so I048–I050 stay focused on
 * pure-function assertions about the matcher. @ExtendWith(MockitoExtension.class)
 * initialises the @Mock fields; the setup wires a ReconciliationOrchestrator
 * that treats the two DAOs as pure collaborators.
 */
@ExtendWith(MockitoExtension.class)
class ReconciliationServiceMockTest {

    @Mock private TradeDAO tradeDAO;
    @Mock private ReconResultDAO reconResultDAO;

    private ReconciliationOrchestrator service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationOrchestrator(tradeDAO, reconResultDAO);
    }

    // -----------------------------------------------------------------------
    // TICKET-I051
    // -----------------------------------------------------------------------
    @Test
    void runForAll_callsFindAllExactlyOnce() {
        List<Trade> sample = List.of(sampleTrade("TRD-1"));
        when(tradeDAO.findAll()).thenReturn(sample);

        ReconSummary summary = service.runForAll();

        verify(tradeDAO, times(1)).findAll();
        assertThat(summary.totalTrades()).isEqualTo(1);
        // Happy-path: no discrepancies persisted because everything matched.
        verifyNoInteractions(reconResultDAO);
    }

    // -----------------------------------------------------------------------
    // TICKET-I052
    // -----------------------------------------------------------------------
    @Test
    void runForAll_oneDiscrepancy_insertsOneReconResult() {
        // Non-MATCHED status (PENDING) → orchestrator flags as MISSING_TRADE break.
        Trade internalOnly = Trade.builder()
                .tradeRef("TRD-INT-ONLY")
                .quantity(new BigDecimal("100")).price(new BigDecimal("245.50"))
                .tradeDate(LocalDate.of(2026, 3, 1))
                .status(TradeStatus.PENDING)
                .build();
        when(tradeDAO.findAll()).thenReturn(List.of(internalOnly));

        service.runForAll();

        ArgumentCaptor<ReconResult> captor = ArgumentCaptor.forClass(ReconResult.class);
        verify(reconResultDAO, times(1)).insert(captor.capture());
        ReconResult inserted = captor.getValue();

        assertThat(inserted.getDiscrepancyType())
                .isEqualTo(DiscrepancyType.MISSING_TRADE);
        assertThat(inserted.getStatus())
                .isEqualTo(ReconResult.Status.OPEN);
    }

    /** Negative-path counterpart to I052 — over-eager persistence surfaces loudly. */
    @Test
    void runForAll_allMatched_neverCallsInsert() {
        Trade matched = sampleTrade("TRD-1");
        when(tradeDAO.findAll()).thenReturn(List.of(matched));
        // External feed (stubbed elsewhere) returns the same trade — nothing to flag.

        service.runForAll();

        verify(reconResultDAO, never()).insert(any(ReconResult.class));
    }

    // -----------------------------------------------------------------------
    // Fixture helpers.
    // -----------------------------------------------------------------------
    private static Trade sampleTrade(String ref) {
        return Trade.builder()
                .tradeRef(ref)
                .quantity(new BigDecimal("100")).price(new BigDecimal("245.50"))
                .tradeDate(LocalDate.of(2026, 3, 1))
                .status(TradeStatus.MATCHED)
                .build();
    }
}
