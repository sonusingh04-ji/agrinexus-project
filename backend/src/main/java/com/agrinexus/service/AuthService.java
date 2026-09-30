package com.agrinexus.service;

import com.agrinexus.dto.AuthResponse;
import com.agrinexus.dto.ForgotPasswordResponse;
import com.agrinexus.dto.LoginRequest;
import com.agrinexus.dto.RegisterRequest;
import com.agrinexus.dto.ResetPasswordRequest;
import com.agrinexus.entity.PasswordResetToken;
import com.agrinexus.entity.User;
import com.agrinexus.enums.Role;
import com.agrinexus.repository.PasswordResetTokenRepository;
import com.agrinexus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final PasswordResetTokenRepository
            passwordResetTokenRepository;

    @Value("${agrinexus.auth.reset-token-expiration-minutes:30}")
    private long resetTokenExpirationMinutes;

    @Value("${agrinexus.auth.expose-reset-token:false}")
    private boolean exposeResetToken;


    /*
     * =========================================================
     * Register
     * =========================================================
     */
    public AuthResponse register(
            RegisterRequest request
    ) {

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        if (
                userRepository.existsByEmailIgnoreCase(
                        email
                )
        ) {

            throw new RuntimeException(
                    "Email is already registered"
            );
        }

        Role role =
                request.getRole() == null
                        ? Role.FARMER
                        : request.getRole();

        if (role == Role.ADMIN) {

            throw new RuntimeException(
                    "ADMIN role cannot be assigned through public registration"
            );
        }

        User user =
                User.builder()
                        .firstName(
                                request.getFirstName().trim()
                        )
                        .lastName(
                                request.getLastName().trim()
                        )
                        .email(email)
                        .password(
                                passwordEncoder.encode(
                                        request.getPassword()
                                )
                        )
                        .role(role)
                        .active(true)
                        .build();

        user =
                userRepository.save(user);

        String token =
                jwtService.generateToken(
                        user
                );

        return toResponse(
                user,
                token
        );
    }


    /*
     * =========================================================
     * Login
     * =========================================================
     */
    public AuthResponse login(
            LoginRequest request
    ) {

        String email =
                request.getEmail()
                        .trim();

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                email
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid email or password"
                                )
                        );

        if (
                user.getActive() == null
                        || !user.getActive()
        ) {

            throw new RuntimeException(
                    "User account is inactive"
            );
        }

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!passwordMatches) {

            throw new RuntimeException(
                    "Invalid email or password"
            );
        }

        String token =
                jwtService.generateToken(
                        user
                );

        return toResponse(
                user,
                token
        );
    }


    /*
     * =========================================================
     * Forgot password
     * =========================================================
     *
     * Generates a short-lived reset token.
     *
     * The raw token is returned only when
     * expose-reset-token=true.
     *
     * The database stores only the SHA-256 hash.
     */
    @Transactional
    public ForgotPasswordResponse forgotPassword(
            String email
    ) {

        String normalizedEmail =
                email
                        .trim()
                        .toLowerCase();


        /*
         * We intentionally use the same generic response for
         * unknown emails to avoid exposing whether an account
         * exists.
         */
        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                normalizedEmail
                        )
                        .orElse(null);


        if (user == null) {

            return ForgotPasswordResponse.builder()

                    .message(
                            "If an account exists for this email, a password reset request has been created."
                    )

                    .resetToken(
                            null
                    )

                    .build();
        }


        /*
         * Delete previous unused reset tokens for the user.
         *
         * This keeps one active reset flow at a time.
         */
        passwordResetTokenRepository
                .findAll()
                .stream()
                .filter(resetToken ->
                        resetToken.getUser() != null
                                && user.getId().equals(
                                resetToken.getUser().getId()
                        )
                                && Boolean.FALSE.equals(
                                resetToken.getUsed()
                        )
                )
                .forEach(
                        passwordResetTokenRepository::delete
                );


        /*
         * Generate cryptographically strong random token.
         */
        String rawToken =
                generateSecureToken();


        String tokenHash =
                hashToken(
                        rawToken
                );


        LocalDateTime expiresAt =
                LocalDateTime.now()
                        .plusMinutes(
                                resetTokenExpirationMinutes
                        );


        PasswordResetToken resetToken =
                PasswordResetToken.builder()

                        .user(
                                user
                        )

                        .tokenHash(
                                tokenHash
                        )

                        .expiresAt(
                                expiresAt
                        )

                        .used(
                                false
                        )

                        .build();


        passwordResetTokenRepository.save(
                resetToken
        );


        String responseToken =
                exposeResetToken
                        ? rawToken
                        : null;


        return ForgotPasswordResponse.builder()

                .message(
                        "If an account exists for this email, a password reset request has been created."
                )

                .resetToken(
                        responseToken
                )

                .build();
    }


    /*
     * =========================================================
     * Reset password
     * =========================================================
     */
    @Transactional
    public String resetPassword(
            ResetPasswordRequest request
    ) {

        String token =
                request.getToken()
                        .trim();


        String tokenHash =
                hashToken(
                        token
                );


        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByTokenHashAndUsedFalse(
                                tokenHash
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid or expired password reset token"
                                )
                        );


        /*
         * Check expiration.
         */
        if (
                resetToken.getExpiresAt() == null
                        || resetToken.getExpiresAt()
                        .isBefore(
                                LocalDateTime.now()
                        )
        ) {

            throw new RuntimeException(
                    "Invalid or expired password reset token"
            );
        }


        User user =
                resetToken.getUser();


        if (user == null) {

            throw new RuntimeException(
                    "User account could not be found"
            );
        }


        if (
                user.getActive() == null
                        || !user.getActive()
        ) {

            throw new RuntimeException(
                    "User account is inactive"
            );
        }


        /*
         * Update BCrypt password.
         */
        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );


        userRepository.save(
                user
        );


        /*
         * Mark the reset token as used.
         */
        resetToken.setUsed(
                true
        );

        resetToken.setUsedAt(
                LocalDateTime.now()
        );


        passwordResetTokenRepository.save(
                resetToken
        );


        return
                "Password reset successfully. Please login with your new password.";
    }


    /*
     * =========================================================
     * Generate secure token
     * =========================================================
     */
    private String generateSecureToken() {

        byte[] randomBytes =
                new byte[32];


        new java.security.SecureRandom()
                .nextBytes(
                        randomBytes
                );


        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        randomBytes
                );
    }


    /*
     * =========================================================
     * SHA-256 hash
     * =========================================================
     */
    private String hashToken(
            String token
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );


            byte[] hash =
                    digest.digest(
                            token.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );


            StringBuilder result =
                    new StringBuilder();


            for (byte value : hash) {

                result.append(
                        String.format(
                                "%02x",
                                value
                        )
                );
            }


            return result.toString();

        } catch (NoSuchAlgorithmException ex) {

            throw new RuntimeException(
                    "Unable to securely process password reset token",
                    ex
            );
        }
    }


    /*
     * =========================================================
     * Entity -> AuthResponse
     * =========================================================
     */
    private AuthResponse toResponse(
            User user,
            String token
    ) {

        return AuthResponse.builder()

                .userId(
                        user.getId()
                )

                .firstName(
                        user.getFirstName()
                )

                .lastName(
                        user.getLastName()
                )

                .email(
                        user.getEmail()
                )

                .role(
                        user.getRole()
                )

                .active(
                        user.getActive()
                )

                .token(
                        token
                )

                .tokenType(
                        token == null
                                ? null
                                : "Bearer"
                )

                .build();
    }
}