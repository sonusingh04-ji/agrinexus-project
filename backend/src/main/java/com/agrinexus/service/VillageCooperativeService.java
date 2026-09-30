package com.agrinexus.service;

import com.agrinexus.dto.*;
import com.agrinexus.entity.*;
import com.agrinexus.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VillageCooperativeService {

    private final VillageCooperativeRepository cooperativeRepository;

    private final VillageCooperativeMemberRepository memberRepository;

    private final SharedFarmResourceRepository resourceRepository;

    private final SharedResourceUsageRepository usageRepository;

    private final FarmRepository farmRepository;


    /*
     * =========================================================
     * CREATE COOPERATIVE
     * =========================================================
     */

    public VillageCooperativeResponse createCooperative(
            VillageCooperativeRequest request
    ) {

        validateCooperativeRequest(request);


        /*
         * If the coordinator has a farm in the system,
         * verify that the supplied farmer ID belongs to it.
         *
         * We do not require a Farmer entity because the
         * existing Farm model already stores farmerId.
         */
        if (request.getCoordinatorFarmerId() != null) {

            boolean coordinatorHasFarm =
                    farmRepository
                            .findAll()
                            .stream()
                            .anyMatch(
                                    farm ->
                                            request
                                                    .getCoordinatorFarmerId()
                                                    .equals(
                                                            farm.getFarmerId()
                                                    )
                            );


            if (!coordinatorHasFarm) {

                throw new RuntimeException(
                        "Coordinator farmer does not have "
                                + "a registered farm"
                );
            }
        }


        VillageCooperative cooperative =
                VillageCooperative.builder()

                        .name(
                                request.getName()
                        )

                        .village(
                                request.getVillage()
                        )

                        .district(
                                request.getDistrict()
                        )

                        .state(
                                request.getState()
                        )

                        .contactInfo(
                                request.getContactInfo()
                        )

                        .coordinatorFarmerId(
                                request.getCoordinatorFarmerId()
                        )

                        .status(
                                normalizeStatus(
                                        request.getStatus(),
                                        "ACTIVE"
                                )
                        )

                        .dataStatus(
                                request.getDataStatus()
                        )

                        .source(
                                request.getSource()
                        )

                        .notes(
                                request.getNotes()
                        )

                        .build();


        cooperative =
                cooperativeRepository.save(
                        cooperative
                );


        /*
         * Automatically add the coordinator as a member.
         */
        if (request.getCoordinatorFarmerId() != null) {

            addMemberInternal(
                    cooperative,
                    request.getCoordinatorFarmerId(),
                    null,
                    "COORDINATOR",
                    "ACTIVE",
                    "Automatically added as cooperative coordinator."
            );
        }


        return toCooperativeResponse(
                cooperative
        );
    }


    /*
     * =========================================================
     * GET COOPERATIVE
     * =========================================================
     */

    @Transactional(readOnly = true)
    public VillageCooperativeResponse getCooperative(
            UUID cooperativeId
    ) {

        VillageCooperative cooperative =
                getCooperativeEntity(
                        cooperativeId
                );


        return toCooperativeResponse(
                cooperative
        );
    }


    /*
     * =========================================================
     * GET BY VILLAGE
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<VillageCooperativeResponse>
    getByVillage(
            String village
    ) {

        return cooperativeRepository
                .findByVillageIgnoreCase(
                        village
                )
                .stream()
                .map(
                        this::toCooperativeResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * GET ACTIVE COOPERATIVES
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<VillageCooperativeResponse>
    getActiveCooperatives() {

        return cooperativeRepository
                .findByStatusIgnoreCase(
                        "ACTIVE"
                )
                .stream()
                .map(
                        this::toCooperativeResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * UPDATE COOPERATIVE
     * =========================================================
     */

    public VillageCooperativeResponse updateCooperative(
            UUID cooperativeId,
            VillageCooperativeRequest request
    ) {

        validateCooperativeRequest(request);


        VillageCooperative cooperative =
                getCooperativeEntity(
                        cooperativeId
                );


        cooperative.setName(
                request.getName()
        );

        cooperative.setVillage(
                request.getVillage()
        );

        cooperative.setDistrict(
                request.getDistrict()
        );

        cooperative.setState(
                request.getState()
        );

        cooperative.setContactInfo(
                request.getContactInfo()
        );

        cooperative.setCoordinatorFarmerId(
                request.getCoordinatorFarmerId()
        );

        cooperative.setStatus(
                normalizeStatus(
                        request.getStatus(),
                        "ACTIVE"
                )
        );

        cooperative.setDataStatus(
                request.getDataStatus()
        );

        cooperative.setSource(
                request.getSource()
        );

        cooperative.setNotes(
                request.getNotes()
        );


        cooperative =
                cooperativeRepository.save(
                        cooperative
                );


        return toCooperativeResponse(
                cooperative
        );
    }


    /*
     * =========================================================
     * ADD MEMBER
     * =========================================================
     */

    public CooperativeMemberResponse addMember(
            CooperativeMemberRequest request
    ) {

        if (request == null) {

            throw new RuntimeException(
                    "Member request is required"
            );
        }


        if (request.getCooperativeId() == null) {

            throw new RuntimeException(
                    "Cooperative ID is required"
            );
        }


        if (request.getFarmerId() == null) {

            throw new RuntimeException(
                    "Farmer ID is required"
            );
        }


        VillageCooperative cooperative =
                getCooperativeEntity(
                        request.getCooperativeId()
                );


        if (
                memberRepository
                        .existsByCooperativeIdAndFarmerId(
                                cooperative.getId(),
                                request.getFarmerId()
                        )
        ) {

            throw new RuntimeException(
                    "Farmer is already a member "
                            + "of this cooperative"
            );
        }


        Farm farm = null;


        if (request.getFarmId() != null) {

            farm =
                    farmRepository.findById(
                            request.getFarmId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Farm not found with ID: "
                                            + request.getFarmId()
                            )
                    );


            if (
                    !request
                            .getFarmerId()
                            .equals(
                                    farm.getFarmerId()
                            )
            ) {

                throw new RuntimeException(
                        "The selected farm does not "
                                + "belong to the supplied farmer"
                );
            }
        }


        VillageCooperativeMember member =
                addMemberInternal(
                        cooperative,
                        request.getFarmerId(),
                        farm,
                        normalizeRole(
                                request.getRole()
                        ),
                        normalizeStatus(
                                request.getStatus(),
                                "ACTIVE"
                        ),
                        request.getNotes()
                );


        return toMemberResponse(
                member
        );
    }


    /*
     * =========================================================
     * GET MEMBERS
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<CooperativeMemberResponse>
    getMembers(
            UUID cooperativeId
    ) {

        getCooperativeEntity(
                cooperativeId
        );


        return memberRepository
                .findByCooperativeId(
                        cooperativeId
                )
                .stream()
                .map(
                        this::toMemberResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * REMOVE / DEACTIVATE MEMBER
     * =========================================================
     */

    public void removeMember(
            UUID memberId
    ) {

        VillageCooperativeMember member =
                memberRepository.findById(
                        memberId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Cooperative member not found"
                        )
                );


        /*
         * Do not permanently delete the history.
         *
         * Mark inactive so historical resource usage
         * remains traceable.
         */
        member.setStatus(
                "INACTIVE"
        );


        memberRepository.save(
                member
        );
    }


    /*
     * =========================================================
     * ADD SHARED RESOURCE
     * =========================================================
     */

    public SharedFarmResourceResponse addResource(
            SharedFarmResourceRequest request
    ) {

        validateResourceRequest(request);


        VillageCooperative cooperative =
                getCooperativeEntity(
                        request.getCooperativeId()
                );


        if (
                request.getOwnerFarmerId() != null
                        && !isCooperativeMember(
                        cooperative.getId(),
                        request.getOwnerFarmerId()
                )
        ) {

            throw new RuntimeException(
                    "Resource owner must be a member "
                            + "of the cooperative"
            );
        }


        SharedFarmResource resource =
                SharedFarmResource.builder()

                        .cooperative(
                                cooperative
                        )

                        .resourceType(
                                request.getResourceType()
                        )

                        .resourceName(
                                request.getResourceName()
                        )

                        .ownerFarmerId(
                                request.getOwnerFarmerId()
                        )

                        .location(
                                request.getLocation()
                        )

                        .status(
                                normalizeStatus(
                                        request.getStatus(),
                                        "AVAILABLE"
                                )
                        )

                        .estimatedTotalCostInr(
                                request
                                        .getEstimatedTotalCostInr()
                        )

                        .estimatedUsageCostInr(
                                request
                                        .getEstimatedUsageCostInr()
                        )

                        .usageUnit(
                                request.getUsageUnit()
                        )

                        .description(
                                request.getDescription()
                        )

                        .dataStatus(
                                request.getDataStatus()
                        )

                        .source(
                                request.getSource()
                        )

                        .notes(
                                request.getNotes()
                        )

                        .build();


        resource =
                resourceRepository.save(
                        resource
                );


        return toResourceResponse(
                resource
        );
    }


    /*
     * =========================================================
     * GET RESOURCES
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<SharedFarmResourceResponse>
    getResources(
            UUID cooperativeId
    ) {

        getCooperativeEntity(
                cooperativeId
        );


        return resourceRepository
                .findByCooperativeId(
                        cooperativeId
                )
                .stream()
                .map(
                        this::toResourceResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * UPDATE RESOURCE STATUS
     * =========================================================
     */

    public SharedFarmResourceResponse
    updateResourceStatus(
            UUID resourceId,
            String status
    ) {

        SharedFarmResource resource =
                resourceRepository.findById(
                        resourceId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Shared resource not found"
                        )
                );


        if (
                status == null
                        || status.isBlank()
        ) {

            throw new RuntimeException(
                    "Resource status is required"
            );
        }


        resource.setStatus(
                status
                        .trim()
                        .toUpperCase()
        );


        resource =
                resourceRepository.save(
                        resource
                );


        return toResourceResponse(
                resource
        );
    }


    /*
     * =========================================================
     * RECORD RESOURCE USAGE
     * =========================================================
     */

    public SharedResourceUsageResponse recordUsage(
            SharedResourceUsageRequest request
    ) {

        validateUsageRequest(
                request
        );


        SharedFarmResource resource =
                resourceRepository.findById(
                        request.getResourceId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Shared resource not found"
                        )
                );


        VillageCooperative cooperative =
                resource.getCooperative();


        if (
                !isCooperativeMember(
                        cooperative.getId(),
                        request.getFarmerId()
                )
        ) {

            throw new RuntimeException(
                    "Only cooperative members can use "
                            + "shared cooperative resources"
            );
        }


        Farm farm = null;


        if (request.getFarmId() != null) {

            farm =
                    farmRepository.findById(
                            request.getFarmId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Farm not found with ID: "
                                            + request.getFarmId()
                            )
                    );


            if (
                    !request
                            .getFarmerId()
                            .equals(
                                    farm.getFarmerId()
                            )
            ) {

                throw new RuntimeException(
                        "The usage farm does not "
                                + "belong to the supplied farmer"
                );
            }
        }


        if (
                resource.getStatus() != null
                        && (
                        resource.getStatus()
                                .equalsIgnoreCase(
                                        "MAINTENANCE"
                                )
                                ||
                                resource.getStatus()
                                        .equalsIgnoreCase(
                                                "INACTIVE"
                                        )
                )
        ) {

            throw new RuntimeException(
                    "This resource is currently unavailable"
            );
        }


        /*
         * Prevent obviously invalid usage.
         */
        if (
                request.getEndTime() != null
                        && request.getStartTime()
                        .isAfter(
                                request.getEndTime()
                        )
        ) {

            throw new RuntimeException(
                    "End time cannot be before start time"
            );
        }


        /*
         * Automatically calculate usage cost when
         * quantity was supplied and the resource has
         * a known usage rate.
         */
        Double usageCost =
                request.getUsageCostInr();


        if (
                usageCost == null
                        && request.getQuantityUsed() != null
                        && resource
                        .getEstimatedUsageCostInr() != null
        ) {

            usageCost =
                    round(
                            request.getQuantityUsed()
                                    * resource
                                    .getEstimatedUsageCostInr()
                    );
        }


        /*
         * If no explicit member share was supplied,
         * default to the recorded usage cost.
         *
         * This keeps the calculation transparent.
         */
        Double memberShare =
                request.getMemberShareInr();


        if (
                memberShare == null
                        && usageCost != null
        ) {

            memberShare =
                    usageCost;
        }


        SharedResourceUsage usage =
                SharedResourceUsage.builder()

                        .resource(
                                resource
                        )

                        .farmerId(
                                request.getFarmerId()
                        )

                        .farm(
                                farm
                        )

                        .startTime(
                                request.getStartTime()
                        )

                        .endTime(
                                request.getEndTime()
                        )

                        .quantityUsed(
                                request.getQuantityUsed()
                        )

                        .usageCostInr(
                                usageCost
                        )

                        .memberShareInr(
                                memberShare
                        )

                        .status(
                                normalizeStatus(
                                        request.getStatus(),
                                        "COMPLETED"
                                )
                        )

                        .purpose(
                                request.getPurpose()
                        )

                        .dataStatus(
                                request.getDataStatus()
                        )

                        .source(
                                request.getSource()
                        )

                        .notes(
                                request.getNotes()
                        )

                        .build();


        usage =
                usageRepository.save(
                        usage
                );


        /*
         * Resource is considered in-use while the
         * usage is planned/current.
         */
        if (
                usage.getStatus()
                        .equalsIgnoreCase(
                                "PLANNED"
                        )
        ) {

            resource.setStatus(
                    "IN_USE"
            );

            resourceRepository.save(
                    resource
            );
        }


        return toUsageResponse(
                usage
        );
    }


    /*
     * =========================================================
     * GET RESOURCE USAGE
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<SharedResourceUsageResponse>
    getResourceUsage(
            UUID resourceId
    ) {

        resourceRepository.findById(
                resourceId
        ).orElseThrow(() ->
                new RuntimeException(
                        "Shared resource not found"
                )
        );


        return usageRepository
                .findByResourceIdOrderByStartTimeDesc(
                        resourceId
                )
                .stream()
                .map(
                        this::toUsageResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * GET FARM USAGE
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<SharedResourceUsageResponse>
    getFarmUsage(
            UUID farmId
    ) {

        farmRepository.findById(
                farmId
        ).orElseThrow(() ->
                new RuntimeException(
                        "Farm not found with ID: "
                                + farmId
                )
        );


        return usageRepository
                .findByFarmIdOrderByStartTimeDesc(
                        farmId
                )
                .stream()
                .map(
                        this::toUsageResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * GET FARMER USAGE
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<SharedResourceUsageResponse>
    getFarmerUsage(
            UUID farmerId
    ) {

        return usageRepository
                .findByFarmerIdOrderByStartTimeDesc(
                        farmerId
                )
                .stream()
                .map(
                        this::toUsageResponse
                )
                .toList();
    }


    /*
     * =========================================================
     * COOPERATIVE SUMMARY
     * =========================================================
     */

    @Transactional(readOnly = true)
    public CooperativeSummaryResponse
    getSummary(
            UUID cooperativeId
    ) {

        VillageCooperative cooperative =
                getCooperativeEntity(
                        cooperativeId
                );


        List<VillageCooperativeMember> members =
                memberRepository
                        .findByCooperativeId(
                                cooperativeId
                        );


        List<SharedFarmResource> resources =
                resourceRepository
                        .findByCooperativeId(
                                cooperativeId
                        );


        double totalUsageCost =
                0.0;

        double totalMemberShare =
                0.0;

        int completedUsageRecords = 0;


        /*
         * Collect usage through the resources.
         *
         * This avoids requiring a repository query
         * that joins cooperative -> resource -> usage.
         */
        for (
                SharedFarmResource resource
                : resources
        ) {

            List<SharedResourceUsage> usages =
                    usageRepository
                            .findByResourceIdOrderByStartTimeDesc(
                                    resource.getId()
                            );


            for (
                    SharedResourceUsage usage
                    : usages
            ) {

                if (
                        usage.getUsageCostInr()
                                != null
                ) {

                    totalUsageCost +=
                            usage
                                    .getUsageCostInr();
                }


                if (
                        usage.getMemberShareInr()
                                != null
                ) {

                    totalMemberShare +=
                            usage
                                    .getMemberShareInr();
                }


                if (
                        usage.getStatus() != null
                                && usage
                                .getStatus()
                                .equalsIgnoreCase(
                                        "COMPLETED"
                                )
                ) {

                    completedUsageRecords++;
                }
            }
        }


        return CooperativeSummaryResponse
                .builder()

                .cooperativeId(
                        cooperative.getId()
                )

                .cooperativeName(
                        cooperative.getName()
                )

                .village(
                        cooperative.getVillage()
                )

                .district(
                        cooperative.getDistrict()
                )

                .state(
                        cooperative.getState()
                )

                .status(
                        cooperative.getStatus()
                )

                .activeMemberCount(
                        (int)
                                members
                                        .stream()
                                        .filter(
                                                member ->
                                                        "ACTIVE"
                                                                .equalsIgnoreCase(
                                                                        member.getStatus()
                                                                )
                                        )
                                        .count()
                )

                .totalMemberCount(
                        members.size()
                )

                .resourceCount(
                        resources.size()
                )

                .availableResourceCount(
                        (int)
                                resources
                                        .stream()
                                        .filter(
                                                resource ->
                                                        "AVAILABLE"
                                                                .equalsIgnoreCase(
                                                                        resource.getStatus()
                                                                )
                                        )
                                        .count()
                )

                .totalUsageRecords(
                        resources
                                .stream()
                                .mapToInt(
                                        resource ->
                                                usageRepository
                                                        .findByResourceIdOrderByStartTimeDesc(
                                                                resource.getId()
                                                        )
                                                        .size()
                                )
                                .sum()
                )

                .completedUsageRecords(
                        completedUsageRecords
                )

                .totalUsageCostInr(
                        round(
                                totalUsageCost
                        )
                )

                .totalMemberShareInr(
                        round(
                                totalMemberShare
                        )
                )

                .dataStatus(
                        cooperative.getDataStatus()
                )

                .source(
                        cooperative.getSource()
                )

                .uncertaintyNote(
                        "Cooperative cost figures are based on "
                                + "recorded resource usage and supplied "
                                + "planning rates. They are not guaranteed "
                                + "savings calculations. Actual costs depend "
                                + "on local rental rates, fuel, maintenance, "
                                + "labor, usage duration and resource "
                                + "availability."
                )

                .build();
    }


    /*
     * =========================================================
     * DELETE / DEACTIVATE COOPERATIVE
     * =========================================================
     */

    public void deactivateCooperative(
            UUID cooperativeId
    ) {

        VillageCooperative cooperative =
                getCooperativeEntity(
                        cooperativeId
                );


        cooperative.setStatus(
                "INACTIVE"
        );


        cooperativeRepository.save(
                cooperative
        );
    }


    /*
     * =========================================================
     * INTERNAL MEMBER CREATION
     * =========================================================
     */

    private VillageCooperativeMember
    addMemberInternal(
            VillageCooperative cooperative,
            UUID farmerId,
            Farm farm,
            String role,
            String status,
            String notes
    ) {

        if (
                memberRepository
                        .existsByCooperativeIdAndFarmerId(
                                cooperative.getId(),
                                farmerId
                        )
        ) {

            return memberRepository
                    .findByCooperativeId(
                            cooperative.getId()
                    )
                    .stream()
                    .filter(
                            member ->
                                    farmerId.equals(
                                            member.getFarmerId()
                                    )
                    )
                    .findFirst()
                    .orElseThrow();
        }


        VillageCooperativeMember member =
                VillageCooperativeMember.builder()

                        .cooperative(
                                cooperative
                        )

                        .farmerId(
                                farmerId
                        )

                        .farm(
                                farm
                        )

                        .role(
                                role
                        )

                        .status(
                                status
                        )

                        .notes(
                                notes
                        )

                        .build();


        return memberRepository.save(
                member
        );
    }


    /*
     * =========================================================
     * VALIDATIONS
     * =========================================================
     */

    private void validateCooperativeRequest(
            VillageCooperativeRequest request
    ) {

        if (request == null) {

            throw new RuntimeException(
                    "Cooperative request is required"
            );
        }


        if (
                request.getName() == null
                        || request.getName().isBlank()
        ) {

            throw new RuntimeException(
                    "Cooperative name is required"
            );
        }


        if (
                request.getCoordinatorFarmerId()
                        == null
        ) {

            throw new RuntimeException(
                    "Coordinator farmer ID is required"
            );
        }
    }


    private void validateResourceRequest(
            SharedFarmResourceRequest request
    ) {

        if (request == null) {

            throw new RuntimeException(
                    "Resource request is required"
            );
        }


        if (request.getCooperativeId() == null) {

            throw new RuntimeException(
                    "Cooperative ID is required"
            );
        }


        if (
                request.getResourceType() == null
                        || request.getResourceType().isBlank()
        ) {

            throw new RuntimeException(
                    "Resource type is required"
            );
        }


        if (
                request.getResourceName() == null
                        || request.getResourceName().isBlank()
        ) {

            throw new RuntimeException(
                    "Resource name is required"
            );
        }


        validateNonNegative(
                request.getEstimatedTotalCostInr(),
                "Estimated total cost"
        );


        validateNonNegative(
                request.getEstimatedUsageCostInr(),
                "Estimated usage cost"
        );
    }


    private void validateUsageRequest(
            SharedResourceUsageRequest request
    ) {

        if (request == null) {

            throw new RuntimeException(
                    "Usage request is required"
            );
        }


        if (request.getResourceId() == null) {

            throw new RuntimeException(
                    "Resource ID is required"
            );
        }


        if (request.getFarmerId() == null) {

            throw new RuntimeException(
                    "Farmer ID is required"
            );
        }


        if (request.getStartTime() == null) {

            throw new RuntimeException(
                    "Start time is required"
            );
        }


        validateNonNegative(
                request.getQuantityUsed(),
                "Quantity used"
        );


        validateNonNegative(
                request.getUsageCostInr(),
                "Usage cost"
        );


        validateNonNegative(
                request.getMemberShareInr(),
                "Member share"
        );
    }


    private void validateNonNegative(
            Double value,
            String fieldName
    ) {

        if (
                value != null
                        && value < 0
        ) {

            throw new RuntimeException(
                    fieldName
                            + " cannot be negative"
            );
        }
    }


    /*
     * =========================================================
     * FINDERS
     * =========================================================
     */

    private VillageCooperative
    getCooperativeEntity(
            UUID cooperativeId
    ) {

        return cooperativeRepository
                .findById(
                        cooperativeId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Village cooperative not found "
                                        + "with ID: "
                                        + cooperativeId
                        )
                );
    }


    private boolean isCooperativeMember(
            UUID cooperativeId,
            UUID farmerId
    ) {

        return memberRepository
                .existsByCooperativeIdAndFarmerId(
                        cooperativeId,
                        farmerId
                );
    }


    /*
     * =========================================================
     * DTO MAPPERS
     * =========================================================
     */

    private VillageCooperativeResponse
    toCooperativeResponse(
            VillageCooperative cooperative
    ) {

        int memberCount =
                memberRepository
                        .findByCooperativeId(
                                cooperative.getId()
                        )
                        .size();


        int resourceCount =
                resourceRepository
                        .findByCooperativeId(
                                cooperative.getId()
                        )
                        .size();


        return VillageCooperativeResponse
                .builder()

                .id(
                        cooperative.getId()
                )

                .name(
                        cooperative.getName()
                )

                .village(
                        cooperative.getVillage()
                )

                .district(
                        cooperative.getDistrict()
                )

                .state(
                        cooperative.getState()
                )

                .contactInfo(
                        cooperative.getContactInfo()
                )

                .coordinatorFarmerId(
                        cooperative
                                .getCoordinatorFarmerId()
                )

                .status(
                        cooperative.getStatus()
                )

                .dataStatus(
                        cooperative.getDataStatus()
                )

                .source(
                        cooperative.getSource()
                )

                .notes(
                        cooperative.getNotes()
                )

                .createdAt(
                        cooperative.getCreatedAt()
                )

                .memberCount(
                        memberCount
                )

                .resourceCount(
                        resourceCount
                )

                .build();
    }


    private CooperativeMemberResponse
    toMemberResponse(
            VillageCooperativeMember member
    ) {

        return CooperativeMemberResponse
                .builder()

                .id(
                        member.getId()
                )

                .cooperativeId(
                        member
                                .getCooperative()
                                .getId()
                )

                .farmerId(
                        member.getFarmerId()
                )

                .farmId(
                        member.getFarm() != null
                                ? member.getFarm().getId()
                                : null
                )

                .farmName(
                        member.getFarm() != null
                                ? member.getFarm().getName()
                                : null
                )

                .role(
                        member.getRole()
                )

                .status(
                        member.getStatus()
                )

                .joinedAt(
                        member.getJoinedAt()
                )

                .notes(
                        member.getNotes()
                )

                .build();
    }


    private SharedFarmResourceResponse
    toResourceResponse(
            SharedFarmResource resource
    ) {

        return SharedFarmResourceResponse
                .builder()

                .id(
                        resource.getId()
                )

                .cooperativeId(
                        resource
                                .getCooperative()
                                .getId()
                )

                .resourceType(
                        resource.getResourceType()
                )

                .resourceName(
                        resource.getResourceName()
                )

                .ownerFarmerId(
                        resource.getOwnerFarmerId()
                )

                .location(
                        resource.getLocation()
                )

                .status(
                        resource.getStatus()
                )

                .estimatedTotalCostInr(
                        resource
                                .getEstimatedTotalCostInr()
                )

                .estimatedUsageCostInr(
                        resource
                                .getEstimatedUsageCostInr()
                )

                .usageUnit(
                        resource.getUsageUnit()
                )

                .description(
                        resource.getDescription()
                )

                .dataStatus(
                        resource.getDataStatus()
                )

                .source(
                        resource.getSource()
                )

                .notes(
                        resource.getNotes()
                )

                .createdAt(
                        resource.getCreatedAt()
                )

                .build();
    }


    private SharedResourceUsageResponse
    toUsageResponse(
            SharedResourceUsage usage
    ) {

        SharedFarmResource resource =
                usage.getResource();


        return SharedResourceUsageResponse
                .builder()

                .id(
                        usage.getId()
                )

                .resourceId(
                        resource.getId()
                )

                .resourceName(
                        resource.getResourceName()
                )

                .resourceType(
                        resource.getResourceType()
                )

                .cooperativeId(
                        resource
                                .getCooperative()
                                .getId()
                )

                .farmerId(
                        usage.getFarmerId()
                )

                .farmId(
                        usage.getFarm() != null
                                ? usage
                                .getFarm()
                                .getId()
                                : null
                )

                .farmName(
                        usage.getFarm() != null
                                ? usage
                                .getFarm()
                                .getName()
                                : null
                )

                .startTime(
                        usage.getStartTime()
                )

                .endTime(
                        usage.getEndTime()
                )

                .quantityUsed(
                        usage.getQuantityUsed()
                )

                .usageCostInr(
                        usage.getUsageCostInr()
                )

                .memberShareInr(
                        usage.getMemberShareInr()
                )

                .status(
                        usage.getStatus()
                )

                .purpose(
                        usage.getPurpose()
                )

                .dataStatus(
                        usage.getDataStatus()
                )

                .source(
                        usage.getSource()
                )

                .notes(
                        usage.getNotes()
                )

                .build();
    }


    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private String normalizeRole(
            String role
    ) {

        if (
                role == null
                        || role.isBlank()
        ) {

            return "MEMBER";
        }


        return role
                .trim()
                .toUpperCase();
    }


    private String normalizeStatus(
            String status,
            String defaultStatus
    ) {

        if (
                status == null
                        || status.isBlank()
        ) {

            return defaultStatus;
        }


        return status
                .trim()
                .toUpperCase();
    }


    private double round(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}