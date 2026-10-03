package com.hoangphuc.ddd.domain.service;

import com.hoangphuc.ddd.domain.model.entity.TrainCarriage;

/**
 * FARE RULES.
 *
 * Fare = distance × rate per seat class × berth level factor × peak surcharge
 *
 * Four factors, and three of them are only known WHEN THE PASSENGER SEARCHES:
 * the distance depends on the station pair, the surcharge on the travel date.
 * That is why the seat table has no price column — a price stored on a seat
 * would only be right for one station pair and one date.
 */
public final class FarePolicy {

    private FarePolicy() {
    }

    /** Base rate in VND per kilometre. */
    private static long pricePerKm(String seatClass) {
        return switch (seatClass) {
            case TrainCarriage.CLASS_SOFT_SEAT -> 620L;
            case TrainCarriage.CLASS_BERTH_6   -> 900L;
            case TrainCarriage.CLASS_BERTH_4   -> 1180L;
            default -> 0L;
        };
    }

    /**
     * Factor by berth level. Level 1 is the lowest, easiest to get in and out
     * of and to store luggage — so it is the most expensive. Higher is cheaper.
     */
    private static double berthFactor(int berthLevel) {
        return switch (berthLevel) {
            case 1 -> 1.00;
            case 2 -> 0.92;
            case 3 -> 0.85;
            default -> 1.00;   // seating carriage, no levels
        };
    }

    /**
     * @param distanceKm     distance between the departure and arrival stations
     * @param seatClass      seat class
     * @param berthLevel     berth level, 0 for a seating carriage
     * @param peakSurcharge  peak factor, 1.0 on normal days
     * @return the fare, rounded to the nearest 1,000 VND
     */
    public static long fare(int distanceKm, String seatClass, int berthLevel, double peakSurcharge) {
        double raw = (double) distanceKm
                * pricePerKm(seatClass)
                * berthFactor(berthLevel)
                * peakSurcharge;
        // Round to thousands: fares are never shown down to single dong
        return Math.round(raw / 1000d) * 1000L;
    }
}
