-- ============================================================================
-- data.sql — Spring Boot SQL initializer (dev profile)
-- ============================================================================
-- This file runs AFTER Liquibase on every startup of the `dev` profile.
-- Spring Boot's initializer refuses to run an EMPTY script, so the SELECT 1
-- below is a placeholder until you complete the Day-1 seed-data ticket.
--
-- TICKET-I011..I013 (Sprint 2): replace this placeholder with the seed
-- inserts the day-1 README spells out (5 instruments, 4 counterparties,
-- 10 trades with mixed statuses).
--
-- Two ways to add the seed:
--   (a) edit this file directly with INSERT statements, OR
--   (b) PREFERRED — author a Liquibase <loadData> changeset alongside your
--       schema changesets, so the seed travels with migrations.
-- ============================================================================

-- backend/src/main/resources/data.sql
-- Dev seed only. spring.sql.init.mode=embedded restricts to H2.

INSERT INTO counterparties (name, lei_code, region)
VALUES ('Deutsche Bank AG', '7LTWFZYICNSX8D621K86', 'EMEA'),
       ('Goldman Sachs Group Inc', '784F5XWPLTWKTBV3E584', 'NAMR'),
       ('Nomura Holdings Inc', '6N69WMNCQOWKSDLVDX42', 'APAC');

INSERT INTO instruments (symbol, name, asset_class, currency, isin)
VALUES ('SAP.DE', 'SAP SE', 'EQUITY', 'EUR', 'DE0007164600'),
       ('NVDA', 'NVIDIA Corp', 'EQUITY', 'USD', 'US67066G1040'),
       ('EURUSD', 'EUR/USD spot', 'FX', 'USD', NULL),
       ('BUND10Y', 'German 10Y Bund', 'FIXED_INCOME', 'EUR', 'DE0001102606'),
       ('XAU', 'Gold spot', 'COMMODITY', 'USD', NULL);

INSERT INTO trades (trade_ref, instrument_id, counterparty_id, quantity, price, trade_date, status, created_at)
VALUES ('TR-001', 1, 1, 500.0000, 120.5000, CURRENT_DATE - 5, 'MATCHED', CURRENT_TIMESTAMP),
       ('TR-002', 2, 2, 100.0000, 890.2500, CURRENT_DATE - 4, 'PENDING', CURRENT_TIMESTAMP),
       ('TR-003', 3, 1, 1000000.0000, 1.0850, CURRENT_DATE - 3, 'MATCHED', CURRENT_TIMESTAMP),
       ('TR-004', 4, 3, 5.0000, 99.4000, CURRENT_DATE - 2, 'UNMATCHED', CURRENT_TIMESTAMP),
       ('TR-005', 5, 2, 100.0000, 2150.0000, CURRENT_DATE - 2, 'DISPUTED', CURRENT_TIMESTAMP),
       ('TR-006', 1, 3, 200.0000, 121.0000, CURRENT_DATE - 1, 'PENDING', CURRENT_TIMESTAMP),
       ('TR-007', 2, 1, 50.0000, 895.0000, CURRENT_DATE - 1, 'MATCHED', CURRENT_TIMESTAMP),
       ('TR-008', 3, 2, 500000.0000, 1.0855, CURRENT_DATE, 'PENDING', CURRENT_TIMESTAMP),
       ('TR-009', 4, 1, 10.0000, 99.5500, CURRENT_DATE, 'MATCHED', CURRENT_TIMESTAMP),
       ('TR-010', 5, 3, 50.0000, 2148.7500, CURRENT_DATE, 'UNMATCHED', CURRENT_TIMESTAMP);
