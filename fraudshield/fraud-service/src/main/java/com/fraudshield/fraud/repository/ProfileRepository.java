package com.fraudshield.fraud.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Behaviour profile of each customer (plain SQL with JdbcTemplate).
 * Built ONLY from payments that were finally approved, so a fraudster's
 * attempts never become part of the "normal" picture.
 */
@Repository
public class ProfileRepository {

    public record Profile(long userId, int approvedCount, BigDecimal approvedTotal, int nightCount,
                          Instant lastPaymentAt, Double lastLat, Double lastLon, String lastCity) {

        public BigDecimal average() {
            return approvedCount == 0 ? BigDecimal.ZERO
                    : approvedTotal.divide(BigDecimal.valueOf(approvedCount), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    private final JdbcTemplate jdbc;

    public ProfileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Profile> find(long userId) {
        List<Profile> rows = jdbc.query("""
                SELECT user_id, approved_count, approved_total, night_count, last_payment_at,
                       last_lat, last_lon, last_city
                FROM user_profiles WHERE user_id = ?""",
                (rs, i) -> new Profile(
                        rs.getLong("user_id"),
                        rs.getInt("approved_count"),
                        rs.getBigDecimal("approved_total"),
                        rs.getInt("night_count"),
                        rs.getTimestamp("last_payment_at") == null ? null : rs.getTimestamp("last_payment_at").toInstant(),
                        (Double) rs.getObject("last_lat"),
                        (Double) rs.getObject("last_lon"),
                        rs.getString("last_city")),
                userId);
        return rows.stream().findFirst();
    }

    public boolean isKnownDevice(long userId, String deviceId) {
        if (deviceId == null) return false;
        Boolean found = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM known_devices WHERE user_id = ? AND device_id = ?)",
                Boolean.class, userId, deviceId);
        return Boolean.TRUE.equals(found);
    }

    public boolean isKnownReceiver(long userId, String receiverUpi) {
        Boolean found = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM known_receivers WHERE user_id = ? AND lower(receiver_upi) = lower(?))",
                Boolean.class, userId, receiverUpi);
        return Boolean.TRUE.equals(found);
    }

    /** Learn from an approved payment. */
    public void recordApproved(long userId, BigDecimal amount, boolean night, Instant at,
                               Double lat, Double lon, String city, String deviceId, String receiverUpi) {
        jdbc.update("""
                INSERT INTO user_profiles (user_id, approved_count, approved_total, night_count,
                                           last_payment_at, last_lat, last_lon, last_city, updated_at)
                VALUES (?, 1, ?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (user_id) DO UPDATE SET
                    approved_count  = user_profiles.approved_count + 1,
                    approved_total  = user_profiles.approved_total + EXCLUDED.approved_total,
                    night_count     = user_profiles.night_count + EXCLUDED.night_count,
                    -- the "last seen" time/place only moves when we actually know the place
                    last_payment_at = CASE WHEN EXCLUDED.last_lat IS NULL THEN user_profiles.last_payment_at
                                           ELSE GREATEST(user_profiles.last_payment_at, EXCLUDED.last_payment_at) END,
                    last_lat        = COALESCE(EXCLUDED.last_lat, user_profiles.last_lat),
                    last_lon        = COALESCE(EXCLUDED.last_lon, user_profiles.last_lon),
                    last_city       = COALESCE(EXCLUDED.last_city, user_profiles.last_city),
                    updated_at      = NOW()
                """,
                userId, amount, night ? 1 : 0, Timestamp.from(at), lat, lon, city);
        if (deviceId != null) {
            jdbc.update("INSERT INTO known_devices (user_id, device_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    userId, deviceId);
        }
        jdbc.update("INSERT INTO known_receivers (user_id, receiver_upi) VALUES (?, ?) ON CONFLICT DO NOTHING",
                userId, receiverUpi);
    }
}
