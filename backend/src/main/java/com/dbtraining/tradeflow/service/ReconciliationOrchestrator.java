package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.repository.ReconResultDAO;
import com.dbtraining.tradeflow.repository.TradeDAO;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * ReconciliationOrchestrator — TICKET-I051 / TICKET-I052 (Day 4)
 *
 * JDBC-based orchestrator: loads all trades from TradeDAO, classifies
 * non-MATCHED trades as MISSING_TRADE breaks, persists each via ReconResultDAO.
 *
 * Used in Day-4 Mockito tests (TICKET-I051 / I052) to demonstrate:
 *   - mocking a JDBC DAO with @Mock + when(tradeDAO.findAll()).thenReturn(...)
 *   - verifying interaction counts with verify(tradeDAO, times(1)).findAll()
 *   - capturing persisted rows with ArgumentCaptor<ReconResult>
 *
 * Day 5 replaces this with a JPA-backed ReconciliationService; both live in
 * the codebase until the JDBC layer is retired.
 */
public class ReconciliationOrchestrator {

    private final TradeDAO tradeDAO;
    private final ReconResultDAO reconResultDAO;

    public ReconciliationOrchestrator(TradeDAO tradeDAO, ReconResultDAO reconResultDAO) {
        this.tradeDAO = tradeDAO;
        this.reconResultDAO = reconResultDAO;
    }

    /**
     * Run reconciliation across all trades in the DB.
     * MATCHED trades are counted as clean; every other status is persisted
     * as a MISSING_TRADE break (no external feed available at the JDBC layer).
     */
    public ReconSummary runForAll() {
        List<Trade> trades = tradeDAO.findAll();

        int matched = 0;
        int unmatched = 0;
        Map<DiscrepancyType, Integer> breakdown = new EnumMap<>(DiscrepancyType.class);
        for (DiscrepancyType t : DiscrepancyType.values()) breakdown.put(t, 0);

        for (Trade trade : trades) {
            if (trade.getStatus() == TradeStatus.MATCHED) {
                matched++;
            } else {
                ReconResult result = ReconResult.builder()
                        .trade(trade)
                        .discrepancyType(DiscrepancyType.MISSING_TRADE)
                        .status(ReconResult.Status.OPEN)
                        .build();
                reconResultDAO.insert(result);
                breakdown.merge(DiscrepancyType.MISSING_TRADE, 1, Integer::sum);
                unmatched++;
            }
        }

        return new ReconSummary(trades.size(), 0, matched, unmatched, breakdown);
    }
}