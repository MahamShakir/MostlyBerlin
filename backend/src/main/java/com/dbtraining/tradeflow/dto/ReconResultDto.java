package com.dbtraining.tradeflow.dto;

import com.dbtraining.tradeflow.model.DiscrepancyType;
import com.dbtraining.tradeflow.model.ReconResult;

import java.time.Instant;

/**
 * ============================================================================
 * ReconResultDto — TICKET-I073
 * ============================================================================
 * Outbound DTO for GET /api/v1/recon/results — Day 7's breaks-blotter table
 * consumes the paginated envelope built from this record. Never return the
 * JPA entity directly: lazy-loading a Trade proxy from inside the JSON
 * serialiser (outside a session) throws LazyInitializationException.
 * ============================================================================
 */
public record ReconResultDto(
        Long id,
        Long tradeId,
        String tradeRef,
        Long counterpartyId,
        ReconResult.Status status,
        DiscrepancyType discrepancyType,
        Instant createdAt,
        Instant resolvedAt
) {
    /**
     * Convenience mapper from entity -> DTO. Guarded against null trade
     * (recon results seeded without a real trade during unit tests).
     */
    public static ReconResultDto from(ReconResult r) {
        Long tradeId = null;
        String tradeRef = null;
        Long counterpartyId = null;
        if (r.getTrade() != null) {
            tradeId = r.getTrade().getId();
            tradeRef = r.getTrade().getTradeRef();
            counterpartyId = r.getTrade().getCounterparty().getId();
        }
        return new ReconResultDto(
                r.getId(),
                tradeId,
                tradeRef,
                counterpartyId,
                r.getStatus(),
                r.getDiscrepancyType(),
                r.getDetectedAt(),
                r.getResolvedAt()
        );
    }
}