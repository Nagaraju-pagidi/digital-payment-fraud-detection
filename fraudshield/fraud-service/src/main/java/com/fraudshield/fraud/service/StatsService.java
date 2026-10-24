package com.fraudshield.fraud.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudshield.fraud.event.RuleHit;
import com.fraudshield.fraud.repository.AccountFlagRepository;
import com.fraudshield.fraud.rules.FraudRule;
import com.fraudshield.fraud.rules.RuleEngine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Numbers and chart data for the Control Center dashboard. */
@Service
public class StatsService {

    private final JdbcTemplate jdbc;
    private final AccountFlagRepository flags;
    private final RuleEngine engine;
    private final ObjectMapper objectMapper;

    public StatsService(JdbcTemplate jdbc, AccountFlagRepository flags, RuleEngine engine, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.flags = flags;
        this.engine = engine;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> stats() {
        Map<String, Object> out = new LinkedHashMap<>();

        // ---- KPI tiles ----
        Map<String, Long> byStatus = new HashMap<>();
        jdbc.query("SELECT status, COUNT(*) AS n FROM fraud_cases GROUP BY status",
                rs -> {
                    byStatus.put(rs.getString("status"), rs.getLong("n"));
                });
        long approved = byStatus.getOrDefault("APPROVED", 0L) + byStatus.getOrDefault("ANALYST_APPROVED", 0L);
        long hold = byStatus.getOrDefault("HOLD", 0L);
        long blocked = byStatus.getOrDefault("BLOCKED", 0L) + byStatus.getOrDefault("ANALYST_REJECTED", 0L);
        long total = approved + hold + blocked;

        BigDecimal protectedAmount = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM fraud_cases WHERE status IN ('BLOCKED', 'ANALYST_REJECTED')",
                BigDecimal.class);
        Double avgScore = jdbc.queryForObject("SELECT COALESCE(AVG(score), 0) FROM fraud_cases", Double.class);

        out.put("total", total);
        out.put("approved", approved);
        out.put("onHold", hold);
        out.put("blocked", blocked);
        out.put("analystReviewed", byStatus.getOrDefault("ANALYST_APPROVED", 0L) + byStatus.getOrDefault("ANALYST_REJECTED", 0L));
        out.put("frozenAccounts", flags.countByFrozenTrue());
        out.put("moneyProtected", protectedAmount);
        out.put("averageScore", avgScore == null ? 0 : Math.round(avgScore * 10) / 10.0);
        out.put("fraudRate", total == 0 ? 0 : Math.round((hold + blocked) * 1000.0 / total) / 10.0);

        // ---- Decisions per minute, last 30 minutes ----
        Instant from = Instant.now().truncatedTo(ChronoUnit.MINUTES).minus(29, ChronoUnit.MINUTES);
        Map<Instant, long[]> buckets = new LinkedHashMap<>();
        for (int i = 0; i < 30; i++) {
            buckets.put(from.plus(i, ChronoUnit.MINUTES), new long[3]);
        }
        jdbc.query("""
                SELECT date_trunc('minute', created_at) AS m, decision, COUNT(*) AS n
                FROM fraud_cases WHERE created_at >= ? GROUP BY 1, 2""",
                rs -> {
                    Instant m = rs.getTimestamp("m").toInstant();
                    long[] b = buckets.get(m);
                    if (b == null) return;
                    switch (rs.getString("decision")) {
                        case "APPROVED" -> b[0] += rs.getLong("n");
                        case "HOLD" -> b[1] += rs.getLong("n");
                        default -> b[2] += rs.getLong("n");
                    }
                }, Timestamp.from(from));
        List<Map<String, Object>> timeline = new ArrayList<>();
        buckets.forEach((minute, b) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("minute", minute.toString());
            row.put("approved", b[0]);
            row.put("hold", b[1]);
            row.put("blocked", b[2]);
            timeline.add(row);
        });
        out.put("timeline", timeline);

        // ---- Most triggered rules (last 1000 checked payments) ----
        Map<String, FraudRule> ruleById = engine.rules().stream()
                .collect(Collectors.toMap(FraudRule::id, Function.identity()));
        Map<String, Long> ruleCounts = new HashMap<>();
        List<String> hitsJson = jdbc.queryForList(
                "SELECT rule_hits FROM fraud_cases ORDER BY created_at DESC LIMIT 1000", String.class);
        TypeReference<List<RuleHit>> type = new TypeReference<>() {
        };
        for (String json : hitsJson) {
            try {
                for (RuleHit h : objectMapper.readValue(json, type)) {
                    ruleCounts.merge(h.ruleId(), 1L, Long::sum);
                }
            } catch (Exception ignored) {
                // skip a broken row
            }
        }
        List<Map<String, Object>> topRules = ruleCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(en -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("ruleId", en.getKey());
                    row.put("name", ruleById.containsKey(en.getKey()) ? ruleById.get(en.getKey()).name() : en.getKey());
                    row.put("count", en.getValue());
                    return row;
                })
                .toList();
        out.put("topRules", topRules);

        // ---- Score distribution ----
        List<Map<String, Object>> scoreBands = new ArrayList<>();
        jdbc.query("""
                SELECT CASE WHEN score < 20 THEN '0-19' WHEN score < 40 THEN '20-39'
                            WHEN score < 70 THEN '40-69' WHEN score < 120 THEN '70-119' ELSE '120+' END AS band,
                       COUNT(*) AS n
                FROM fraud_cases GROUP BY 1""",
                rs -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("band", rs.getString("band"));
                    row.put("count", rs.getLong("n"));
                    scoreBands.add(row);
                });
        List<String> order = List.of("0-19", "20-39", "40-69", "70-119", "120+");
        scoreBands.sort(Comparator.comparingInt(r -> order.indexOf((String) r.get("band"))));
        out.put("scoreBands", scoreBands);
        return out;
    }
}
