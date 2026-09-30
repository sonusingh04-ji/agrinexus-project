package com.agrinexus.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CooperativeSummaryResponse {

    private UUID cooperativeId;

    private String cooperativeName;

    private String village;

    private String district;

    private String state;

    private String status;

    private Integer activeMemberCount;

    private Integer totalMemberCount;

    private Integer resourceCount;

    private Integer availableResourceCount;

    private Integer totalUsageRecords;

    private Integer completedUsageRecords;

    private Double totalUsageCostInr;

    private Double totalMemberShareInr;

    private String dataStatus;

    private String source;

    private String uncertaintyNote;
}