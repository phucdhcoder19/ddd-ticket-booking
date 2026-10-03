package com.hoangphuc.ddd.application.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * The current sale window — feeds the countdown on the home page.
 */
@Data
public class SaleWindowDTO {

    /** When the sale opens. If it is already open, this moment is in the past. */
    private LocalDateTime opensAt;

    private String label;

    /**
     * LOMBOK + JACKSON TRAP: a boolean field "isOpen" generates the getter isOpen(),
     * Jackson strips the "is" prefix and names the JSON property "open" — a
     * frontend reading res.isOpen gets undefined with no error anywhere.
     * @JsonProperty forces the right name.
     */
    @JsonProperty("isOpen")
    private boolean isOpen;

    /**
     * SERVER time. The countdown must count from this moment, not the client's
     * clock — the client can change its clock, and can sleep and wake up.
     */
    private LocalDateTime serverNow;
}
