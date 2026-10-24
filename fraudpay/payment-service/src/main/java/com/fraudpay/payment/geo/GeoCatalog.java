package com.fraudpay.payment.geo;

import com.fraudpay.payment.dto.CityDto;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Small offline city catalogue (no internet / paid map API needed).
 * - Indian cities are offered as "home city" at registration.
 * - nearest(lat, lon) turns the browser's GPS position into a readable place + country.
 */
@Component
public class GeoCatalog {

    public record City(String name, String state, String country, double lat, double lon) {
    }

    public record GeoMatch(String city, String country, double distanceKm) {
    }

    private static final List<City> INDIA = List.of(
            new City("Hyderabad", "Telangana", "IN", 17.3850, 78.4867),
            new City("Warangal", "Telangana", "IN", 17.9689, 79.5941),
            new City("Vijayawada", "Andhra Pradesh", "IN", 16.5062, 80.6480),
            new City("Visakhapatnam", "Andhra Pradesh", "IN", 17.6868, 83.2185),
            new City("Tirupati", "Andhra Pradesh", "IN", 13.6288, 79.4192),
            new City("Bengaluru", "Karnataka", "IN", 12.9716, 77.5946),
            new City("Mysuru", "Karnataka", "IN", 12.2958, 76.6394),
            new City("Chennai", "Tamil Nadu", "IN", 13.0827, 80.2707),
            new City("Coimbatore", "Tamil Nadu", "IN", 11.0168, 76.9558),
            new City("Kochi", "Kerala", "IN", 9.9312, 76.2673),
            new City("Thiruvananthapuram", "Kerala", "IN", 8.5241, 76.9366),
            new City("Mumbai", "Maharashtra", "IN", 19.0760, 72.8777),
            new City("Pune", "Maharashtra", "IN", 18.5204, 73.8567),
            new City("Nagpur", "Maharashtra", "IN", 21.1458, 79.0882),
            new City("Ahmedabad", "Gujarat", "IN", 23.0225, 72.5714),
            new City("Surat", "Gujarat", "IN", 21.1702, 72.8311),
            new City("Delhi", "Delhi", "IN", 28.6139, 77.2090),
            new City("Jaipur", "Rajasthan", "IN", 26.9124, 75.7873),
            new City("Lucknow", "Uttar Pradesh", "IN", 26.8467, 80.9462),
            new City("Chandigarh", "Chandigarh", "IN", 30.7333, 76.7794),
            new City("Bhopal", "Madhya Pradesh", "IN", 23.2599, 77.4126),
            new City("Kolkata", "West Bengal", "IN", 22.5726, 88.3639),
            new City("Bhubaneswar", "Odisha", "IN", 20.2961, 85.8245),
            new City("Patna", "Bihar", "IN", 25.5941, 85.1376),
            new City("Guwahati", "Assam", "IN", 26.1445, 91.7362)
    );

    private static final List<City> WORLD = List.of(
            new City("Dubai", "", "AE", 25.2048, 55.2708),
            new City("Singapore", "", "SG", 1.3521, 103.8198),
            new City("London", "", "GB", 51.5074, -0.1278),
            new City("New York", "", "US", 40.7128, -74.0060),
            new City("San Francisco", "", "US", 37.7749, -122.4194),
            new City("Toronto", "", "CA", 43.6532, -79.3832),
            new City("Lagos", "", "NG", 6.5244, 3.3792),
            new City("Nairobi", "", "KE", -1.2921, 36.8219),
            new City("Moscow", "", "RU", 55.7558, 37.6173),
            new City("Paris", "", "FR", 48.8566, 2.3522),
            new City("Berlin", "", "DE", 52.5200, 13.4050),
            new City("Hong Kong", "", "HK", 22.3193, 114.1694),
            new City("Bangkok", "", "TH", 13.7563, 100.5018),
            new City("Kuala Lumpur", "", "MY", 3.1390, 101.6869),
            new City("Tokyo", "", "JP", 35.6762, 139.6503),
            new City("Sydney", "", "AU", -33.8688, 151.2093),
            new City("Kathmandu", "", "NP", 27.7172, 85.3240),
            new City("Dhaka", "", "BD", 23.8103, 90.4125),
            new City("Colombo", "", "LK", 6.9271, 79.8612),
            new City("Karachi", "", "PK", 24.8607, 67.0011),
            new City("Sao Paulo", "", "BR", -23.5505, -46.6333)
    );

    public List<CityDto> homeCities() {
        return INDIA.stream()
                .sorted(Comparator.comparing(City::name))
                .map(c -> new CityDto(c.name(), c.state(), c.country(), c.lat(), c.lon()))
                .toList();
    }

    public Optional<City> findHomeCity(String name) {
        if (name == null) return Optional.empty();
        return INDIA.stream().filter(c -> c.name().equalsIgnoreCase(name.trim())).findFirst();
    }

    public List<City> foreignCities() {
        return WORLD;
    }

    /** Nearest known city to a GPS position. */
    public GeoMatch nearest(double lat, double lon) {
        City best = null;
        double bestDistance = Double.MAX_VALUE;
        for (List<City> list : List.of(INDIA, WORLD)) {
            for (City c : list) {
                double d = distanceKm(lat, lon, c.lat(), c.lon());
                if (d < bestDistance) {
                    bestDistance = d;
                    best = c;
                }
            }
        }
        String label = bestDistance <= 150 ? best.name() : "Near " + best.name();
        return new GeoMatch(label, best.country(), Math.round(bestDistance * 10) / 10.0);
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
}
