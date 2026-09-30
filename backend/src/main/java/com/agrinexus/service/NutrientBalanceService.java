package com.agrinexus.service;

import com.agrinexus.dto.NutrientBalanceRequest;
import com.agrinexus.dto.NutrientBalanceResponse;
import com.agrinexus.entity.Crop;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.FertilizerInput;
import com.agrinexus.entity.NutrientBalance;
import com.agrinexus.repository.CropRepository;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.FertilizerInputRepository;
import com.agrinexus.repository.NutrientBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NutrientBalanceService {

    /*
     * =============================================================
     * AGRINEXUS NUTRIENT BALANCE TRACKING
     * =============================================================
     *
     * Methodology:
     *
     * AGRINEXUS-NUTRIENT-BALANCE-V2
     *
     * Complete accounting formula:
     *
     * Balance =
     *
     * Inputs
     * + Residue Return
     * + Biological Nitrogen Fixation
     * - Crop Uptake
     *
     * Important:
     *
     * - Missing request numbers are treated as zero ONLY when
     *   calculating an explicitly supplied accounting record.
     *
     * - Provenance is preserved separately.
     *
     * - A calculated balance is never labelled MEASURED.
     *
     * - Explicit TEST source is labelled TEST.
     *
     * - Explicit CALCULATED / ESTIMATED / MIXED status is preserved.
     *
     * - No nutrient deficiency diagnosis is generated here.
     *
     * - Fertilizer-input aggregation uses the repository directly
     *   to avoid a circular dependency with FertilizerInputService.
     *
     * - Fertilizer-input aggregation does NOT call calculateBalances()
     *   when required accounting components are unavailable.
     *
     * - No fake nutrient values are generated.
     * =============================================================
     */

    private static final String METHODOLOGY_VERSION =
            "AGRINEXUS-NUTRIENT-BALANCE-V2";

    private final NutrientBalanceRepository nutrientBalanceRepository;

    private final FarmRepository farmRepository;

    private final CropRepository cropRepository;

    /*
     * Direct repository access is intentional.
     *
     * Do NOT replace this with FertilizerInputService.
     * Doing so creates:
     *
     * NutrientBalanceService
     *        -> FertilizerInputService
     *        -> NutrientBalanceService
     *
     * which creates a Spring bean cycle.
     */
    private final FertilizerInputRepository fertilizerInputRepository;


    /*
     * =============================================================
     * CREATE NUTRIENT BALANCE
     * =============================================================
     */
    @Transactional
    public NutrientBalanceResponse createNutrientBalance(
            NutrientBalanceRequest request
    ) {

        validateRequest(
                request
        );

        Farm farm =
                farmRepository.findById(
                                request.getFarmId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + request.getFarmId()
                                )
                        );

        Crop crop = null;

        if (
                request.getCropId() != null
        ) {

            crop =
                    cropRepository.findById(
                                    request.getCropId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Crop not found with ID: "
                                                    + request.getCropId()
                                    )
                            );
        }


        /*
         * ---------------------------------------------------------
         * Determine provenance BEFORE saving.
         * ---------------------------------------------------------
         */

        String finalDataStatus =
                determineDataStatus(
                        request
                );

        String finalSource =
                determineSource(
                        request
                );


        /*
         * ---------------------------------------------------------
         * Build balance entity.
         *
         * safeValue() is used for arithmetic/storage compatibility.
         * Provenance tells us that zero may mean that the value was
         * not supplied.
         * ---------------------------------------------------------
         */

        NutrientBalance balance =
                NutrientBalance.builder()

                        .farm(
                                farm
                        )

                        .crop(
                                crop
                        )

                        .periodStart(
                                request.getPeriodStart()
                        )

                        .periodEnd(
                                request.getPeriodEnd()
                        )

                        .nitrogenInput(
                                safeValue(
                                        request.getNitrogenInput()
                                )
                        )

                        .phosphorusInput(
                                safeValue(
                                        request.getPhosphorusInput()
                                )
                        )

                        .potassiumInput(
                                safeValue(
                                        request.getPotassiumInput()
                                )
                        )

                        .sulphurInput(
                                safeValue(
                                        request.getSulphurInput()
                                )
                        )

                        .zincInput(
                                safeValue(
                                        request.getZincInput()
                                )
                        )

                        .nitrogenUptake(
                                safeValue(
                                        request.getNitrogenUptake()
                                )
                        )

                        .phosphorusUptake(
                                safeValue(
                                        request.getPhosphorusUptake()
                                )
                        )

                        .potassiumUptake(
                                safeValue(
                                        request.getPotassiumUptake()
                                )
                        )

                        .sulphurUptake(
                                safeValue(
                                        request.getSulphurUptake()
                                )
                        )

                        .zincUptake(
                                safeValue(
                                        request.getZincUptake()
                                )
                        )

                        .nitrogenResidueReturn(
                                safeValue(
                                        request
                                                .getNitrogenResidueReturn()
                                )
                        )

                        .phosphorusResidueReturn(
                                safeValue(
                                        request
                                                .getPhosphorusResidueReturn()
                                )
                        )

                        .potassiumResidueReturn(
                                safeValue(
                                        request
                                                .getPotassiumResidueReturn()
                                )
                        )

                        .sulphurResidueReturn(
                                safeValue(
                                        request
                                                .getSulphurResidueReturn()
                                )
                        )

                        .zincResidueReturn(
                                safeValue(
                                        request
                                                .getZincResidueReturn()
                                )
                        )

                        .nitrogenBiologicalFixation(
                                safeValue(
                                        request
                                                .getNitrogenBiologicalFixation()
                                )
                        )

                        .dataStatus(
                                finalDataStatus
                        )

                        .source(
                                finalSource
                        )

                        .notes(
                                request.getNotes()
                        )

                        .build();


        /*
         * ---------------------------------------------------------
         * Calculate complete accounting balance.
         *
         * This workflow assumes the caller intentionally supplied
         * the accounting components.
         * ---------------------------------------------------------
         */

        calculateBalances(
                balance
        );


        NutrientBalance savedBalance =
                nutrientBalanceRepository.save(
                        balance
                );


        return toResponse(
                savedBalance
        );
    }


    /*
     * =============================================================
     * GENERATE PARTIAL BALANCE FROM REAL FERTILIZER INPUTS
     * =============================================================
     *
     * This method uses ONLY nutrient amounts that are already
     * stored in FertilizerInput records.
     *
     * It does NOT infer:
     *
     * - crop nutrient uptake
     * - residue nutrient return
     * - biological nitrogen fixation
     *
     * Therefore:
     *
     * - the resulting record is INSUFFICIENT_DATA
     * - net balances remain NULL
     *
     * No fabricated accounting result is produced.
     *
     * IMPORTANT:
     *
     * FertilizerInputRepository is used directly here.
     * This prevents a circular Spring dependency.
     *
     * =============================================================
     */
    @Transactional
    public NutrientBalanceResponse
    generateFromFertilizerInputs(
            UUID farmId,
            UUID cropId
    ) {

        verifyFarmExists(
                farmId
        );


        /*
         * ---------------------------------------------------------
         * Retrieve REAL fertilizer input records.
         * ---------------------------------------------------------
         */

        List<FertilizerInput> inputs;

        if (
                cropId != null
        ) {

            verifyCropExists(
                    cropId
            );

            inputs =
                    fertilizerInputRepository
                            .findByFarmIdAndCropIdOrderByApplicationDateDesc(
                                    farmId,
                                    cropId
                            );

        } else {

            inputs =
                    fertilizerInputRepository
                            .findByFarmIdOrderByApplicationDateDesc(
                                    farmId
                            );
        }


        /*
         * ---------------------------------------------------------
         * No real records.
         *
         * Do not create an empty fake balance.
         * ---------------------------------------------------------
         */

        if (
                inputs == null
                        || inputs.isEmpty()
        ) {

            return null;
        }


        /*
         * ---------------------------------------------------------
         * Aggregate actual nutrient amounts.
         * ---------------------------------------------------------
         */

        double nitrogenInput = 0.0;

        double phosphorusInput = 0.0;

        double potassiumInput = 0.0;

        double sulphurInput = 0.0;

        double zincInput = 0.0;


        boolean nitrogenAvailable = false;

        boolean phosphorusAvailable = false;

        boolean potassiumAvailable = false;

        boolean sulphurAvailable = false;

        boolean zincAvailable = false;


        LocalDate earliestDate = null;

        LocalDate latestDate = null;


        /*
         * ---------------------------------------------------------
         * Read each real fertilizer input.
         * ---------------------------------------------------------
         */

        for (
                FertilizerInput input :
                inputs
        ) {

            if (
                    input == null
            ) {

                continue;
            }


            /*
             * -----------------------------------------------------
             * Determine accounting period from real application
             * dates.
             * -----------------------------------------------------
             */

            LocalDate applicationDate =
                    input.getApplicationDate();

            if (
                    applicationDate != null
            ) {

                if (
                        earliestDate == null
                                || applicationDate.isBefore(
                                earliestDate
                        )
                ) {

                    earliestDate =
                            applicationDate;
                }


                if (
                        latestDate == null
                                || applicationDate.isAfter(
                                latestDate
                        )
                ) {

                    latestDate =
                            applicationDate;
                }
            }


            /*
             * -----------------------------------------------------
             * Nitrogen
             * -----------------------------------------------------
             */

            if (
                    input.getNitrogen() != null
            ) {

                validateFiniteNonNegative(
                        input.getNitrogen(),
                        "Nitrogen input"
                );

                nitrogenInput +=
                        input.getNitrogen();

                nitrogenAvailable =
                        true;
            }


            /*
             * -----------------------------------------------------
             * Phosphorus
             * -----------------------------------------------------
             */

            if (
                    input.getPhosphorus() != null
            ) {

                validateFiniteNonNegative(
                        input.getPhosphorus(),
                        "Phosphorus input"
                );

                phosphorusInput +=
                        input.getPhosphorus();

                phosphorusAvailable =
                        true;
            }


            /*
             * -----------------------------------------------------
             * Potassium
             * -----------------------------------------------------
             */

            if (
                    input.getPotassium() != null
            ) {

                validateFiniteNonNegative(
                        input.getPotassium(),
                        "Potassium input"
                );

                potassiumInput +=
                        input.getPotassium();

                potassiumAvailable =
                        true;
            }


            /*
             * -----------------------------------------------------
             * Sulphur
             * -----------------------------------------------------
             */

            if (
                    input.getSulphur() != null
            ) {

                validateFiniteNonNegative(
                        input.getSulphur(),
                        "Sulphur input"
                );

                sulphurInput +=
                        input.getSulphur();

                sulphurAvailable =
                        true;
            }


            /*
             * -----------------------------------------------------
             * Zinc
             * -----------------------------------------------------
             */

            if (
                    input.getZinc() != null
            ) {

                validateFiniteNonNegative(
                        input.getZinc(),
                        "Zinc input"
                );

                zincInput +=
                        input.getZinc();

                zincAvailable =
                        true;
            }
        }


        /*
         * ---------------------------------------------------------
         * A real accounting period requires at least one real
         * fertilizer application date.
         * ---------------------------------------------------------
         */

        if (
                earliestDate == null
                        || latestDate == null
        ) {

            return null;
        }


        /*
         * ---------------------------------------------------------
         * Preserve missing nutrient values as NULL.
         *
         * NULL means unavailable.
         *
         * We do NOT convert a missing nutrient into a measured zero.
         * ---------------------------------------------------------
         */

        Double nitrogen =
                nitrogenAvailable
                        ? nitrogenInput
                        : null;

        Double phosphorus =
                phosphorusAvailable
                        ? phosphorusInput
                        : null;

        Double potassium =
                potassiumAvailable
                        ? potassiumInput
                        : null;

        Double sulphur =
                sulphurAvailable
                        ? sulphurInput
                        : null;

        Double zinc =
                zincAvailable
                        ? zincInput
                        : null;


        /*
         * ---------------------------------------------------------
         * Resolve real Farm entity.
         * ---------------------------------------------------------
         */

        Farm farm =
                farmRepository.findById(
                                farmId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + farmId
                                )
                        );


        /*
         * ---------------------------------------------------------
         * Resolve optional Crop entity.
         * ---------------------------------------------------------
         */

        Crop crop = null;

        if (
                cropId != null
        ) {

            crop =
                    cropRepository.findById(
                                    cropId
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Crop not found with ID: "
                                                    + cropId
                                    )
                            );
        }


        /*
         * ---------------------------------------------------------
         * Build a PARTIAL nutrient accounting record.
         *
         * Uptake:
         *     NULL
         *
         * Residue return:
         *     NULL
         *
         * Biological fixation:
         *     NULL
         *
         * Net balance:
         *     NULL
         *
         * We deliberately DO NOT call calculateBalances().
         * ---------------------------------------------------------
         */

        NutrientBalance balance =
                NutrientBalance.builder()

                        .farm(
                                farm
                        )

                        .crop(
                                crop
                        )

                        .periodStart(
                                earliestDate
                        )

                        .periodEnd(
                                latestDate
                        )

                        .nitrogenInput(
                                nitrogen
                        )

                        .phosphorusInput(
                                phosphorus
                        )

                        .potassiumInput(
                                potassium
                        )

                        .sulphurInput(
                                sulphur
                        )

                        .zincInput(
                                zinc
                        )

                        /*
                         * -------------------------------------------------
                         * Uptake unavailable.
                         * -------------------------------------------------
                         */

                        .nitrogenUptake(
                                null
                        )

                        .phosphorusUptake(
                                null
                        )

                        .potassiumUptake(
                                null
                        )

                        .sulphurUptake(
                                null
                        )

                        .zincUptake(
                                null
                        )

                        /*
                         * -------------------------------------------------
                         * Residue return unavailable.
                         * -------------------------------------------------
                         */

                        .nitrogenResidueReturn(
                                null
                        )

                        .phosphorusResidueReturn(
                                null
                        )

                        .potassiumResidueReturn(
                                null
                        )

                        .sulphurResidueReturn(
                                null
                        )

                        .zincResidueReturn(
                                null
                        )

                        /*
                         * -------------------------------------------------
                         * Biological fixation unavailable.
                         * -------------------------------------------------
                         */

                        .nitrogenBiologicalFixation(
                                null
                        )

                        /*
                         * -------------------------------------------------
                         * Provenance.
                         * -------------------------------------------------
                         */

                        .dataStatus(
                                "INSUFFICIENT_DATA"
                        )

                        .source(
                                "FERTILIZER_INPUT_AGGREGATION"
                        )

                        .notes(
                                buildInputAggregationNotes(
                                        inputs
                                )
                        )

                        .build();


        /*
         * ---------------------------------------------------------
         * Save the partial accounting record.
         * ---------------------------------------------------------
         */

        NutrientBalance savedBalance =
                nutrientBalanceRepository.save(
                        balance
                );


        return toResponse(
                savedBalance
        );
    }


    /*
     * =============================================================
     * GET BY ID
     * =============================================================
     */
    public NutrientBalanceResponse
    getNutrientBalanceById(
            UUID balanceId
    ) {

        NutrientBalance balance =
                nutrientBalanceRepository.findById(
                                balanceId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Nutrient balance not found with ID: "
                                                + balanceId
                                )
                        );

        return toResponse(
                balance
        );
    }


    /*
     * =============================================================
     * GET ALL FOR FARM
     * =============================================================
     */
    public List<NutrientBalanceResponse>
    getFarmNutrientBalances(
            UUID farmId
    ) {

        verifyFarmExists(
                farmId
        );

        return nutrientBalanceRepository
                .findByFarmIdOrderByPeriodStartDesc(
                        farmId
                )
                .stream()
                .map(
                        this::toResponse
                )
                .toList();
    }


    /*
     * =============================================================
     * GET FOR CROP
     * =============================================================
     */
    public List<NutrientBalanceResponse>
    getCropNutrientBalances(
            UUID farmId,
            UUID cropId
    ) {

        verifyFarmExists(
                farmId
        );

        verifyCropExists(
                cropId
        );

        return nutrientBalanceRepository
                .findByFarmIdAndCropIdOrderByPeriodStartDesc(
                        farmId,
                        cropId
                )
                .stream()
                .map(
                        this::toResponse
                )
                .toList();
    }


    /*
     * =============================================================
     * GET LATEST FARM BALANCE
     * =============================================================
     */
    public NutrientBalanceResponse
    getLatestFarmBalance(
            UUID farmId
    ) {

        verifyFarmExists(
                farmId
        );

        NutrientBalance balance =
                nutrientBalanceRepository
                        .findFirstByFarmIdOrderByPeriodEndDesc(
                                farmId
                        );

        if (
                balance == null
        ) {

            return null;
        }

        return toResponse(
                balance
        );
    }


    /*
     * =============================================================
     * GET LATEST CROP BALANCE
     * =============================================================
     */
    public NutrientBalanceResponse
    getLatestCropBalance(
            UUID farmId,
            UUID cropId
    ) {

        verifyFarmExists(
                farmId
        );

        verifyCropExists(
                cropId
        );

        NutrientBalance balance =
                nutrientBalanceRepository
                        .findFirstByFarmIdAndCropIdOrderByPeriodEndDesc(
                                farmId,
                                cropId
                        );

        if (
                balance == null
        ) {

            return null;
        }

        return toResponse(
                balance
        );
    }


    /*
     * =============================================================
     * DATE RANGE
     * =============================================================
     */
    public List<NutrientBalanceResponse>
    getBalancesByDateRange(
            UUID farmId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        verifyFarmExists(
                farmId
        );

        if (
                startDate == null
                        || endDate == null
        ) {

            throw new RuntimeException(
                    "Start date and end date are required"
            );
        }

        if (
                startDate.isAfter(
                        endDate
                )
        ) {

            throw new RuntimeException(
                    "Start date cannot be after end date"
            );
        }

        return nutrientBalanceRepository
                .findByFarmIdAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualOrderByPeriodStartDesc(
                        farmId,
                        endDate,
                        startDate
                )
                .stream()
                .map(
                        this::toResponse
                )
                .toList();
    }


    /*
     * =============================================================
     * UPDATE
     * =============================================================
     */
    @Transactional
    public NutrientBalanceResponse
    updateNutrientBalance(
            UUID balanceId,
            NutrientBalanceRequest request
    ) {

        validateRequest(
                request
        );


        NutrientBalance balance =
                nutrientBalanceRepository.findById(
                                balanceId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Nutrient balance not found with ID: "
                                                + balanceId
                                )
                        );


        Farm farm =
                farmRepository.findById(
                                request.getFarmId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + request.getFarmId()
                                )
                        );


        Crop crop = null;

        if (
                request.getCropId() != null
        ) {

            crop =
                    cropRepository.findById(
                                    request.getCropId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Crop not found with ID: "
                                                    + request.getCropId()
                                    )
                            );
        }


        /*
         * ---------------------------------------------------------
         * Recalculate provenance on update.
         * ---------------------------------------------------------
         */

        String finalDataStatus =
                determineDataStatus(
                        request
                );

        String finalSource =
                determineSource(
                        request
                );


        balance.setFarm(
                farm
        );

        balance.setCrop(
                crop
        );

        balance.setPeriodStart(
                request.getPeriodStart()
        );

        balance.setPeriodEnd(
                request.getPeriodEnd()
        );


        balance.setNitrogenInput(
                safeValue(
                        request.getNitrogenInput()
                )
        );

        balance.setPhosphorusInput(
                safeValue(
                        request.getPhosphorusInput()
                )
        );

        balance.setPotassiumInput(
                safeValue(
                        request.getPotassiumInput()
                )
        );

        balance.setSulphurInput(
                safeValue(
                        request.getSulphurInput()
                )
        );

        balance.setZincInput(
                safeValue(
                        request.getZincInput()
                )
        );


        balance.setNitrogenUptake(
                safeValue(
                        request.getNitrogenUptake()
                )
        );

        balance.setPhosphorusUptake(
                safeValue(
                        request.getPhosphorusUptake()
                )
        );

        balance.setPotassiumUptake(
                safeValue(
                        request.getPotassiumUptake()
                )
        );

        balance.setSulphurUptake(
                safeValue(
                        request.getSulphurUptake()
                )
        );

        balance.setZincUptake(
                safeValue(
                        request.getZincUptake()
                )
        );


        balance.setNitrogenResidueReturn(
                safeValue(
                        request.getNitrogenResidueReturn()
                )
        );

        balance.setPhosphorusResidueReturn(
                safeValue(
                        request.getPhosphorusResidueReturn()
                )
        );

        balance.setPotassiumResidueReturn(
                safeValue(
                        request.getPotassiumResidueReturn()
                )
        );

        balance.setSulphurResidueReturn(
                safeValue(
                        request.getSulphurResidueReturn()
                )
        );

        balance.setZincResidueReturn(
                safeValue(
                        request.getZincResidueReturn()
                )
        );


        balance.setNitrogenBiologicalFixation(
                safeValue(
                        request.getNitrogenBiologicalFixation()
                )
        );


        balance.setDataStatus(
                finalDataStatus
        );

        balance.setSource(
                finalSource
        );

        balance.setNotes(
                request.getNotes()
        );


        /*
         * ---------------------------------------------------------
         * Recalculate after update.
         * ---------------------------------------------------------
         */

        calculateBalances(
                balance
        );


        NutrientBalance updatedBalance =
                nutrientBalanceRepository.save(
                        balance
                );


        return toResponse(
                updatedBalance
        );
    }


    /*
     * =============================================================
     * DELETE
     * =============================================================
     */
    public void deleteNutrientBalance(
            UUID balanceId
    ) {

        if (
                !nutrientBalanceRepository
                        .existsById(
                                balanceId
                        )
        ) {

            throw new RuntimeException(
                    "Nutrient balance not found with ID: "
                            + balanceId
            );
        }

        nutrientBalanceRepository.deleteById(
                balanceId
        );
    }


    /*
     * =============================================================
     * CALCULATE BALANCES
     * =============================================================
     *
     * Complete accounting formula:
     *
     * N =
     * input
     * + residue return
     * + biological fixation
     * - uptake
     *
     * P =
     * input
     * + residue return
     * - uptake
     *
     * K =
     * input
     * + residue return
     * - uptake
     *
     * S =
     * input
     * + residue return
     * - uptake
     *
     * Zn =
     * input
     * + residue return
     * - uptake
     *
     * This method is used ONLY for an explicitly supplied
     * accounting request, not for incomplete fertilizer-input
     * aggregation.
     *
     * =============================================================
     */
    private void calculateBalances(
            NutrientBalance balance
    ) {

        double nitrogenBalance =
                safeValue(
                        balance.getNitrogenInput()
                )
                        +
                        safeValue(
                                balance
                                        .getNitrogenResidueReturn()
                        )
                        +
                        safeValue(
                                balance
                                        .getNitrogenBiologicalFixation()
                        )
                        -
                        safeValue(
                                balance
                                        .getNitrogenUptake()
                        );


        double phosphorusBalance =
                safeValue(
                        balance.getPhosphorusInput()
                )
                        +
                        safeValue(
                                balance
                                        .getPhosphorusResidueReturn()
                        )
                        -
                        safeValue(
                                balance
                                        .getPhosphorusUptake()
                        );


        double potassiumBalance =
                safeValue(
                        balance.getPotassiumInput()
                )
                        +
                        safeValue(
                                balance
                                        .getPotassiumResidueReturn()
                        )
                        -
                        safeValue(
                                balance
                                        .getPotassiumUptake()
                        );


        double sulphurBalance =
                safeValue(
                        balance.getSulphurInput()
                )
                        +
                        safeValue(
                                balance
                                        .getSulphurResidueReturn()
                        )
                        -
                        safeValue(
                                balance
                                        .getSulphurUptake()
                        );


        double zincBalance =
                safeValue(
                        balance.getZincInput()
                )
                        +
                        safeValue(
                                balance
                                        .getZincResidueReturn()
                        )
                        -
                        safeValue(
                                balance
                                        .getZincUptake()
                        );


        balance.setNitrogenBalance(
                nitrogenBalance
        );

        balance.setPhosphorusBalance(
                phosphorusBalance
        );

        balance.setPotassiumBalance(
                potassiumBalance
        );

        balance.setSulphurBalance(
                sulphurBalance
        );

        balance.setZincBalance(
                zincBalance
        );
    }


    /*
     * =============================================================
     * ENTITY -> RESPONSE
     * =============================================================
     */
    private NutrientBalanceResponse toResponse(
            NutrientBalance balance
    ) {

        return NutrientBalanceResponse.builder()

                .id(
                        balance.getId()
                )

                .farmId(
                        balance.getFarm() != null
                                ? balance.getFarm().getId()
                                : null
                )

                .cropId(
                        balance.getCrop() != null
                                ? balance.getCrop().getId()
                                : null
                )

                .cropName(
                        balance.getCrop() != null
                                ? balance.getCrop().getName()
                                : null
                )

                .periodStart(
                        balance.getPeriodStart()
                )

                .periodEnd(
                        balance.getPeriodEnd()
                )


                .nitrogenInput(
                        balance.getNitrogenInput()
                )

                .phosphorusInput(
                        balance.getPhosphorusInput()
                )

                .potassiumInput(
                        balance.getPotassiumInput()
                )

                .sulphurInput(
                        balance.getSulphurInput()
                )

                .zincInput(
                        balance.getZincInput()
                )


                .nitrogenUptake(
                        balance.getNitrogenUptake()
                )

                .phosphorusUptake(
                        balance.getPhosphorusUptake()
                )

                .potassiumUptake(
                        balance.getPotassiumUptake()
                )

                .sulphurUptake(
                        balance.getSulphurUptake()
                )

                .zincUptake(
                        balance.getZincUptake()
                )


                .nitrogenResidueReturn(
                        balance.getNitrogenResidueReturn()
                )

                .phosphorusResidueReturn(
                        balance.getPhosphorusResidueReturn()
                )

                .potassiumResidueReturn(
                        balance.getPotassiumResidueReturn()
                )

                .sulphurResidueReturn(
                        balance.getSulphurResidueReturn()
                )

                .zincResidueReturn(
                        balance.getZincResidueReturn()
                )


                .nitrogenBiologicalFixation(
                        balance
                                .getNitrogenBiologicalFixation()
                )


                .nitrogenBalance(
                        balance.getNitrogenBalance()
                )

                .phosphorusBalance(
                        balance.getPhosphorusBalance()
                )

                .potassiumBalance(
                        balance.getPotassiumBalance()
                )

                .sulphurBalance(
                        balance.getSulphurBalance()
                )

                .zincBalance(
                        balance.getZincBalance()
                )


                .nitrogenStatus(
                        getBalanceStatus(
                                balance.getNitrogenBalance()
                        )
                )

                .phosphorusStatus(
                        getBalanceStatus(
                                balance.getPhosphorusBalance()
                        )
                )

                .potassiumStatus(
                        getBalanceStatus(
                                balance.getPotassiumBalance()
                        )
                )

                .sulphurStatus(
                        getBalanceStatus(
                                balance.getSulphurBalance()
                        )
                )

                .zincStatus(
                        getBalanceStatus(
                                balance.getZincBalance()
                        )
                )


                .dataStatus(
                        balance.getDataStatus()
                )

                .source(
                        balance.getSource()
                )

                .notes(
                        balance.getNotes()
                )

                .build();
    }


    /*
     * =============================================================
     * BALANCE STATUS
     * =============================================================
     *
     * These are accounting labels only.
     *
     * They are NOT soil deficiency diagnoses.
     * =============================================================
     */
    private String getBalanceStatus(
            Double balance
    ) {

        if (
                balance == null
        ) {

            return "NOT_AVAILABLE";
        }

        if (
                balance > 0
        ) {

            return "POSITIVE";
        }

        if (
                balance < 0
        ) {

            return "NEGATIVE";
        }

        return "BALANCED";
    }


    /*
     * =============================================================
     * SAFE NUMERIC VALUE
     * =============================================================
     *
     * Used ONLY for complete accounting arithmetic.
     *
     * Provenance is handled independently.
     * =============================================================
     */
    private double safeValue(
            Double value
    ) {

        if (
                value == null
        ) {

            return 0.0;
        }

        if (
                value.isNaN()
                        || value.isInfinite()
        ) {

            throw new RuntimeException(
                    "Nutrient value must be a finite number"
            );
        }

        return value;
    }


    /*
     * =============================================================
     * DATA STATUS
     * =============================================================
     */
    private String determineDataStatus(
            NutrientBalanceRequest request
    ) {

        String requestedStatus =
                normalize(
                        request.getDataStatus()
                );

        String requestedSource =
                normalize(
                        request.getSource()
                );


        /*
         * ---------------------------------------------------------
         * Explicit test/synthetic source.
         * ---------------------------------------------------------
         */

        if (
                isTestSource(
                        requestedSource
                )
        ) {

            return "TEST";
        }


        if (
                "TEST".equals(
                        requestedStatus
                )
                        ||
                        "SYNTHETIC".equals(
                                requestedStatus
                        )
        ) {

            return "TEST";
        }


        /*
         * ---------------------------------------------------------
         * Explicit calculated/estimated/mixed status.
         * ---------------------------------------------------------
         */

        if (
                "CALCULATED".equals(
                        requestedStatus
                )
                        ||
                        "MIXED".equals(
                                requestedStatus
                        )
                        ||
                        "ESTIMATED".equals(
                                requestedStatus
                        )
        ) {

            return requestedStatus;
        }


        /*
         * ---------------------------------------------------------
         * A nutrient balance is an accounting calculation.
         * ---------------------------------------------------------
         */

        if (
                hasAnyNutrientInput(
                        request
                )
                        ||
                        hasAnyNutrientUptake(
                                request
                        )
                        ||
                        hasAnyResidueReturn(
                                request
                        )
                        ||
                        request.getNitrogenBiologicalFixation() != null
        ) {

            return "CALCULATED";
        }


        /*
         * ---------------------------------------------------------
         * Nothing useful supplied.
         * ---------------------------------------------------------
         */

        return "INSUFFICIENT_DATA";
    }


    /*
     * =============================================================
     * SOURCE
     * =============================================================
     */
    private String determineSource(
            NutrientBalanceRequest request
    ) {

        String requestedSource =
                normalize(
                        request.getSource()
                );


        /*
         * ---------------------------------------------------------
         * Keep explicit source.
         * ---------------------------------------------------------
         */

        if (
                !"UNKNOWN".equals(
                        requestedSource
                )
        ) {

            return request
                    .getSource()
                    .trim();
        }


        /*
         * ---------------------------------------------------------
         * Explicit test status but no source.
         * ---------------------------------------------------------
         */

        String requestedStatus =
                normalize(
                        request.getDataStatus()
                );

        if (
                "TEST".equals(
                        requestedStatus
                )
                        ||
                        "SYNTHETIC".equals(
                                requestedStatus
                        )
        ) {

            return "TEST_NUTRIENT_BALANCE_INPUT";
        }


        /*
         * ---------------------------------------------------------
         * Some nutrient accounting data was supplied.
         * ---------------------------------------------------------
         */

        if (
                hasAnyNutrientInput(
                        request
                )
                        ||
                        hasAnyNutrientUptake(
                                request
                        )
                        ||
                        hasAnyResidueReturn(
                                request
                        )
                        ||
                        request.getNitrogenBiologicalFixation() != null
        ) {

            return "NUTRIENT_BALANCE_INPUT";
        }


        return "BACKEND_VALIDATION";
    }


    /*
     * =============================================================
     * CHECK TEST SOURCE
     * =============================================================
     */
    private boolean isTestSource(
            String source
    ) {

        if (
                source == null
                        ||
                        "UNKNOWN".equals(
                                source
                        )
        ) {

            return false;
        }

        return source.contains(
                "TEST"
        )
                ||
                source.contains(
                        "SYNTHETIC"
                )
                ||
                source.contains(
                        "DEMO"
                );
    }


    /*
     * =============================================================
     * CHECK ANY NUTRIENT INPUT
     * =============================================================
     */
    private boolean hasAnyNutrientInput(
            NutrientBalanceRequest request
    ) {

        return request.getNitrogenInput() != null
                ||
                request.getPhosphorusInput() != null
                ||
                request.getPotassiumInput() != null
                ||
                request.getSulphurInput() != null
                ||
                request.getZincInput() != null;
    }


    /*
     * =============================================================
     * CHECK ANY UPTAKE
     * =============================================================
     */
    private boolean hasAnyNutrientUptake(
            NutrientBalanceRequest request
    ) {

        return request.getNitrogenUptake() != null
                ||
                request.getPhosphorusUptake() != null
                ||
                request.getPotassiumUptake() != null
                ||
                request.getSulphurUptake() != null
                ||
                request.getZincUptake() != null;
    }


    /*
     * =============================================================
     * CHECK ANY RESIDUE RETURN
     * =============================================================
     */
    private boolean hasAnyResidueReturn(
            NutrientBalanceRequest request
    ) {

        return request.getNitrogenResidueReturn() != null
                ||
                request.getPhosphorusResidueReturn() != null
                ||
                request.getPotassiumResidueReturn() != null
                ||
                request.getSulphurResidueReturn() != null
                ||
                request.getZincResidueReturn() != null;
    }


    /*
     * =============================================================
     * NORMALIZE STRING
     * =============================================================
     */
    private String normalize(
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return "UNKNOWN";
        }

        return value
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }


    /*
     * =============================================================
     * VALIDATE REQUEST
     * =============================================================
     */
    private void validateRequest(
            NutrientBalanceRequest request
    ) {

        if (
                request == null
        ) {

            throw new RuntimeException(
                    "Request cannot be null"
            );
        }


        if (
                request.getFarmId() == null
        ) {

            throw new RuntimeException(
                    "Farm ID is required"
            );
        }


        if (
                request.getPeriodStart() == null
        ) {

            throw new RuntimeException(
                    "Period start is required"
            );
        }


        if (
                request.getPeriodEnd() == null
        ) {

            throw new RuntimeException(
                    "Period end is required"
            );
        }


        if (
                request.getPeriodStart()
                        .isAfter(
                                request.getPeriodEnd()
                        )
        ) {

            throw new RuntimeException(
                    "Period start cannot be after period end"
            );
        }


        validateNonNegative(
                request.getNitrogenInput(),
                "Nitrogen input"
        );

        validateNonNegative(
                request.getPhosphorusInput(),
                "Phosphorus input"
        );

        validateNonNegative(
                request.getPotassiumInput(),
                "Potassium input"
        );

        validateNonNegative(
                request.getSulphurInput(),
                "Sulphur input"
        );

        validateNonNegative(
                request.getZincInput(),
                "Zinc input"
        );


        validateNonNegative(
                request.getNitrogenUptake(),
                "Nitrogen uptake"
        );

        validateNonNegative(
                request.getPhosphorusUptake(),
                "Phosphorus uptake"
        );

        validateNonNegative(
                request.getPotassiumUptake(),
                "Potassium uptake"
        );

        validateNonNegative(
                request.getSulphurUptake(),
                "Sulphur uptake"
        );

        validateNonNegative(
                request.getZincUptake(),
                "Zinc uptake"
        );


        validateNonNegative(
                request.getNitrogenResidueReturn(),
                "Nitrogen residue return"
        );

        validateNonNegative(
                request.getPhosphorusResidueReturn(),
                "Phosphorus residue return"
        );

        validateNonNegative(
                request.getPotassiumResidueReturn(),
                "Potassium residue return"
        );

        validateNonNegative(
                request.getSulphurResidueReturn(),
                "Sulphur residue return"
        );

        validateNonNegative(
                request.getZincResidueReturn(),
                "Zinc residue return"
        );


        validateNonNegative(
                request.getNitrogenBiologicalFixation(),
                "Nitrogen biological fixation"
        );
    }


    /*
     * =============================================================
     * NON-NEGATIVE VALIDATION
     * =============================================================
     */
    private void validateNonNegative(
            Double value,
            String fieldName
    ) {

        if (
                value == null
        ) {

            return;
        }

        if (
                value.isNaN()
                        ||
                        value.isInfinite()
        ) {

            throw new RuntimeException(
                    fieldName +
                            " must be a finite number"
            );
        }

        if (
                value < 0
        ) {

            throw new RuntimeException(
                    fieldName +
                            " cannot be negative"
            );
        }
    }


    /*
     * =============================================================
     * VERIFY FARM
     * =============================================================
     */
    private void verifyFarmExists(
            UUID farmId
    ) {

        if (
                !farmRepository.existsById(
                        farmId
                )
        ) {

            throw new RuntimeException(
                    "Farm not found with ID: "
                            + farmId
            );
        }
    }


    /*
     * =============================================================
     * VERIFY CROP
     * =============================================================
     */
    private void verifyCropExists(
            UUID cropId
    ) {

        if (
                !cropRepository.existsById(
                        cropId
                )
        ) {

            throw new RuntimeException(
                    "Crop not found with ID: "
                            + cropId
            );
        }
    }


    /*
     * =============================================================
     * VALIDATE REAL FERTILIZER INPUT
     * =============================================================
     */
    private void validateFiniteNonNegative(
            Double value,
            String fieldName
    ) {

        if (
                value == null
        ) {

            return;
        }

        if (
                value.isNaN()
                        ||
                        value.isInfinite()
        ) {

            throw new RuntimeException(
                    fieldName +
                            " must be a finite number"
            );
        }

        if (
                value < 0
        ) {

            throw new RuntimeException(
                    fieldName +
                            " cannot be negative"
            );
        }
    }


    /*
     * =============================================================
     * BUILD INPUT AGGREGATION AUDIT NOTE
     * =============================================================
     */
    private String buildInputAggregationNotes(
            List<FertilizerInput> inputs
    ) {

        int recordCount =
                inputs == null
                        ? 0
                        : inputs.size();


        return
                "Generated from " +
                        recordCount +
                        " real fertilizer-input record(s). " +

                        "Nutrient inputs are aggregated from stored " +
                        "fertilizer application records. " +

                        "Crop uptake, residue nutrient return and " +
                        "biological nitrogen fixation were not supplied " +
                        "by this source and therefore remain unavailable. " +

                        "No net nutrient balance is calculated until " +
                        "the required accounting components are available.";
    }
}