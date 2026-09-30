package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CooperativeMemberRequest {

    private UUID cooperativeId;

    private UUID farmerId;

    private UUID farmId;

    /*
     * MEMBER
     * COORDINATOR
     */
    private String role;

    /*
     * ACTIVE
     * INACTIVE
     */
    private String status;

    private String notes;
}