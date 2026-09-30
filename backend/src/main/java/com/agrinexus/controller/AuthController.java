package com.agrinexus.controller;

import com.agrinexus.dto.AuthResponse;
import com.agrinexus.dto.LoginRequest;
import com.agrinexus.dto.RegisterRequest;
import com.agrinexus.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.agrinexus.dto.ForgotPasswordRequest;
import com.agrinexus.dto.ForgotPasswordResponse;
import com.agrinexus.dto.ResetPasswordRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    /*
     * =========================================================
     * Register
     * =========================================================
     *
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        authService.register(
                                request
                        )
                );
    }


    /*
     * =========================================================
     * Login
     * =========================================================
     *
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return ResponseEntity.ok(
                authService.login(
                        request
                )
        );
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {

        return ResponseEntity.ok(
                authService.forgotPassword(
                        request.getEmail()
                )
        );
    }
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {

        return ResponseEntity.ok(
                authService.resetPassword(
                        request
                )
        );
    }
}