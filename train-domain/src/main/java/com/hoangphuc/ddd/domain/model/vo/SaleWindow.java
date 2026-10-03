package com.hoangphuc.ddd.domain.model.vo;

import java.time.LocalDateTime;

/**
 * Value object: the sale window currently in effect.
 *
 * No id, not stored in any table — it is DERIVED from the ACTIVE tickets. Two
 * SaleWindows with the same values are the same thing; that is the mark of a
 * value object, unlike entities (Station, TicketDetail), which are told apart
 * by their id.
 *
 * @param opensAt when the sale window under consideration opens
 * @param open    whether the sale is already open at the moment of asking
 */
public record SaleWindow(LocalDateTime opensAt, boolean open) {

    /** No ticket has a sale schedule configured — treat it as not open. */
    public static SaleWindow closedAt(LocalDateTime now) {
        return new SaleWindow(now, false);
    }
}
