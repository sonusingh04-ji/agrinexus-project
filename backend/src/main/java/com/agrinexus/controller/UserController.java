package com.agrinexus.controller;

import com.agrinexus.dto.UserProfileResponse;
import com.agrinexus.dto.UserProfileUpdateRequest;
import com.agrinexus.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /*
     * Development upload directory.
     */
    private static final Path PROFILE_UPLOAD_DIR =
            Paths.get("uploads", "profile");


    /*
     * =========================================================
     * GET MY PROFILE
     * =========================================================
     *
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                userService.getMyProfile(
                        authentication
                )
        );
    }


    /*
     * =========================================================
     * UPDATE MY PROFILE
     * =========================================================
     *
     * PUT /api/users/me
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            Authentication authentication,

            @Valid
            @RequestBody
            UserProfileUpdateRequest request
    ) {

        return ResponseEntity.ok(
                userService.updateMyProfile(
                        authentication,
                        request
                )
        );
    }


    /*
     * =========================================================
     * UPLOAD PROFILE PHOTO
     * =========================================================
     *
     * POST /api/users/me/photo
     */
    @PostMapping(
            value = "/me/photo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserProfileResponse> uploadProfilePhoto(
            Authentication authentication,

            @RequestParam("file")
            MultipartFile file
    ) throws IOException {

        if (file == null
                || file.isEmpty()) {

            return ResponseEntity.badRequest()
                    .build();
        }


        /*
         * Accept image files only.
         */
        String contentType =
                file.getContentType();

        if (contentType == null
                || !contentType.startsWith("image/")) {

            return ResponseEntity
                    .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .build();
        }


        /*
         * Development limit: 5 MB.
         */
        if (file.getSize()
                > 5 * 1024 * 1024) {

            return ResponseEntity
                    .status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .build();
        }


        /*
         * Create upload folder.
         */
        Files.createDirectories(
                PROFILE_UPLOAD_DIR
        );


        /*
         * Delete old photo if it exists.
         */
        UserProfileResponse currentProfile =
                userService.getMyProfile(
                        authentication
                );

        deleteExistingPhoto(
                currentProfile.getProfilePhotoUrl()
        );


        /*
         * Determine extension.
         */
        String originalFilename =
                file.getOriginalFilename();

        String extension =
                getSafeExtension(
                        originalFilename
                );


        /*
         * Generate random server-side filename.
         */
        String filename =
                UUID.randomUUID()
                        + extension;


        Path target =
                PROFILE_UPLOAD_DIR.resolve(
                        filename
                );


        /*
         * Save file.
         */
        Files.copy(
                file.getInputStream(),
                target,
                StandardCopyOption.REPLACE_EXISTING
        );


        /*
         * URL used by frontend.
         */
        String photoUrl =
                "/api/users/me/photo/" +
                        filename;


        UserProfileResponse response =
                userService.updateProfilePhoto(
                        authentication,
                        photoUrl
                );


        return ResponseEntity.ok(
                response
        );
    }


    /*
     * =========================================================
     * SERVE MY PROFILE PHOTO
     * =========================================================
     *
     * GET /api/users/me/photo/{filename}
     */
    @GetMapping("/me/photo/{filename}")
    public ResponseEntity<byte[]> getProfilePhoto(
            Authentication authentication,

            @PathVariable String filename
    ) throws IOException {

        /*
         * Make sure the filename cannot escape
         * the profile directory.
         */
        Path file =
                PROFILE_UPLOAD_DIR
                        .resolve(filename)
                        .normalize();


        if (!file.startsWith(
                PROFILE_UPLOAD_DIR.toAbsolutePath()
        ) && !file.startsWith(
                PROFILE_UPLOAD_DIR
        )) {

            return ResponseEntity
                    .badRequest()
                    .build();
        }


        if (!Files.exists(file)
                || !Files.isRegularFile(file)) {

            return ResponseEntity
                    .notFound()
                    .build();
        }


        /*
         * Ensure the requested profile photo
         * belongs to the authenticated user.
         */
        UserProfileResponse profile =
                userService.getMyProfile(
                        authentication
                );

        String savedUrl =
                profile.getProfilePhotoUrl();


        if (savedUrl == null
                || !savedUrl.endsWith(
                "/" + filename
        )) {

            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }


        byte[] bytes =
                Files.readAllBytes(file);


        MediaType mediaType =
                detectImageMediaType(
                        file
                );


        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(bytes);
    }


    /*
     * =========================================================
     * DELETE PROFILE PHOTO
     * =========================================================
     *
     * DELETE /api/users/me/photo
     */
    @DeleteMapping("/me/photo")
    public ResponseEntity<UserProfileResponse>
    deleteProfilePhoto(
            Authentication authentication
    ) {

        try {

            UserProfileResponse profile =
                    userService.getMyProfile(
                            authentication
                    );

            deleteExistingPhoto(
                    profile.getProfilePhotoUrl()
            );

            return ResponseEntity.ok(
                    userService.removeProfilePhoto(
                            authentication
                    )
            );

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Unable to remove profile photo",
                    exception
            );
        }
    }


    /*
     * =========================================================
     * Helpers
     * =========================================================
     */

    private void deleteExistingPhoto(
            String photoUrl
    ) throws IOException {

        if (photoUrl == null
                || photoUrl.isBlank()) {

            return;
        }


        String filename =
                photoUrl.substring(
                        photoUrl.lastIndexOf("/")
                                + 1
                );


        if (filename.isBlank()) {
            return;
        }


        Path existing =
                PROFILE_UPLOAD_DIR
                        .resolve(filename)
                        .normalize();


        if (existing.startsWith(
                PROFILE_UPLOAD_DIR
        )
                && Files.exists(existing)) {

            Files.deleteIfExists(
                    existing
            );
        }
    }


    private String getSafeExtension(
            String filename
    ) {

        if (filename == null
                || filename.isBlank()) {

            return ".jpg";
        }


        String lower =
                filename.toLowerCase();


        if (lower.endsWith(".png")) {
            return ".png";
        }

        if (lower.endsWith(".jpeg")) {
            return ".jpeg";
        }

        if (lower.endsWith(".jpg")) {
            return ".jpg";
        }

        if (lower.endsWith(".webp")) {
            return ".webp";
        }

        return ".jpg";
    }


    private MediaType detectImageMediaType(
            Path file
    ) throws IOException {

        String type =
                Files.probeContentType(file);


        if ("image/png".equals(type)) {
            return MediaType.IMAGE_PNG;
        }


        if ("image/webp".equals(type)) {
            return MediaType.parseMediaType(
                    "image/webp"
            );
        }


        if ("image/jpeg".equals(type)) {
            return MediaType.IMAGE_JPEG;
        }


        return MediaType.IMAGE_JPEG;
    }
}