package com.dbtraining.tradeflow.repository;

/**
 * JdbcException — supporting class for TICKETS I045 / I046 / I047.
 *
 * Runtime wrapper for SQLException so DAO callers do not drown in
 * checked-exception ceremony. Message always carries the failed operation
 * and the offending key (tradeRef, tradeId, region, ...).
 */
public class JdbcException extends RuntimeException {

    public JdbcException(String message) {
        super(message);
    }

    public JdbcException(String message, Throwable cause) {
        super(message, cause);
    }
}