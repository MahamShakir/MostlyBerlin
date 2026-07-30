package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.EquityTrade;
import com.dbtraining.tradeflow.model.TradeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TradeServiceTest — TICKET-I053 coverage booster.
 *
 * Targets the branches in TradeService that I048-I052 do not exercise:
 * duplicate-tradeRef guard, unmodifiable getAllTrades view, filtered-and-
 * grouped notional sum, top-N validation and ordering. Together these
 * push com.dbtraining.tradeflow.service line coverage above the 70%
 * JaCoCo threshold demanded by I053.
 */
class TradeServiceTest {

    private TradeService service;

    @BeforeEach
    void setUp() {
        service = new TradeService();
    }

    // -----------------------------------------------------------------------
    // I041 — HashMap store.
    // -----------------------------------------------------------------------
    @Test
    void addTrade_duplicateRef_throwsIllegalState() {
        service.addTrade(equity("TRD-1", new BigDecimal("100"), new BigDecimal("10.00"), TradeStatus.MATCHED));

        assertThatThrownBy(() -> service.addTrade(
                equity("TRD-1", new BigDecimal("50"), new BigDecimal("11.00"), TradeStatus.MATCHED)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TRD-1");
    }

    @Test
    void findByRef_missing_returnsEmpty() {
        assertThat(service.findByRef("nope")).isEmpty();
    }

    @Test
    void findByRef_present_returnsTrade() {
        BaseTrade t = equity("TRD-1", new BigDecimal("100"), new BigDecimal("10.00"), TradeStatus.MATCHED);
        service.addTrade(t);

        assertThat(service.findByRef("TRD-1")).contains(t);
    }

    @Test
    void getAllTrades_isUnmodifiable() {
        service.addTrade(equity("TRD-1", new BigDecimal("100"), new BigDecimal("10.00"), TradeStatus.MATCHED));
        Collection<BaseTrade> view = service.getAllTrades();

        assertThatThrownBy(() -> view.add(equity("TRD-2", new BigDecimal("1"), new BigDecimal("1"), TradeStatus.PENDING)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // -----------------------------------------------------------------------
    // I042 — sumByCounterparty.
    // -----------------------------------------------------------------------
    @Test
    void sumByCounterparty_ignoresNonMatched_andGroupsByCounterparty() {
        List<BaseTrade> input = List.of(
                equityFor(1L, "TRD-1", new BigDecimal("100"), new BigDecimal("10.00"), TradeStatus.MATCHED),
                equityFor(1L, "TRD-2", new BigDecimal("50"),  new BigDecimal("20.00"), TradeStatus.MATCHED),
                equityFor(2L, "TRD-3", new BigDecimal("10"),  new BigDecimal("30.00"), TradeStatus.MATCHED),
                equityFor(2L, "TRD-4", new BigDecimal("999"), new BigDecimal("999"),   TradeStatus.PENDING) // excluded
        );

        Map<Long, BigDecimal> sums = service.sumByCounterparty(input);

        assertThat(sums).hasSize(2);
        assertThat(sums.get(1L)).isEqualByComparingTo("2000.00"); // 100*10 + 50*20
        assertThat(sums.get(2L)).isEqualByComparingTo("300.00");  // 10*30 only
    }

    @Test
    void sumByCounterparty_emptyInput_returnsEmptyMap() {
        assertThat(service.sumByCounterparty(List.of())).isEmpty();
    }

    // -----------------------------------------------------------------------
    // I043 — topNByValue.
    // -----------------------------------------------------------------------
    @Test
    void topNByValue_nZero_throws() {
        assertThatThrownBy(() -> service.topNByValue(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0");
    }

    @Test
    void topNByValue_returnsHighestNotionalsInOrder() {
        service.addTrade(equity("TRD-A", new BigDecimal("10"), new BigDecimal("1"),  TradeStatus.MATCHED)); // 10
        service.addTrade(equity("TRD-B", new BigDecimal("10"), new BigDecimal("5"),  TradeStatus.MATCHED)); // 50
        service.addTrade(equity("TRD-C", new BigDecimal("10"), new BigDecimal("3"),  TradeStatus.MATCHED)); // 30
        service.addTrade(equity("TRD-D", new BigDecimal("10"), new BigDecimal("2"),  TradeStatus.MATCHED)); // 20
        service.addTrade(equity("TRD-E", new BigDecimal("10"), new BigDecimal("4"),  TradeStatus.MATCHED)); // 40

        List<BaseTrade> top3 = service.topNByValue(3);

        assertThat(top3).extracting(BaseTrade::getTradeRef).containsExactly("TRD-B", "TRD-E", "TRD-C");
    }

    // -----------------------------------------------------------------------
    // Fixture helpers.
    // -----------------------------------------------------------------------
    private static BaseTrade equity(String ref, BigDecimal qty, BigDecimal price, TradeStatus status) {
        return equityFor(1L, ref, qty, price, status);
    }

    private static BaseTrade equityFor(Long counterpartyId, String ref, BigDecimal qty, BigDecimal price, TradeStatus status) {
        return EquityTrade.builder()
                .tradeRef(ref).instrumentId(1L).counterpartyId(counterpartyId)
                .quantity(qty).price(price).tradeDate(LocalDate.of(2026, 3, 1))
                .status(status).exchange("XETRA").lotSize(100)
                .build();
    }
}
