package com.dbtraining.tradeflow.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

/**
 * ============================================================================
 * ReconResult — TICKET-I024 + TICKET-I058
 * ============================================================================
 * WHAT:    Outcome of comparing one trade against its external counterpart.
 *          One row per break (or per matched trade, depending on team policy).
 * HOW:     POJO on Day 2; @Entity on Day 5.
 * WHY:     The Ops UI page on Day 8 lists ReconResults so users can resolve.
 * OBSERVE: A row with status='OPEN' and discrepancyType=PRICE_MISMATCH means
 *          a human has to investigate.
 * ============================================================================
 *  (TICKET-I024) [Day 2]:
 *    Fields: id, tradeId (Long), status (String for now), discrepancyType
 *            (DiscrepancyType, nullable), resolvedAt (Instant, nullable),
 *            createdAt (Instant).
 *
 *  (TICKET-I058) [Day 5]:
 *    Convert to JPA entity.
 *    - @ManyToOne(fetch = LAZY) on the Trade reference
 *    - @Enumerated(EnumType.STRING) on discrepancyType
 *    - resolvedAt is @Column(nullable = true)
 * ============================================================================
 */
@Entity
@Table(name = "recon_breaks")
public class ReconResult {
    // (TICKET-I024): fields, private ctor, Builder, getters.
    public enum Status { OPEN, RESOLVED, SUPPRESSED };
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_id")
    private Trade trade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(name = "discrepancy_type", nullable = false, length = 30)
    private DiscrepancyType discrepancyType;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected ReconResult() {}

    private ReconResult(Builder b) {
        this.trade           = b.trade;
        this.discrepancyType = b.discrepancyType;
        this.status          = String.valueOf(b.status != null ? b.status : Status.OPEN);
        this.detectedAt      = b.detectedAt != null ? b.detectedAt : Instant.now();
        this.resolvedAt      = b.resolvedAt;
    }

    public Long getId()                        { return id; }
    public Trade getTrade()                      { return trade; }
    public String getStatus()                  { return status; }
    public DiscrepancyType getDiscrepancyType(){ return discrepancyType; }
    public Instant getDetectedAt()             { return detectedAt; }
    public Instant getResolvedAt()             { return resolvedAt; }


    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private Trade trade;
        private DiscrepancyType discrepancyType;
        private Status status;
        private Instant detectedAt;
        private Instant resolvedAt;

        public Builder trade(Trade v)                       { this.trade = v; return this; }
        public Builder discrepancyType(DiscrepancyType v)   { this.discrepancyType = v; return this; }
        public Builder status(Status v)                     { this.status = v; return this; }
        public Builder detectedAt(Instant v)                { this.detectedAt = v; return this; }
        public Builder resolvedAt(Instant v)                { this.resolvedAt = v; return this; }

        public ReconResult build() { return new ReconResult(this); }
    }

    public void resolve() {
        if ("RESOLVED".equals(this.status)) return;
        this.status = "RESOLVED";
        this.resolvedAt = Instant.now();
    }

    public boolean isOpen() { return "OPEN".equals(status); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReconResult other)) return false;
        return Objects.equals(trade, other.trade)
                && discrepancyType == other.discrepancyType
                && Objects.equals(detectedAt, other.detectedAt);
    }

    @Override
    public int hashCode() { return Objects.hash(trade, discrepancyType, detectedAt); }
}

