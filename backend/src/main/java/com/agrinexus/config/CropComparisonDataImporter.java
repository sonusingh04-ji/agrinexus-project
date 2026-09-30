package com.agrinexus.config;

import com.agrinexus.entity.AffordableCropOption;
import com.agrinexus.entity.Crop;
import com.agrinexus.repository.AffordableCropOptionRepository;
import com.agrinexus.repository.CropRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class CropComparisonDataImporter {

    private final CropRepository cropRepository;

    private final AffordableCropOptionRepository optionRepository;


    @Bean
    CommandLineRunner importCropComparisonData() {

        return args -> {

            ClassPathResource resource =
                    new ClassPathResource(
                            "crop_comparison.csv"
                    );

            if (!resource.exists()) {

                System.out.println(
                        "Crop comparison CSV not found. Skipping import."
                );

                return;
            }


            try (
                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            resource.getInputStream(),
                                            StandardCharsets.UTF_8
                                    )
                            )
            ) {

                String header = reader.readLine();

                if (header == null) {

                    System.out.println(
                            "Crop comparison CSV is empty."
                    );

                    return;
                }


                int processed = 0;
                int createdCrops = 0;
                int reusedCrops = 0;
                int createdOptions = 0;
                int updatedOptions = 0;
                int skipped = 0;


                String line;


                while ((line = reader.readLine()) != null) {

                    if (line.isBlank()) {
                        continue;
                    }


                    String[] values =
                            parseCsvLine(line);


                    /*
                     * Expected 20 columns.
                     */
                    if (values.length < 20) {

                        System.out.println(
                                "Skipping malformed row: "
                                        + line
                        );

                        skipped++;

                        continue;
                    }


                    String cropName =
                            clean(values[1]);


                    if (cropName.isBlank()) {

                        skipped++;

                        continue;
                    }


                    /*
                     * =================================================
                     * FIND OR CREATE CROP MASTER
                     * =================================================
                     */

                    Crop crop =
                            cropRepository
                                    .findByNameIgnoreCase(
                                            cropName
                                    );


                    if (crop == null) {

                        crop =
                                Crop.builder()

                                        .name(
                                                cropName
                                        )

                                        .category(
                                                clean(values[3])
                                        )

                                        .season(
                                                clean(values[2])
                                        )

                                        .idealTemperatureMin(
                                                parseDouble(values[9])
                                        )

                                        .idealTemperatureMax(
                                                parseDouble(values[10])
                                        )

                                        .soilType(
                                                clean(values[4])
                                        )

                                        .soilPhMin(
                                                parseDouble(values[5])
                                        )

                                        .soilPhMax(
                                                parseDouble(values[6])
                                        )

                                        .waterNeed(
                                                buildWaterNeed(
                                                        values[7],
                                                        values[8],
                                                        values[16]
                                                )
                                        )

                                        .build();


                        crop =
                                cropRepository.save(
                                        crop
                                );


                        createdCrops++;


                        System.out.println(
                                "Created missing Crop: "
                                        + cropName
                        );

                    } else {

                        reusedCrops++;
                    }


                    /*
                     * =================================================
                     * FIND EXISTING AFFORDABILITY OPTION
                     * =================================================
                     */

                    AffordableCropOption option =
                            optionRepository
                                    .findFirstByCropId(
                                            crop.getId()
                                    );


                    boolean newOption =
                            option == null;


                    if (newOption) {

                        option =
                                new AffordableCropOption();
                    }


                    /*
                     * =================================================
                     * COMPARISON DATA
                     * =================================================
                     */

                    option.setCrop(crop);

                    option.setWaterRequirementMmMin(
                            parseDouble(values[7])
                    );

                    option.setWaterRequirementMmMax(
                            parseDouble(values[8])
                    );

                    option.setWaterDemandClass(
                            clean(values[16])
                    );

                    option.setCropDurationDaysMin(
                            parseInteger(values[11])
                    );

                    option.setCropDurationDaysMax(
                            parseInteger(values[12])
                    );

                    option.setEstimatedInputCostInrPerHa(
                            parseDouble(values[13])
                    );

                    option.setEstimatedEnergyFuelCostInrPerHa(
                            parseDouble(values[14])
                    );

                    option.setEstimatedFertilizerPesticideCostInrPerHa(
                            parseDouble(values[15])
                    );

                    option.setAgronomicSafetyTradeoff(
                            clean(values[17])
                    );

                    option.setRecommendationRule(
                            clean(values[18])
                    );

                    option.setDataStatus(
                            clean(values[19])
                    );

                    option.setSource(
                            "crop_comparison.csv"
                    );

                    option.setNotes(
                            "Planning comparison dataset. "
                                    + "Values are estimates and "
                                    + "must be calibrated with "
                                    + "current local agricultural "
                                    + "guidance."
                    );


                    optionRepository.save(option);


                    if (newOption) {

                        createdOptions++;

                    } else {

                        updatedOptions++;
                    }


                    processed++;
                }


                System.out.println(
                        "=========================================="
                );

                System.out.println(
                        "Crop comparison import completed."
                );

                System.out.println(
                        "Processed rows  = "
                                + processed
                );

                System.out.println(
                        "Created crops   = "
                                + createdCrops
                );

                System.out.println(
                        "Reused crops    = "
                                + reusedCrops
                );

                System.out.println(
                        "Created options = "
                                + createdOptions
                );

                System.out.println(
                        "Updated options = "
                                + updatedOptions
                );

                System.out.println(
                        "Skipped rows    = "
                                + skipped
                );

                System.out.println(
                        "=========================================="
                );
            }
        };
    }


    /*
     * =========================================================
     * Build readable water requirement text
     * =========================================================
     */

    private String buildWaterNeed(
            String minValue,
            String maxValue,
            String demandClass
    ) {

        String min =
                clean(minValue);

        String max =
                clean(maxValue);

        String demand =
                clean(demandClass);


        if (
                !min.isBlank()
                        && !max.isBlank()
                        && !demand.isBlank()
        ) {

            return
                    min
                            + "-"
                            + max
                            + " mm; "
                            + demand
                            + " demand";
        }


        if (
                !min.isBlank()
                        && !max.isBlank()
        ) {

            return
                    min
                            + "-"
                            + max
                            + " mm";
        }


        return demand;
    }


    /*
     * =========================================================
     * CSV parser
     * =========================================================
     */

    private String[] parseCsvLine(
            String line
    ) {

        java.util.List<String> result =
                new java.util.ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        boolean insideQuotes = false;


        for (
                int i = 0;
                i < line.length();
                i++
        ) {

            char ch =
                    line.charAt(i);


            if (ch == '"') {

                if (
                        insideQuotes
                                && i + 1 < line.length()
                                && line.charAt(i + 1) == '"'
                ) {

                    current.append('"');

                    i++;

                } else {

                    insideQuotes =
                            !insideQuotes;
                }

            } else if (
                    ch == ','
                            && !insideQuotes
            ) {

                result.add(
                        current.toString()
                );

                current.setLength(0);

            } else {

                current.append(ch);
            }
        }


        result.add(
                current.toString()
        );


        return result.toArray(
                new String[0]
        );
    }


    /*
     * =========================================================
     * Clean CSV value
     * =========================================================
     */

    private String clean(
            String value
    ) {

        if (value == null) {

            return "";
        }


        return value
                .replace("\uFEFF", "")
                .trim()
                .replaceAll(
                        "^\"|\"$",
                        ""
                );
    }


    /*
     * =========================================================
     * Double parser
     * =========================================================
     */

    private Double parseDouble(
            String value
    ) {

        String cleaned =
                clean(value);


        if (cleaned.isBlank()) {

            return null;
        }


        try {

            return Double.parseDouble(
                    cleaned
            );

        } catch (NumberFormatException e) {

            return null;
        }
    }


    /*
     * =========================================================
     * Integer parser
     * =========================================================
     */

    private Integer parseInteger(
            String value
    ) {

        String cleaned =
                clean(value);


        if (cleaned.isBlank()) {

            return null;
        }


        try {

            return Integer.parseInt(
                    cleaned
            );

        } catch (NumberFormatException e) {

            return null;
        }
    }
}