package com.hoangphuc.ddd.domain.model.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Passenger groups that get a fare discount.
 *
 * Lives in the DOMAIN, not in the controller or the frontend: this rule
 * decides how much money we take from the customer. The frontend has an
 * identical rate table (DISCOUNT_RATE in types.ts), but it is for DISPLAY
 * only. If the server trusted a number sent by the client, anyone editing
 * the request could give themselves 100% off. The client only says "I am a
 * student"; the server decides how much that is worth.
 */
public enum PassengerDiscount {

    NONE(0),
    STUDENT(10),
    CHILD(25),
    SENIOR(15);

    /** Discount in percent. Integer so we avoid double rounding errors (0.1 + 0.2 != 0.3). */
    private final int percent;

    PassengerDiscount(int percent) {
        this.percent = percent;
    }

    public int percent() {
        return percent;
    }

    /**
     * Price after discount, rounded to the nearest 1,000 VND — same rule as the
     * frontend finalPriceOf(), so the amount on screen matches the amount charged.
     *
     * BigDecimal + HALF_UP instead of Math.round(double): money must not depend
     * on floating point error, especially right at the ...500 boundary, where a
     * tiny error flips the rounding direction and changes the price by 1,000 VND.
     */
    public long apply(long basePrice) {
        return BigDecimal.valueOf(basePrice)
                .multiply(BigDecimal.valueOf(100 - percent))
                .divide(BigDecimal.valueOf(100_000), 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(1_000))
                .longValueExact();
    }
}
