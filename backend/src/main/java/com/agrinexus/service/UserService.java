package com.agrinexus.service;

import com.agrinexus.dto.UserProfileResponse;
import com.agrinexus.dto.UserProfileUpdateRequest;
import com.agrinexus.entity.User;
import com.agrinexus.enums.Language;
import com.agrinexus.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;


    /*
     * =========================================================
     * Get authenticated user
     * =========================================================
     *
     * Your JwtAuthenticationFilter creates the authenticated
     * principal using the actual User entity.
     *
     * Therefore we first use authentication.getPrincipal().
     *
     * Fallback:
     * If another authentication implementation supplies only
     * the UUID as authentication.getName(), we also support that.
     */
    public User getAuthenticatedUser(
            Authentication authentication
    ) {

        if (authentication == null) {

            throw new RuntimeException(
                    "Authentication is required"
            );
        }


        /*
         * =====================================================
         * Preferred method
         * =====================================================
         *
         * JwtAuthenticationFilter does:
         *
         * new UsernamePasswordAuthenticationToken(
         *      user,
         *      null,
         *      authorities
         * )
         *
         * Therefore the principal is User.
         */
        Object principal =
                authentication.getPrincipal();


        if (principal instanceof User user) {

            /*
             * Make sure the user still exists and is active.
             */
            if (user.getId() == null) {

                throw new RuntimeException(
                        "Authenticated user ID is missing"
                );
            }

            return userRepository
                    .findById(user.getId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Authenticated user not found"
                            )
                    );
        }


        /*
         * =====================================================
         * Fallback
         * =====================================================
         *
         * Your JwtService sets:
         *
         * subject(user.getId().toString())
         *
         * So authentication.getName() may be the UUID.
         */
        String authenticationName =
                authentication.getName();


        if (authenticationName == null
                || authenticationName.isBlank()) {

            throw new RuntimeException(
                    "Authenticated user ID is missing"
            );
        }


        try {

            UUID userId =
                    UUID.fromString(
                            authenticationName
                    );


            return userRepository
                    .findById(userId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Authenticated user not found"
                            )
                    );

        } catch (IllegalArgumentException ex) {

            throw new RuntimeException(
                    "Authenticated user ID is invalid"
            );
        }
    }


    /*
     * =========================================================
     * Get current profile
     * =========================================================
     */
    public UserProfileResponse getMyProfile(
            Authentication authentication
    ) {

        User user =
                getAuthenticatedUser(
                        authentication
                );

        return toResponse(user);
    }


    /*
     * =========================================================
     * Update current profile
     * =========================================================
     */
    @Transactional
    public UserProfileResponse updateMyProfile(
            Authentication authentication,
            UserProfileUpdateRequest request
    ) {

        User user =
                getAuthenticatedUser(
                        authentication
                );


        if (request == null) {

            throw new RuntimeException(
                    "Profile update data is required"
            );
        }


        String newEmail =
                request.getEmail()
                        .trim()
                        .toLowerCase();


        /*
         * Check whether another account already uses
         * the requested email.
         */
        if (!user.getEmail()
                .equalsIgnoreCase(newEmail)) {

            userRepository
                    .findByEmailIgnoreCase(newEmail)
                    .ifPresent(existingUser -> {

                        if (!existingUser
                                .getId()
                                .equals(user.getId())) {

                            throw new RuntimeException(
                                    "Email is already registered"
                            );
                        }
                    });
        }


        user.setFirstName(
                request.getFirstName()
                        .trim()
        );


        user.setLastName(
                request.getLastName()
                        .trim()
        );


        user.setEmail(
                newEmail
        );


        /*
         * Mobile number is optional.
         */
        String mobile =
                request.getMobileNumber();


        if (mobile != null) {

            mobile =
                    mobile.trim();

            if (mobile.isBlank()) {
                mobile = null;
            }
        }


        user.setMobileNumber(
                mobile
        );


        /*
         * Preferred language.
         */
        if (request.getPreferredLanguage()
                != null) {

            user.setPreferredLanguage(
                    request.getPreferredLanguage()
            );

        } else if (
                user.getPreferredLanguage()
                        == null
        ) {

            user.setPreferredLanguage(
                    Language.HI
            );
        }


        User savedUser =
                userRepository.save(user);


        return toResponse(
                savedUser
        );
    }


    /*
     * =========================================================
     * Update profile photo
     * =========================================================
     */
    @Transactional
    public UserProfileResponse updateProfilePhoto(
            Authentication authentication,
            String photoUrl
    ) {

        User user =
                getAuthenticatedUser(
                        authentication
                );


        user.setProfilePhotoUrl(
                photoUrl
        );


        User savedUser =
                userRepository.save(user);


        return toResponse(
                savedUser
        );
    }


    /*
     * =========================================================
     * Remove profile photo
     * =========================================================
     */
    @Transactional
    public UserProfileResponse removeProfilePhoto(
            Authentication authentication
    ) {

        User user =
                getAuthenticatedUser(
                        authentication
                );


        user.setProfilePhotoUrl(
                null
        );


        User savedUser =
                userRepository.save(user);


        return toResponse(
                savedUser
        );
    }


    /*
     * =========================================================
     * Entity → Response
     * =========================================================
     */
    private UserProfileResponse toResponse(
            User user
    ) {

        return UserProfileResponse
                .builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobileNumber(
                        user.getMobileNumber()
                )
                .profilePhotoUrl(
                        user.getProfilePhotoUrl()
                )
                .preferredLanguage(
                        user.getPreferredLanguage()
                )
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}