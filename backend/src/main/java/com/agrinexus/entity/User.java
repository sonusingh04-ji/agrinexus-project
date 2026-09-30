package com.agrinexus.entity;

import com.agrinexus.enums.Language;
import com.agrinexus.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_email",
                        columnNames = "email"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /*
     * BCrypt password hash.
     * Never store plain-text passwords.
     */
    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(nullable = false)
    private Boolean active;

    /*
     * Farmer mobile number.
     */
    @Column(length = 20)
    private String mobileNumber;

    /*
     * URL or application endpoint for the profile photo.
     */
    @Column(length = 1000)
    private String profilePhotoUrl;

    /*
     * Preferred language.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Language preferredLanguage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;

        updatedAt = now;

        if (active == null) {
            active = true;
        }

        if (role == null) {
            role = Role.FARMER;
        }

        if (preferredLanguage == null) {
            preferredLanguage = Language.HI;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}