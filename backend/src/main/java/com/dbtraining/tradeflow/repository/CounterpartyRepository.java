package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.Counterparty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ============================================================================
 * CounterpartyRepository — Day 5 (support for TICKET-I062 TradeService)
 * ============================================================================
 * Not listed as its own ticket in the guide, but TradeService (I062) injects
 * this to resolve counterpartyId -> Counterparty when persisting a new Trade.
 *
 * findByLeiCode is the hint the guide uses to demonstrate uniqueness
 * ("counterpartyRepository.findByLeiCode(\"DEUTDEFF\")").
 * ============================================================================
 */
@Repository
public interface CounterpartyRepository extends JpaRepository<Counterparty, Long> {

    Optional<Counterparty> findByLeiCode(String leiCode);
}