package com.agrinexus.dto;

import com.agrinexus.enums.Role;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private UUID userId;

    private String firstName;

    private String lastName;

    private String email;

    private Role role;

    private Boolean active;

    /*
     * JWT will be added in the next authentication stage.
     */
    private String token;

    private String tokenType;
}