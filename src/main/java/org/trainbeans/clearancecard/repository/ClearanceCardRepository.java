package org.trainbeans.clearancecard.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.trainbeans.clearancecard.model.ClearanceCard;
import org.trainbeans.clearancecard.model.ClearanceCardRequest;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ClearanceCardRepository {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert cardInsert;
    private final SimpleJdbcInsert orderInsert;

    public ClearanceCardRepository(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.cardInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("clearance_card")
                .usingGeneratedKeyColumns("id");
        this.orderInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("clearance_card_order_number")
                .usingColumns("clearance_card_id", "sequence", "order_number");
    }

    public ClearanceCard save(ClearanceCardRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("station", request.station());
        params.put("issued_time", Time.valueOf(request.issuedTime()));
        params.put("issued_date", Date.valueOf(request.issuedDate()));
        params.put("train_number", request.trainNumber());
        params.put("order_count", request.orderCount());
        params.put("signal_stop_for", request.signalStopFor());
        params.put("operator_name", request.operatorName());

        long id = cardInsert.executeAndReturnKey(params).longValue();
        insertOrderNumbers(id, request.orderNumbers());
        return findById(id).orElseThrow();
    }

    public Optional<ClearanceCard> findById(Long id) {
        List<ClearanceCard> rows = jdbc.query(
                "SELECT * FROM clearance_card WHERE id = ?",
                (rs, rowNum) -> mapRow(rs),
                id);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        List<String> orderNumbers = jdbc.queryForList(
                "SELECT order_number FROM clearance_card_order_number " +
                "WHERE clearance_card_id = ? ORDER BY sequence",
                String.class, id);
        ClearanceCard c = rows.get(0);
        return Optional.of(withOrderNumbers(c, orderNumbers));
    }

    public List<ClearanceCard> findAll() {
        return fetchWithChildren(
                "SELECT * FROM clearance_card ORDER BY issued_date DESC, issued_time DESC");
    }

    public List<ClearanceCard> findByDate(LocalDate date) {
        return fetchWithChildren(
                "SELECT * FROM clearance_card WHERE issued_date = ? ORDER BY issued_time DESC",
                Date.valueOf(date));
    }

    // ── private helpers ─────────────────────────────────────────────────────────

    /**
     * Executes the parent query then batch-fetches all child order numbers in a
     * single IN-clause query to avoid N+1 queries.
     */
    private List<ClearanceCard> fetchWithChildren(String sql, Object... args) {
        List<ClearanceCard> cards = jdbc.query(sql, (rs, rowNum) -> mapRow(rs), args);
        if (cards.isEmpty()) {
            return cards;
        }

        List<Long> ids = cards.stream().map(ClearanceCard::id).toList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        Map<Long, List<String>> orderMap = new HashMap<>();

        jdbc.query(
                "SELECT clearance_card_id, order_number " +
                "FROM clearance_card_order_number " +
                "WHERE clearance_card_id IN (" + placeholders + ") ORDER BY clearance_card_id, sequence",
                rs -> {
                    long cid = rs.getLong("clearance_card_id");
                    orderMap.computeIfAbsent(cid, k -> new ArrayList<>())
                            .add(rs.getString("order_number"));
                },
                ids.toArray());

        return cards.stream()
                .map(c -> withOrderNumbers(c, orderMap.getOrDefault(c.id(), List.of())))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private void insertOrderNumbers(long cardId, List<String> orderNumbers) {
        if (orderNumbers == null || orderNumbers.isEmpty()) {
            return;
        }
        List<Map<String, Object>> batch = new ArrayList<>(orderNumbers.size());
        for (int i = 0; i < orderNumbers.size(); i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("clearance_card_id", cardId);
            row.put("sequence", i + 1);
            row.put("order_number", orderNumbers.get(i));
            batch.add(row);
        }
        orderInsert.executeBatch(batch.toArray(new Map[0]));
    }

    private ClearanceCard mapRow(ResultSet rs) throws SQLException {
        return new ClearanceCard(
                rs.getLong("id"),
                rs.getString("station"),
                rs.getTime("issued_time").toLocalTime(),
                rs.getDate("issued_date").toLocalDate(),
                rs.getString("train_number"),
                rs.getInt("order_count"),
                rs.getString("signal_stop_for"),
                rs.getString("operator_name"),
                List.of());   // order numbers fetched separately
    }

    private static ClearanceCard withOrderNumbers(ClearanceCard c, List<String> orderNumbers) {
        return new ClearanceCard(
                c.id(), c.station(), c.issuedTime(), c.issuedDate(),
                c.trainNumber(), c.orderCount(), c.signalStopFor(),
                c.operatorName(), orderNumbers);
    }
}

