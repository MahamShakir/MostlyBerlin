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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceMockTest {

    @Mock private TradeDAO tradeDAO;
    @Mock private ReconResultDAO reconResultDAO;

    private ReconciliationOrchestrator service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationOrchestrator(tradeDAO, reconResultDAO);
    }

    @Test
    void runForAll_callsFindAllExactlyOnce() {
        List<Trade> sample = List.of(sampleTrade("TRD-1"));
        when(tradeDAO.findAll()).thenReturn(sample);

        ReconSummary summary = service.runForAll();

        verify(tradeDAO, times(1)).findAll();
        assertThat(summary.totalTrades()).isEqualTo(1);
        verifyNoInteractions(reconResultDAO);
    }

    @Test
    void runForAll_oneDiscrepancy_insertsOneReconResult() {
        Trade internalOnly = sampleTrade("TRD-INT-ONLY");
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

    @Test
    void runForAll_allMatched_neverCallsInsert() {
        Trade matched = sampleTrade("TRD-1");
        when(tradeDAO.findAll()).thenReturn(List.of(matched));

        service.runForAll();

        verify(reconResultDAO, never()).insert(any(ReconResult.class));
    }

    private static Trade sampleTrade(String ref) {
        return Trade.builder()
                .tradeRef(ref)
                .quantity(new BigDecimal("100")).price(new BigDecimal("245.50"))
                .tradeDate(LocalDate.of(2026, 3, 1))
                .status(TradeStatus.MATCHED)
                .build();
    }
}