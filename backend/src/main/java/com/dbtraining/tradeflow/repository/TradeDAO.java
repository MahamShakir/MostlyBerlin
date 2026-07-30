package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class TradeDAO {

    private static final String SELECT_COLUMNS =
            "t.id AS t_id, t.trade_ref AS t_trade_ref, t.quantity AS t_quantity, " +
            "t.price AS t_price, t.trade_date AS t_trade_date, t.status AS t_status, " +
            "t.created_at AS t_created_at, " +
            "i.symbol AS i_symbol, i.name AS i_name, i.asset_class AS i_asset_class, " +
            "i.currency AS i_currency, i.isin AS i_isin, " +
            "c.name AS c_name, c.lei_code AS c_lei_code, c.region AS c_region ";

    private static final String FROM_JOIN =
            "FROM trades t " +
            "JOIN instruments i    ON i.id = t.instrument_id " +
            "JOIN counterparties c ON c.id = t.counterparty_id ";

    private final DataSource dataSource;

    public TradeDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    public long insert(Trade trade) {
        String sql = "INSERT INTO trades " +
                "(trade_ref, instrument_id, counterparty_id, quantity, price, trade_date, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, trade.getTradeRef());
            ps.setLong(2,   trade.getInstrument().getId());
            ps.setLong(3,   trade.getCounterparty().getId());
            ps.setBigDecimal(4, trade.getQuantity());
            ps.setBigDecimal(5, trade.getPrice());
            ps.setDate(6, Date.valueOf(trade.getTradeDate()));
            ps.setString(7, trade.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new JdbcException("insert returned no generated key");
            }
        } catch (SQLException e) {
            throw new JdbcException("insert failed: " + trade.getTradeRef(), e);
        }
    }

    public Optional<Trade> findByRef(String tradeRef) {
        String sql = "SELECT " + SELECT_COLUMNS + FROM_JOIN + "WHERE t.trade_ref = ? LIMIT 1";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, tradeRef);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new JdbcException("findByRef failed: " + tradeRef, e);
        }
    }

    public List<Trade> findAll() {
        String sql = "SELECT " + SELECT_COLUMNS + FROM_JOIN + "ORDER BY t.trade_date DESC, t.id DESC";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Trade> out = new ArrayList<>();
            while (rs.next()) out.add(mapRow(rs));
            return out;
        } catch (SQLException e) {
            throw new JdbcException("findAll failed", e);
        }
    }

    /**
     * Update a trade's status. Returns the number of rows affected
     * (0 means tradeRef did not exist — caller decides whether to treat that
     * as an error).
     */
    public int updateStatus(String tradeRef, TradeStatus newStatus) {
        String sql = "UPDATE trades SET status = ? WHERE trade_ref = ?";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, tradeRef);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new JdbcException("updateStatus failed: " + tradeRef, e);
        }
    }

    private static Trade mapRow(ResultSet rs) throws SQLException {
        Instrument instrument = Instrument.builder()
                .symbol(rs.getString("i_symbol")).name(rs.getString("i_name"))
                .assetClass(AssetClass.valueOf(rs.getString("i_asset_class")))
                .currency(rs.getString("i_currency")).isin(rs.getString("i_isin")).build();
        Counterparty counterparty = Counterparty.builder()
                .name(rs.getString("c_name")).leiCode(rs.getString("c_lei_code"))
                .region(rs.getString("c_region")).build();
        return Trade.builder()
                .tradeRef(rs.getString("t_trade_ref"))
                .instrument(instrument).counterparty(counterparty)
                .quantity(rs.getBigDecimal("t_quantity"))
                .price(rs.getBigDecimal("t_price"))
                .tradeDate(rs.getDate("t_trade_date").toLocalDate())
                .status(TradeStatus.valueOf(rs.getString("t_status")))
                .build();
    }
}