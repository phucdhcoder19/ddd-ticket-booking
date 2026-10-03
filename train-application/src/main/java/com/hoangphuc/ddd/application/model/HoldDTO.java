package com.hoangphuc.ddd.application.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HoldDTO {

    /** Public code of the hold. Never the auto-increment id. */
    private String holdId;

    private String tripId;

    private String fromCode;
    private String toCode;

    /** Seats being held, with the price of each. */
    private List<HoldItemDTO> items;

    /** Provisional total — before each passenger's discount. */
    private long totalAmount;

    /** When it expires — the client draws its timer from this moment. */
    private LocalDateTime expiresAt;

    /** SERVER time. The client must count from this, not from its own clock. */
    private LocalDateTime serverNow;

    /** Precomputed so the client does not have to subtract the two timestamps. */
    private long secondsLeft;

    /** HOLDING | USED | RELEASED | EXPIRED */
    private String status;
}
