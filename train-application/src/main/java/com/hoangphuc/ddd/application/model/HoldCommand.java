package com.hoangphuc.ddd.application.model;

import java.util.List;

/**
 * A hold request, grouped into one type instead of loose parameters.
 *
 * With several consecutive parameters, three of them strings, swapping any two
 * still compiles and runs — just with the wrong stations. With a record, using
 * the wrong field name is an immediate error.
 */
public record HoldCommand(
        Long tripId,
        String seatClass,
        List<String> seatIds,
        String fromCode,
        String toCode,
        Long userId,
        /** Waiting room admission token (header X-Queue-Token). Without it, no hold. */
        String queueToken) {
}
