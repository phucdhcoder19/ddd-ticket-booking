package com.hoangphuc.ddd.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * PUT /holds/{holdCode}/passengers
 *
 * The rules here MIRROR the frontend rules (lib/validate.ts): whatever the
 * frontend accepts, the server must accept too, otherwise a customer who
 * filled everything in correctly still gets rejected. The frontend validates
 * to show friendly errors; the server validates because the client can never
 * be trusted.
 */
@Data
public class SavePassengersRequest {

    @NotEmpty(message = "at least 1 passenger is required")
    @Size(max = 4, message = "a hold has at most 4 seats")
    @Valid
    private List<PassengerRequest> passengers;

    @Data
    public static class PassengerRequest {

        @NotBlank(message = "seatId is required")
        private String seatId;

        /** At least two words (first + last name), letters, spaces and apostrophes only. */
        @NotBlank(message = "full name is required")
        @Size(max = 100, message = "full name is at most 100 characters")
        @Pattern(regexp = "^\\s*[\\p{L}']+(\\s+[\\p{L}']+)+\\s*$",
                 message = "full name must include first and last name, letters only")
        private String fullName;

        /** 12-digit national ID, or a 9-digit legacy ID card. */
        @NotBlank(message = "ID number is required")
        @Pattern(regexp = "^\\s*(\\d{12}|\\d{9})\\s*$",
                 message = "ID number must have 12 digits (or 9 for a legacy ID card)")
        private String idNumber;

        /** 0912345678, 0912 345 678, 0912.345.678 and +84912345678 are all valid. */
        @NotBlank(message = "phone number is required")
        @Pattern(regexp = "^\\s*(\\+84|0)([\\s.]*\\d){9}[\\s.]*$",
                 message = "phone number must have 10 digits and start with 0")
        private String phone;

        @Pattern(regexp = "NONE|STUDENT|CHILD|SENIOR",
                 message = "discount must be one of NONE, STUDENT, CHILD, SENIOR")
        private String discount;
    }
}
