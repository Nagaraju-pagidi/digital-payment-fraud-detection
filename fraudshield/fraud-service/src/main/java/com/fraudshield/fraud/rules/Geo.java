package com.fraudshield.fraud.rules;

import java.text.NumberFormat;
import java.util.Locale;

public final class Geo {

    private Geo() {
    }

    /** Haversine formula - distance between two GPS points on Earth, in km. */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** 1250000.5 -> "1,250,000" */
    public static String inr(Number value) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));
        f.setMaximumFractionDigits(0);
        return f.format(value);
    }
}
