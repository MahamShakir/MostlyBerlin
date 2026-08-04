package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ============================================================================
 * InstrumentRepository — Day 5 (support for TICKET-I062 TradeService)
 * ============================================================================
 * Not listed as its own ticket in the guide, but TradeService (I062) injects
 * this to resolve instrumentId -> Instrument when persisting a new Trade.
 *
 * Derived finder findBySymbol supports lookups by the business key.
 * ============================================================================
 */
@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    Optional<Instrument> findBySymbol(String symbol);
}