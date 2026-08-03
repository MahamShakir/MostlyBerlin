package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.dto.TradeRequest;
import com.dbtraining.tradeflow.exception.TradeNotFoundException;
import com.dbtraining.tradeflow.kafka.TradeEventProducer;
import com.dbtraining.tradeflow.model.*;
import com.dbtraining.tradeflow.repository.CounterpartyRepository;
import com.dbtraining.tradeflow.repository.InstrumentRepository;
import com.dbtraining.tradeflow.repository.TradeRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ============================================================================
 * TradeService — TICKET-I041. . . I043 + TICKET-I062
 * ============================================================================
 * WHAT:    Business-logic facade for Trade operations.
 * Day 4: HashMap-backed + Streams pipelines.
 * Day 5: rewritten to use TradeRepository (Spring Data JPA).
 * Day 6: also publishes TradeEvent to Kafka (TICKET-I115).
 * HOW:     @Service from Day 1 (so Spring can wire it into controllers).
 * Day-1 default is a no-op stub — controllers bypass it via
 * JdbcTemplate. Day-4 onward, students replace the stubs.
 * WHY:     Controllers stay thin — all rules and persistence live here.
 * OBSERVE: Switching from HashMap to JPA on Day 5 should NOT require changing
 * callers (controller code stays the same).
 * ============================================================================
 * =====================  TICKETS REMOVED =====================
 * TICKET-I041: refactor in-memory store to Map<String, BaseTrade>.
 * TICKET-I042: Streams pipeline — sumByCounterparty.
 * TICKET-I043: Streams pipeline — topNByValue.
 * ============================================================================
 * TICKET-I062: rewrite using JPA repositories + DTOs (Day 5).
 * ============================================================================
 */
@Service
public class TradeService {

    // (TICKET-I062) [Day 5]: replace the Map with TradeRepository injection:
    //   private final TradeRepository tradeRepository;
    //   public TradeService(TradeRepository tradeRepository) { ... }

    private final TradeRepository tradeRepository;
    private final InstrumentRepository instrumentRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final TradeEventProducer eventProducer;
    private static final Map<TradeStatus, Set<TradeStatus>> ALLOWED = Map.of(
            TradeStatus.PENDING, Set.of(TradeStatus.MATCHED, TradeStatus.CANCELLED),
            TradeStatus.MATCHED, Set.of(TradeStatus.SETTLED, TradeStatus.CANCELLED),
            TradeStatus.UNMATCHED, Set.of(TradeStatus.MATCHED, TradeStatus.CANCELLED)
    );
    private final Counter tradesCreatedCounter;

    public TradeService(TradeRepository tradeRepository,
                        InstrumentRepository instrumentRepository,
                        CounterpartyRepository counterpartyRepository,
                        TradeEventProducer eventProducer,
                        MeterRegistry meterRegistry) {
        this.tradeRepository = tradeRepository;
        this.instrumentRepository = instrumentRepository;
        this.counterpartyRepository = counterpartyRepository;
        this.eventProducer = eventProducer;
        this.tradesCreatedCounter = Counter.builder("tradeflow_trades_created_total")
                .description("Total trades successfully created via POST /api/v1/trades")
                .register(meterRegistry);

        for (TradeStatus status : TradeStatus.values()) {
            Gauge.builder("tradeflow_trades_by_status",
                            tradeRepository,
                            r -> (double) r.countByStatus(status))
                    .description("Live count of trades per status")
                    .tag("status", status.name())
                    .register(meterRegistry);
        }
    }

    @Transactional(readOnly = true)
    public List<TradeDto> findAll() {
        return tradeRepository.findAll().stream().map(TradeDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<TradeDto> findAll(Pageable pageable) {
        return tradeRepository.findAll(pageable).map(TradeDto::from);
    }

    @Transactional(readOnly = true)
    public TradeDto findById(Long id) {
        return tradeRepository.findById(id).map(TradeDto::from)
                .orElseThrow(() -> new TradeNotFoundException("Trade " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<TradeDto> findByStatus(TradeStatus status) {
        return tradeRepository.findByStatus(status).stream().map(TradeDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<TradeDto> findPageByStatus(TradeStatus status, Pageable pageable) {
        return tradeRepository.findByStatus(status, pageable).map(TradeDto::from);
    }

    @Transactional(readOnly = true)
    public List<TradeDto> findByDateRange(LocalDate from, LocalDate to) {
        return tradeRepository.findByTradeDateBetween(from, to).stream().map(TradeDto::from).toList();
    }

    /**
     * TODO(TICKET-I062) [Day 5]:
     *   Convert TradeRequest -> Trade entity, save via TradeRepository,
     *   publish TradeEvent on success (TICKET-I115), return TradeDto.
     */
    public TradeDto createTrade(TradeRequest request) {
        if (tradeRepository.existsByTradeRef(request.tradeRef())) {
            throw new IllegalStateException("Trade with tradeRef '" + request.tradeRef() + "' already exists");
        }
        Instrument instrument = instrumentRepository.findById(request.instrumentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "instrumentId " + request.instrumentId() + " not found"));
        Counterparty counterparty = counterpartyRepository.findById(request.counterpartyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "counterpartyId " + request.counterpartyId() + " not found"));

        Trade trade = Trade.builder()
                .tradeRef(request.tradeRef())
                .instrument(instrument)
                .counterparty(counterparty)
                .quantity(request.quantity())
                .price(request.price())
                .tradeDate(request.tradeDate())
                .status(TradeStatus.PENDING)
                .build();

        Trade saved = tradeRepository.save(trade);
        tradesCreatedCounter.increment();
        return TradeDto.from(saved);
    }

    @Transactional
    public TradeDto updateStatus(Long id, TradeStatus newStatus) {
        // TODO(TICKET-I070): implement on Day 6.
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("Trade " + id + " not found"));
        if (trade.getStatus().isTerminal()) {
            throw new IllegalStateException(
                    "Trade " + id + " is in terminal state " + trade.getStatus());
        }
        Set<TradeStatus> allowed = ALLOWED.getOrDefault(trade.getStatus(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    "Illegal transition " + trade.getStatus() + " -> " + newStatus);
        }
        trade.setStatus(newStatus);
        return TradeDto.from(trade);
    }

    @Transactional
    public void softDelete(Long id) {
        // TODO(TICKET-I071): implement soft delete + audit log on Day 6.
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("Trade " + id + " not found"));
        if (trade.getStatus() == TradeStatus.CANCELLED) {
            return;  // idempotent
        }
        if (trade.getStatus() == TradeStatus.SETTLED) {
            throw new IllegalStateException("Trade " + id + " is SETTLED — cannot cancel");
        }
        trade.setStatus(TradeStatus.CANCELLED);
    }

    public Page<TradeDto> findByTradeDateBetween(LocalDate from, LocalDate to, Pageable pageable) {
        return null;
    }
}
