package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.RuleHit;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * R03 - Impossible travel: compared with the customer's last approved payment,
 * they would have had to travel faster than a plane (900 km/h).
 */
@Component
public class ImpossibleTravelRule implements FraudRule {
    private static final double MAX_SPEED_KMH = 900;

    public String id() { return "R03"; }
    public String name() { return "Impossible travel"; }
    public String description() { return "Distance from the last payment location is impossible to cover in the time passed (faster than 900 km/h)"; }
    public int maxPoints() { return 60; }

    @Override
    public Optional<RuleHit> evaluate(FraudContext ctx) {
        var p = ctx.profile();
        var e = ctx.event();
        if (!ctx.hasLocation() || p == null || p.lastPaymentAt() == null || p.lastLat() == null || p.lastLon() == null) {
            return Optional.empty();
        }
        double km = Geo.distanceKm(p.lastLat(), p.lastLon(), e.latitude(), e.longitude());
        if (km < 100) return Optional.empty();
        long minutes = Math.max(1, Duration.between(p.lastPaymentAt(), e.createdAt()).toMinutes());
        double speed = km / (minutes / 60.0);
        if (speed <= MAX_SPEED_KMH) return Optional.empty();
        return hit(60, (p.lastCity() == null ? "Last location" : p.lastCity()) + " -> " + e.locationCity() + ": " + Geo.inr(km) + " km in " + minutes
                + " min (" + Geo.inr(speed) + " km/h) - no one can travel that fast");
    }
}
