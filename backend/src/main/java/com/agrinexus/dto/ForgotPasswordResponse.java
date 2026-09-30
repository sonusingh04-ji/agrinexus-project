package com.agrinexus.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordResponse {

    private String message;

    /*
     * Development/testing only.
     *
     * This should be null when email delivery is configured
     * for a real deployment.
     */
    private String resetToken;
}