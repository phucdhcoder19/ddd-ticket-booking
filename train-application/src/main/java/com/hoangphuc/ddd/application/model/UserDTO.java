package com.hoangphuc.ddd.application.model;

import lombok.Data;

/** The signed-in customer, as the frontend sees it. Never carries the password hash. */
@Data
public class UserDTO {
    private Long id;
    private String phone;
    private String fullName;
}
