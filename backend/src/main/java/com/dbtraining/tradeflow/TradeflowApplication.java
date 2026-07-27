// backend/src/main/java/com/dbtraining/tradeflow/TradeflowApplication.java
package com.dbtraining.tradeflow;

import com.dbtraining.tradeflow.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TradeflowApplication {

    public static void main(String[] args) {
        printBanner();
        printDay2Demo();
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("  Deutsche Bank — TDI 2026 Graduate Technical Training");
        System.out.println("  Intermediate Track — Case Study: Trade Reconciliation");
        System.out.println();
    }

    /** TICKET-I026 — Console demo of the domain model independent of the DB. */
    private static void printDay2Demo() {
        List<Trade> trades = List.of(
                Trade.builder().tradeRef("TRD-2026-0001")
                        .instrumentId(1L).counterpartyId(1L)
                        .quantity(new BigDecimal("1000.00")).price(new BigDecimal("245.50"))
                        .tradeDate(LocalDate.of(2026, 3, 1)).status(TradeStatus.MATCHED).build(),
                Trade.builder().tradeRef("TRD-2026-0002")
                        .instrumentId(1L).counterpartyId(2L)
                        .quantity(new BigDecimal("500.00")).price(new BigDecimal("246.00"))
                        .tradeDate(LocalDate.of(2026, 3, 1)).status(TradeStatus.UNMATCHED).build(),
                Trade.builder().tradeRef("TRD-2026-0003")
                        .instrumentId(2L).counterpartyId(1L)
                        .quantity(new BigDecimal("100000.00")).price(new BigDecimal("99.50"))
                        .tradeDate(LocalDate.of(2026, 3, 2)).status(TradeStatus.MATCHED).build(),
                Trade.builder().tradeRef("TRD-2026-0004")
                        .instrumentId(3L).counterpartyId(2L)
                        .quantity(new BigDecimal("10.00")).price(new BigDecimal("2125.75"))
                        .tradeDate(LocalDate.of(2026, 3, 3)).status(TradeStatus.DISPUTED).build(),
                Trade.builder().tradeRef("TRD-2026-0005")
                        .instrumentId(2L).counterpartyId(3L)
                        .quantity(new BigDecimal("750.00")).price(new BigDecimal("100.25"))
                        .tradeDate(LocalDate.of(2026, 3, 4)).status(TradeStatus.PENDING).build()
        );

        System.out.println();
        System.out.println("== Day-2 domain-model demo (TICKET-I026) ===========================================");
        System.out.printf("%-15s | %-13s | %-5s | %-10s | %-10s | %-12s | %-10s%n",
                "TRADE_REF", "INSTRUMENT_ID", "CP_ID", "QTY", "PRICE", "DATE", "STATUS");
        System.out.println("-".repeat(95));
        trades.forEach(t -> System.out.printf("%-15s | %-13s | %-5s | %10s | %10s | %-12s | %-10s%n",
                t.getTradeRef(),
                t.getInstrumentId(),
                t.getCounterpartyId(),
                t.getQuantity(),
                t.getPrice(),
                t.getTradeDate(),
                t.getStatus()));
        System.out.println("====================================================================================");
        System.out.println();
    }
}