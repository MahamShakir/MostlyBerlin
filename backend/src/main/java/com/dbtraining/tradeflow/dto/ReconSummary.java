package com.dbtraining.tradeflow.dto;

import com.dbtraining.tradeflow.model.DiscrepancyType;

import java.util.Map;

/**
 * ReconSummary — TICKET-I036.
 *
 * Rolled-up output of a reconciliation run — the shape both the Day-3
 * console and the Day-6 REST endpoint return. breakdownByType is seeded
 * with every DiscrepancyType (see ReconciliationService.generateReport)
 * so downstream consumers never have to null-guard missing keys.
 */
public record ReconSummary(
        int totalInternal,
        int totalExternal,
        int matchedCount,
        int unmatchedCount,
        Map<DiscrepancyType, Integer> breakdownByType
) {
    /** Convenience accessor — total trades seen on the internal (TradeDAO) side. */
    public int totalTrades() { return totalInternal; }
}
