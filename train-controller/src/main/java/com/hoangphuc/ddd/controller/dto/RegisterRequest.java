package com.hoangphuc.ddd.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    /** Checked again after normalising (AuthController), since "+84 912 345 678" is valid input. */
    @NotBlank(message = "phone is required")
    private String phone;

    @NotBlank(message = "fullName is required")
    @Size(min = 2, max = 100, message = "fullName must be 2-100 characters")
    private String fullName;

    /** 72 is BCrypt's limit: anything longer is silently cut, so reject it instead. */
    @NotBlank(message = "password is required")
    @Size(min = 8, max = 72, message = "password must be 8-72 characters")
    private String password;
}
