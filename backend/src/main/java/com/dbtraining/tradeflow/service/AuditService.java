package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.TradeEvent;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * AuditService — TICKET-I119 (Day 9)
 * ============================================================================
 **/
@Service
public class AuditService {

    public void record(TradeEvent event) {
        throw new UnsupportedOperationException("TICKET-I119");
    }
}
