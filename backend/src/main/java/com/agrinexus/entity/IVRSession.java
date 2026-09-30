package com.agrinexus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ivr_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IVRSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /*
     * Caller reference.
     *
     * In production this can be a hashed/secured
     * phone identifier instead of storing raw numbers.
     */
    @Column(length = 100)
    private String callerReference;

    /*
     * EN / HI
     */
    @Column(nullable = false, length = 10)
    private String language;

    /*
     * ACTIVE
     * COMPLETED
     * EXPIRED
     */
    @Column(nullable = false, length = 30)
    private String status;

    /*
     * Current IVR menu state.
     */
    @Column(length = 50)
    private String currentMenu;

    private LocalDateTime startedAt;

    private LocalDateTime lastInteractionAt;

    private LocalDateTime expiresAt;

    @Column(length = 1000)
    private String notes;
}