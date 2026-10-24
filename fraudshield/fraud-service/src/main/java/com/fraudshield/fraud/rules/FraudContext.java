package com.fraudshield.fraud.rules;

import com.fraudshield.fraud.event.PaymentRequestedEvent;
import com.fraudshield.fraud.repository.ProfileRepository.Profile;

import java.time.ZonedDateTime;

/**
 * Everything the rules need to judge one payment:
 * the payment itself + what we know about this customer's normal behaviour.
 *
 * @param event              the payment (with home city, device and live location)
 * @param profile            learned normal behaviour, or null for a customer with no history
 * @param knownDevice        device was used before (or is the device bound at login)
 * @param knownReceiver      customer has paid this receiver before
 * @param recentPayments     payments by this customer in the last 10 minutes (before this one)
 * @param distanceFromHomeKm distance between live location and home city, null if location unknown
 * @param localTime          payment time in the customer's time zone (IST)
 */
public record FraudContext(
        PaymentRequestedEvent event,
        Profile profile,
        boolean knownDevice,
        boolean knownReceiver,
        long recentPayments,
        Double distanceFromHomeKm,
        ZonedDateTime localTime
) {
    public static final double FAR_FROM_HOME_KM = 500;

    public boolean hasLocation() {
        return event.latitude() != null && event.longitude() != null;
    }

    public boolean isForeign() {
        return hasLocation() && event.locationCountry() != null
                && !event.locationCountry().equalsIgnoreCase(event.senderHomeCountry());
    }

    public boolean isFarFromHome() {
        return distanceFromHomeKm != null && distanceFromHomeKm > FAR_FROM_HOME_KM;
    }

    public boolean isNewDevice() {
        return !knownDevice;
    }
}
