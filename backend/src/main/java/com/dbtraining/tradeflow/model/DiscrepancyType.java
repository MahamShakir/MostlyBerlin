// backend/src/main/java/com/dbtraining/tradeflow/model/DiscrepancyType.java
package com.dbtraining.tradeflow.model;

/**
 * DiscrepancyType — TICKET-I021.
 * Reason code for a reconciliation break. Ops users see the describe()
 * output in the recon UI and CSV export.
 */
public enum DiscrepancyType {

    PRICE_MISMATCH,
    QUANTITY_MISMATCH,
    DATE_MISMATCH,
    MISSING_TRADE;

    /** Short human-readable string for UI / CSV export. */
    public String describe() {
        return switch (this) {
            case PRICE_MISMATCH    -> "Price does not match counterparty record";
            case QUANTITY_MISMATCH -> "Quantity does not match counterparty record";
            case DATE_MISMATCH     -> "Trade or settlement date mismatch";
            case MISSING_TRADE     -> "Trade exists on one side only";
        };
    }
}