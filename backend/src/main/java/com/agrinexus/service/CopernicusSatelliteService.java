package com.agrinexus.service;

import com.agrinexus.config.CopernicusSatelliteProperties;
import com.agrinexus.dto.SatelliteFieldDataRequest;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.SatelliteObservation;
import com.agrinexus.repository.FarmRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CopernicusSatelliteService {

    /*
     * ============================================================
     * CONSTANTS
     * ============================================================
     */

    private static final String DATA_SOURCE =
            "COPERNICUS_DATA_SPACE";

    private static final String SATELLITE_SOURCE =
            "SENTINEL-2 L2A";

    private static final String COPERNICUS_TOKEN_URL =
            "https://identity.dataspace.copernicus.eu/auth/realms/CDSE/protocol/openid-connect/token";

    /*
     * Maximum number of real Sentinel-2 scenes that will be tested
     * before reporting that no usable satellite observation exists.
     */
    private static final int MAX_SCENE_ATTEMPTS = 20;


    /*
     * ============================================================
     * SENTINEL-2 STATISTICAL API EVALSCRIPT
     * ============================================================
     *
     * Outputs:
     *
     * 1. NDVI
     * 2. NDWI
     * 3. Cloud
     *
     * Important:
     *
     * - Invalid/no-data spectral pixels are excluded.
     * - Cloud/shadow/cirrus/snow pixels are excluded from NDVI/NDWI.
     * - Division by zero is prevented.
     * - NaN spectral values are rejected.
     * - No artificial NDVI value is generated.
     */

    private static final String EVALSCRIPT = """
            //VERSION=3

            function setup() {

              return {

                input: [{
                  bands: [
                    "B03",
                    "B04",
                    "B08",
                    "SCL",
                    "dataMask"
                  ]
                }],

                output: [

                  {
                    id: "ndvi",
                    bands: 1,
                    sampleType: "FLOAT32"
                  },

                  {
                    id: "ndwi",
                    bands: 1,
                    sampleType: "FLOAT32"
                  },

                  {
                    id: "cloud",
                    bands: 1,
                    sampleType: "FLOAT32"
                  },

                  {
                    id: "dataMask",
                    bands: [
                      "ndvi",
                      "ndwi",
                      "cloud"
                    ]
                  }
                ]
              };
            }


            function evaluatePixel(sample) {

              /*
               * ----------------------------------------------------
               * VALIDATE SPECTRAL VALUES
               * ----------------------------------------------------
               *
               * NaN is the only JavaScript value that is not equal
               * to itself, therefore:
               *
               * value === value
               *
               * is a safe NaN check.
               */

              var validB03 =
                    sample.B03 === sample.B03 &&
                    sample.B03 >= 0;

              var validB04 =
                    sample.B04 === sample.B04 &&
                    sample.B04 >= 0;

              var validB08 =
                    sample.B08 === sample.B08 &&
                    sample.B08 >= 0;


              /*
               * ----------------------------------------------------
               * CLOUD / INVALID SCL CLASSES
               * ----------------------------------------------------
               *
               * 0  = No data
               * 1  = Saturated / defective
               * 3  = Cloud shadow
               * 7  = Cloud low probability / unclassified
               * 8  = Cloud medium probability
               * 9  = Cloud high probability
               * 10 = Cirrus
               * 11 = Snow / ice
               */

              var invalidScl =
                    (
                        sample.SCL === 0 ||
                        sample.SCL === 1 ||
                        sample.SCL === 3 ||
                        sample.SCL === 7 ||
                        sample.SCL === 8 ||
                        sample.SCL === 9 ||
                        sample.SCL === 10 ||
                        sample.SCL === 11
                    );


              /*
               * ----------------------------------------------------
               * CLOUD FLAG
               * ----------------------------------------------------
               */

              var cloud =
                    invalidScl
                    ? 1
                    : 0;


              /*
               * ----------------------------------------------------
               * NDVI
               * ----------------------------------------------------
               */

              var ndviDenominator =
                    sample.B08 + sample.B04;

              var validNdvi =
                    sample.dataMask === 1 &&
                    validB04 &&
                    validB08 &&
                    !invalidScl &&
                    ndviDenominator > 0.000001;

              var ndvi = 0;

              if (validNdvi) {

                ndvi =
                    (sample.B08 - sample.B04)
                    /
                    ndviDenominator;
              }


              /*
               * ----------------------------------------------------
               * NDWI
               * ----------------------------------------------------
               */

              var ndwiDenominator =
                    sample.B03 + sample.B08;

              var validNdwi =
                    sample.dataMask === 1 &&
                    validB03 &&
                    validB08 &&
                    !invalidScl &&
                    ndwiDenominator > 0.000001;

              var ndwi = 0;

              if (validNdwi) {

                ndwi =
                    (sample.B03 - sample.B08)
                    /
                    ndwiDenominator;
              }


              /*
               * ----------------------------------------------------
               * MASKS
               * ----------------------------------------------------
               *
               * NDVI and NDWI use only valid clear pixels.
               *
               * Cloud uses all valid data pixels.
               */

              var vegetationMask =
                    validNdvi
                    ? 1
                    : 0;

              var waterMask =
                    validNdwi
                    ? 1
                    : 0;

              var cloudMask =
                    sample.dataMask === 1
                    ? 1
                    : 0;


              return {

                ndvi: [
                  ndvi
                ],

                ndwi: [
                  ndwi
                ],

                cloud: [
                  cloud
                ],

                dataMask: [

                  vegetationMask,

                  waterMask,

                  cloudMask

                ]
              };
            }
            """;


    /*
     * ============================================================
     * DEPENDENCIES
     * ============================================================
     */

    private final RestClient.Builder restClientBuilder;

    private final CopernicusSatelliteProperties properties;

    private final FarmRepository farmRepository;

    private final SatelliteObservationService
            satelliteObservationService;

    private final SatelliteFieldDataService
            satelliteFieldDataService;

    private final CombinedRiskAnalysisService
            combinedRiskAnalysisService;


    /*
     * ============================================================
     * JACKSON
     * ============================================================
     */

    private final ObjectMapper objectMapper =
            new ObjectMapper();


    /*
     * ============================================================
     * MAIN SATELLITE SYNC
     * ============================================================
     */

    public SatelliteObservation syncFarm(
            UUID farmId
    ) {

        if (farmId == null) {

            throw new IllegalArgumentException(
                    "farmId is required."
            );
        }


        /*
         * --------------------------------------------------------
         * FIND FARM
         * --------------------------------------------------------
         */

        Farm farm =
                farmRepository
                        .findById(farmId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Farm not found with ID: "
                                                + farmId
                                )
                        );


        /*
         * --------------------------------------------------------
         * VALIDATE CONFIGURATION
         * --------------------------------------------------------
         */

        validateCredentials();

        validateCoordinates(farm);


        /*
         * --------------------------------------------------------
         * GET REAL COPERNICUS ACCESS TOKEN
         * --------------------------------------------------------
         */

        String accessToken =
                getAccessToken();


        /*
         * --------------------------------------------------------
         * FIND REAL SENTINEL-2 CANDIDATE SCENES
         * --------------------------------------------------------
         */

        List<CatalogScene> scenes =
                findSceneCandidates(
                        farm,
                        accessToken
                );


        if (scenes.isEmpty()) {

            throw new IllegalStateException(
                    "No Sentinel-2 L2A scene was found for this farm "
                            + "in the last "
                            + properties.getLookbackDays()
                            + " days."
            );
        }


        /*
         * --------------------------------------------------------
         * TRY MULTIPLE REAL SCENES
         * --------------------------------------------------------
         *
         * A scene can exist in the Catalog but still produce
         * no usable NDVI over the farm AOI.
         *
         * Example:
         *
         * - AOI completely covered by clouds
         * - cloud shadow
         * - snow/ice
         * - no-data pixels
         *
         * Instead of failing immediately, try the next real scene.
         */

        SatelliteStatistics statistics = null;

        CatalogScene selectedScene = null;

        List<String> rejectedScenes =
                new ArrayList<>();


        int attempts =
                Math.min(
                        scenes.size(),
                        MAX_SCENE_ATTEMPTS
                );


        for (
                int index = 0;
                index < attempts;
                index++
        ) {

            CatalogScene scene =
                    scenes.get(index);


            try {

                SatelliteStatistics candidateStatistics =
                        getStatistics(
                                farm,
                                scene,
                                accessToken
                        );


                if (
                        candidateStatistics != null
                                &&
                                candidateStatistics.ndvi() != null
                ) {

                    statistics =
                            candidateStatistics;

                    selectedScene =
                            scene;

                    break;
                }


                rejectedScenes.add(
                        scene.id()
                                + " -> no usable NDVI"
                );


            } catch (IllegalStateException exception) {

                /*
                 * This is expected when a real scene has no usable
                 * pixels. Try the next real scene.
                 */

                rejectedScenes.add(
                        scene.id()
                                + " -> "
                                + exception.getMessage()
                );
            }
        }


        /*
         * --------------------------------------------------------
         * NO USABLE REAL SCENE
         * --------------------------------------------------------
         */

        if (
                statistics == null
                        ||
                        selectedScene == null
        ) {

            throw new IllegalStateException(
                    "No usable real Sentinel-2 NDVI observation "
                            + "was available for the farm AOI in the "
                            + "last "
                            + properties.getLookbackDays()
                            + " days. "
                            + "Copernicus returned scenes, but their "
                            + "AOI pixels did not contain a usable "
                            + "NDVI after quality masking. "
                            + "Scenes checked: "
                            + attempts
            );
        }


        /*
         * --------------------------------------------------------
         * VALIDATE FINAL NDVI
         * --------------------------------------------------------
         */

        validateFiniteValue(
                statistics.ndvi(),
                "NDVI"
        );


        if (statistics.ndwi() != null) {

            validateFiniteValue(
                    statistics.ndwi(),
                    "NDWI"
            );
        }


        if (statistics.cloudCoverPercent() != null) {

            validateFiniteValue(
                    statistics.cloudCoverPercent(),
                    "cloud coverage"
            );
        }


        /*
         * --------------------------------------------------------
         * CONFIDENCE
         * --------------------------------------------------------
         */

        double confidence =
                calculateConfidence(
                        statistics.cloudCoverPercent()
                );


        /*
         * --------------------------------------------------------
         * SAVE SATELLITE OBSERVATION
         * --------------------------------------------------------
         */

        SatelliteObservation observation =
                satelliteObservationService.create(

                        farmId,

                        null,

                        selectedScene.observedAt(),

                        SATELLITE_SOURCE,

                        selectedScene.id(),

                        statistics.ndvi(),

                        statistics.ndwi(),

                        statistics.cloudCoverPercent(),

                        10.0,

                        "OBSERVED",

                        DATA_SOURCE,

                        confidence,

                        false
                );


        /*
         * --------------------------------------------------------
         * SAVE SATELLITE FIELD DATA
         * --------------------------------------------------------
         *
         * Sentinel-2 optical imagery does not directly provide
         * validated soil-moisture percentage.
         *
         * Therefore soilMoisture remains null.
         */

        SatelliteFieldDataRequest fieldDataRequest =
                SatelliteFieldDataRequest.builder()

                        .farmId(
                                farmId
                        )

                        .ndvi(
                                statistics.ndvi()
                        )

                        .vegetationHealth(
                                ndviToVegetationHealth(
                                        statistics.ndvi()
                                )
                        )

                        .soilMoisture(
                                null
                        )

                        .cloudCoverage(
                                statistics.cloudCoverPercent()
                        )

                        .build();


        satelliteFieldDataService
                .createSatelliteFieldData(
                        fieldDataRequest
                );


        /*
         * --------------------------------------------------------
         * REFRESH COMBINED RISK
         * --------------------------------------------------------
         *
         * Existing CombinedRiskAnalysisService remains unchanged.
         *
         * IMPORTANT:
         *
         * Weather = 60%
         * Satellite = 40%
         *
         * Existing formula is NOT changed here.
         */

        combinedRiskAnalysisService
                .analyzeFarmRisk(
                        farmId
                );


        return observation;
    }


    /*
     * ============================================================
     * COPERNICUS OAUTH
     * ============================================================
     */

    private String getAccessToken() {

        RestClient client =
                restClientBuilder.build();


        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();


        form.add(
                "grant_type",
                "client_credentials"
        );


        form.add(
                "client_id",
                properties.getClientId()
        );


        form.add(
                "client_secret",
                properties.getClientSecret()
        );


        try {

            String responseBody =
                    client.post()

                            .uri(
                                    COPERNICUS_TOKEN_URL
                            )

                            .contentType(
                                    MediaType.APPLICATION_FORM_URLENCODED
                            )

                            .body(
                                    form
                            )

                            .retrieve()

                            .body(
                                    String.class
                            );


            JsonNode response =
                    parseJson(
                            responseBody,
                            "Copernicus authentication"
                    );


            JsonNode tokenNode =
                    response.path(
                            "access_token"
                    );


            if (
                    tokenNode.isMissingNode()
                            ||
                            tokenNode.isNull()
                            ||
                            tokenNode.asText().isBlank()
            ) {

                throw new IllegalStateException(
                        "Copernicus authentication returned "
                                + "no access token."
                );
            }


            return tokenNode.asText();


        } catch (
                RestClientResponseException exception
        ) {

            throw new IllegalStateException(
                    "Copernicus authentication failed. HTTP "
                            + exception
                            .getStatusCode()
                            .value()
                            + " - "
                            + exception
                            .getResponseBodyAsString(),
                    exception
            );
        }
    }


    /*
     * ============================================================
     * FIND SENTINEL-2 SCENE CANDIDATES
     * ============================================================
     *
     * Returns multiple real scenes instead of only one.
     *
     * This is the main fix for the NaN situation.
     */

    private List<CatalogScene> findSceneCandidates(
            Farm farm,
            String accessToken
    ) {

        Instant now =
                Instant.now();


        Instant from =
                now.minus(
                        Duration.ofDays(
                                properties.getLookbackDays()
                        )
                );


        Map<String, Object> request =
                new HashMap<>();


        /*
         * --------------------------------------------------------
         * COLLECTION
         * --------------------------------------------------------
         */

        request.put(
                "collections",
                List.of(
                        properties.getCollection()
                )
        );


        /*
         * --------------------------------------------------------
         * FARM BBOX
         * --------------------------------------------------------
         */

        request.put(
                "bbox",
                createBbox(
                        farm
                )
        );


        /*
         * --------------------------------------------------------
         * TIME RANGE
         * --------------------------------------------------------
         */

        request.put(
                "datetime",
                from.toString()
                        + "/"
                        + now.toString()
        );


        /*
         * --------------------------------------------------------
         * RETURN MULTIPLE REAL SCENES
         * --------------------------------------------------------
         */

        request.put(
                "limit",
                50
        );


        /*
         * --------------------------------------------------------
         * FIELDS
         * --------------------------------------------------------
         */

        request.put(
                "fields",
                Map.of(
                        "include",
                        List.of(
                                "id",
                                "properties.datetime",
                                "properties.eo:cloud_cover"
                        ),

                        "exclude",
                        List.of()
                )
        );


        /*
         * IMPORTANT:
         *
         * Do not send "sortby".
         *
         * The Copernicus Catalog endpoint previously rejected
         * that request with HTTP 400.
         */


        /*
         * --------------------------------------------------------
         * CALL CATALOG
         * --------------------------------------------------------
         */

        List<JsonNode> features =
                postCatalog(
                        request,
                        accessToken
                );


        /*
         * --------------------------------------------------------
         * CONVERT FEATURES
         * --------------------------------------------------------
         */

        List<CatalogScene> scenes =
                features.stream()

                        .map(
                                this::toCatalogScene
                        )

                        .filter(
                                scene ->
                                        scene != null
                        )

                        .sorted(
                                Comparator.comparing(
                                        CatalogScene::observedAt
                                ).reversed()
                        )

                        .toList();


        /*
         * --------------------------------------------------------
         * PRIORITIZE LOW-CLOUD SCENES
         * --------------------------------------------------------
         *
         * We still keep real higher-cloud scenes as fallback.
         *
         * We do NOT create or modify cloud values.
         */

        List<CatalogScene> preferred =
                scenes.stream()

                        .filter(
                                scene ->
                                        scene.cloudCover() == null
                                                ||
                                                scene.cloudCover()
                                                        <=
                                                        properties
                                                                .getMaxCloudCover()
                        )

                        .toList();


        List<CatalogScene> fallback =
                scenes.stream()

                        .filter(
                                scene ->
                                        !preferred.contains(
                                                scene
                                        )
                        )

                        .toList();


        List<CatalogScene> ordered =
                new ArrayList<>();


        ordered.addAll(
                preferred
        );

        ordered.addAll(
                fallback
        );


        return ordered;
    }


    /*
     * ============================================================
     * CATALOG REQUEST
     * ============================================================
     */

    private List<JsonNode> postCatalog(
            Map<String, Object> request,
            String accessToken
    ) {

        RestClient client =
                restClientBuilder.build();

        try {

            String responseBody =
                    client.post()

                            .uri(
                                    properties.getCatalogUrl()
                            )

                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )

                            /*
                             * IMPORTANT:
                             *
                             * Do NOT set:
                             *
                             * .accept(MediaType.APPLICATION_JSON)
                             *
                             * Copernicus Catalog can return its STAC
                             * response using its own supported media type.
                             */

                            .header(
                                    "Authorization",
                                    "Bearer " + accessToken
                            )

                            .body(
                                    request
                            )

                            .retrieve()

                            .body(
                                    String.class
                            );


            JsonNode response =
                    parseJson(
                            responseBody,
                            "Copernicus Catalog"
                    );


            if (
                    !response.has("features")
                            ||
                            !response
                                    .get("features")
                                    .isArray()
            ) {

                return List.of();
            }


            List<JsonNode> features =
                    new ArrayList<>();


            response
                    .get("features")
                    .forEach(
                            features::add
                    );


            return features;


        } catch (
                RestClientResponseException exception
        ) {

            throw new IllegalStateException(
                    "Copernicus Catalog request failed. HTTP "
                            + exception
                            .getStatusCode()
                            .value()
                            + " - "
                            + exception
                            .getResponseBodyAsString(),
                    exception
            );
        }
    }


    /*
     * ============================================================
     * STATISTICAL API
     * ============================================================
     */

    private SatelliteStatistics getStatistics(
            Farm farm,
            CatalogScene scene,
            String accessToken
    ) {

        /*
         * --------------------------------------------------------
         * SCENE UTC DAY
         * --------------------------------------------------------
         */

        Instant sceneInstant =
                scene
                        .observedAt()
                        .toInstant(
                                ZoneOffset.UTC
                        );


        Instant start =
                sceneInstant
                        .truncatedTo(
                                ChronoUnit.DAYS
                        );


        Instant end =
                start.plus(
                        1,
                        ChronoUnit.DAYS
                );


        /*
         * --------------------------------------------------------
         * INPUT DATA
         * --------------------------------------------------------
         */

        Map<String, Object> inputData =
                new HashMap<>();


        inputData.put(
                "type",
                properties.getCollection()
        );


        /*
         * Select the lowest cloud-cover acquisition in the
         * requested interval.
         */

        inputData.put(
                "dataFilter",
                Map.of(
                        "mosaickingOrder",
                        "leastCC"
                )
        );


        /*
         * --------------------------------------------------------
         * REQUEST
         * --------------------------------------------------------
         */

        Map<String, Object> request =
                new HashMap<>();


        request.put(
                "input",
                Map.of(

                        "bounds",
                        Map.of(
                                "bbox",
                                createBbox(
                                        farm
                                )
                        ),

                        "data",
                        List.of(
                                inputData
                        )
                )
        );


        /*
         * --------------------------------------------------------
         * DAILY AGGREGATION
         * --------------------------------------------------------
         */

        request.put(
                "aggregation",
                Map.of(

                        "timeRange",
                        Map.of(

                                "from",
                                start.toString(),

                                "to",
                                end.toString()
                        ),

                        "aggregationInterval",
                        Map.of(
                                "of",
                                "P1D"
                        ),

                        "evalscript",
                        EVALSCRIPT,

                        "resx",
                        10,

                        "resy",
                        10
                )
        );


        RestClient client =
                restClientBuilder.build();


        /*
         * --------------------------------------------------------
         * STATISTICAL API REQUEST
         * --------------------------------------------------------
         */

        try {

            String responseBody =
                    client.post()

                            .uri(
                                    properties
                                            .getStatisticsUrl()
                            )

                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )

                            .accept(
                                    MediaType.APPLICATION_JSON
                            )

                            .header(
                                    "Authorization",
                                    "Bearer " + accessToken
                            )

                            .body(
                                    request
                            )

                            .retrieve()

                            .body(
                                    String.class
                            );


            /*
             * Safe diagnostic.
             *
             * No OAuth token or client secret is printed.
             */

            System.out.println();
            System.out.println(
                    "============================================================"
            );
            System.out.println(
                    "COPERNICUS STATISTICAL API"
            );
            System.out.println(
                    "Scene: " + scene.id()
            );
            System.out.println(
                    "Observed At: " + scene.observedAt()
            );
            System.out.println(
                    "Raw Response:"
            );
            System.out.println(
                    responseBody
            );
            System.out.println(
                    "============================================================"
            );
            System.out.println();


            JsonNode response =
                    parseJson(
                            responseBody,
                            "Copernicus Statistical API"
                    );


            return parseStatistics(
                    response
            );


        } catch (
                RestClientResponseException exception
        ) {

            throw new IllegalStateException(
                    "Copernicus Statistical API request failed. "
                            + "HTTP "
                            + exception
                            .getStatusCode()
                            .value()
                            + " - "
                            + exception
                            .getResponseBodyAsString(),
                    exception
            );
        }
    }


    /*
     * ============================================================
     * PARSE STATISTICAL API RESPONSE
     * ============================================================
     */

    private SatelliteStatistics parseStatistics(
            JsonNode response
    ) {

        if (
                response == null
                        ||
                        !response.has("data")
                        ||
                        !response.get("data").isArray()
                        ||
                        response.get("data").isEmpty()
        ) {

            throw new IllegalStateException(
                    "Copernicus Statistical API returned no data."
            );
        }


        JsonNode data =
                response.get("data");


        /*
         * --------------------------------------------------------
         * FIND AN INTERVAL WITH REAL NUMERIC NDVI
         * --------------------------------------------------------
         */

        for (
                int index = data.size() - 1;
                index >= 0;
                index--
        ) {

            JsonNode interval =
                    data.get(index);


            JsonNode outputs =
                    interval.path(
                            "outputs"
                    );


            Double ndvi =
                    extractOutputMean(
                            outputs,
                            "ndvi"
                    );


            /*
             * IMPORTANT:
             *
             * If NDVI is NaN, extractOutputMean returns null.
             *
             * We therefore do not save it.
             */

            if (ndvi == null) {

                continue;
            }


            Double ndwi =
                    extractOutputMean(
                            outputs,
                            "ndwi"
                    );


            Double cloud =
                    extractOutputMean(
                            outputs,
                            "cloud"
                    );


            /*
             * Cloud output mean is 0..1.
             *
             * Convert to 0..100%.
             */

            if (cloud != null) {

                cloud =
                        clamp(
                                cloud * 100.0,
                                0.0,
                                100.0
                        );
            }


            return new SatelliteStatistics(
                    ndvi,
                    ndwi,
                    cloud
            );
        }


        /*
         * --------------------------------------------------------
         * NO USABLE NDVI
         * --------------------------------------------------------
         */

        JsonNode newestInterval =
                data.get(
                        data.size() - 1
                );


        JsonNode outputs =
                newestInterval.path(
                        "outputs"
                );


        List<String> outputNames =
                new ArrayList<>();


        if (outputs.isObject()) {

            outputs
                    .fieldNames()
                    .forEachRemaining(
                            outputNames::add
                    );
        }


        JsonNode ndviOutput =
                outputs.path(
                        "ndvi"
                );


        throw new IllegalStateException(
                "Copernicus Statistical API returned no usable "
                        + "NDVI value. Available outputs: "
                        + outputNames
                        + ". NDVI response: "
                        + ndviOutput
        );
    }


    /*
     * ============================================================
     * EXTRACT STATISTICAL MEAN
     * ============================================================
     *
     * Expected:
     *
     * output
     *   -> bands
     *      -> B0
     *         -> stats
     *            -> mean
     */

    private Double extractOutputMean(
            JsonNode outputs,
            String outputName
    ) {

        if (
                outputs == null
                        ||
                        !outputs.isObject()
        ) {

            return null;
        }


        JsonNode output =
                outputs.get(
                        outputName
                );


        /*
         * Case-insensitive fallback.
         */

        if (
                output == null
                        ||
                        output.isNull()
                        ||
                        output.isMissingNode()
        ) {

            var fields =
                    outputs.fields();


            while (fields.hasNext()) {

                var entry =
                        fields.next();


                if (
                        entry.getKey()
                                .equalsIgnoreCase(
                                        outputName
                                )
                ) {

                    output =
                            entry.getValue();

                    break;
                }
            }
        }


        if (
                output == null
                        ||
                        output.isNull()
                        ||
                        output.isMissingNode()
        ) {

            return null;
        }


        /*
         * --------------------------------------------------------
         * STANDARD FORMAT
         * --------------------------------------------------------
         */

        JsonNode bands =
                output.path(
                        "bands"
                );


        if (bands.isObject()) {

            /*
             * Standard B0.
             */

            JsonNode b0 =
                    bands.get(
                            "B0"
                    );


            if (
                    b0 != null
                            &&
                            !b0.isNull()
            ) {

                Double mean =
                        extractMeanFromNode(
                                b0
                        );


                if (mean != null) {

                    return mean;
                }
            }


            /*
             * Generic fallback for another band name.
             */

            var bandFields =
                    bands.fields();


            while (bandFields.hasNext()) {

                var entry =
                        bandFields.next();


                Double mean =
                        extractMeanFromNode(
                                entry.getValue()
                        );


                if (mean != null) {

                    return mean;
                }
            }
        }


        /*
         * --------------------------------------------------------
         * DIRECT STATS FALLBACK
         * --------------------------------------------------------
         */

        return extractMeanFromNode(
                output
        );
    }


    /*
     * ============================================================
     * EXTRACT MEAN FROM NODE
     * ============================================================
     */

    private Double extractMeanFromNode(
            JsonNode node
    ) {

        if (
                node == null
                        ||
                        node.isNull()
        ) {

            return null;
        }


        JsonNode stats =
                node.path(
                        "stats"
                );


        JsonNode mean =
                stats.path(
                        "mean"
                );


        if (!mean.isNumber()) {

            return null;
        }


        double value =
                mean.asDouble();


        /*
         * Reject:
         *
         * NaN
         * +Infinity
         * -Infinity
         */

        if (
                Double.isNaN(value)
                        ||
                        Double.isInfinite(value)
        ) {

            return null;
        }


        return value;
    }


    /*
     * ============================================================
     * CONVERT CATALOG FEATURE
     * ============================================================
     */

    private CatalogScene toCatalogScene(
            JsonNode feature
    ) {

        if (
                feature == null
                        ||
                        feature.isNull()
        ) {

            return null;
        }


        /*
         * --------------------------------------------------------
         * SCENE ID
         * --------------------------------------------------------
         */

        String id =
                text(
                        feature,
                        "id"
                );


        /*
         * --------------------------------------------------------
         * SCENE DATETIME
         * --------------------------------------------------------
         */

        String datetime =
                text(
                        feature,
                        "properties",
                        "datetime"
                );


        if (
                id == null
                        ||
                        datetime == null
        ) {

            return null;
        }


        LocalDateTime observedAt;


        try {

            observedAt =
                    Instant
                            .parse(
                                    datetime
                            )
                            .atZone(
                                    ZoneOffset.UTC
                            )
                            .toLocalDateTime();

        } catch (Exception exception) {

            return null;
        }


        /*
         * --------------------------------------------------------
         * CLOUD COVER
         * --------------------------------------------------------
         */

        Double cloudCover =
                number(
                        feature,
                        "properties",
                        "eo:cloud_cover"
                );


        return new CatalogScene(
                id,
                observedAt,
                cloudCover
        );
    }


    /*
     * ============================================================
     * FARM BBOX
     * ============================================================
     */

    private List<Double> createBbox(
            Farm farm
    ) {

        double latitude =
                farm.getLatitude();


        double longitude =
                farm.getLongitude();


        double half =
                properties
                        .getAoiHalfSizeDegrees();


        return List.of(

                longitude - half,

                latitude - half,

                longitude + half,

                latitude + half
        );
    }


    /*
     * ============================================================
     * CONFIDENCE
     * ============================================================
     */

    private double calculateConfidence(
            Double cloudCover
    ) {

        if (cloudCover == null) {

            return 70.0;
        }


        return Math.round(

                clamp(
                        100.0 - cloudCover,
                        0.0,
                        100.0
                )
                        * 100.0

        ) / 100.0;
    }


    /*
     * ============================================================
     * NDVI -> VEGETATION HEALTH
     * ============================================================
     *
     * This is an indicator derived from the real NDVI.
     *
     * It is not presented as a direct crop diagnosis.
     */

    private double ndviToVegetationHealth(
            double ndvi
    ) {

        return Math.round(

                clamp(
                        (ndvi + 1.0) * 50.0,
                        0.0,
                        100.0
                )
                        * 100.0

        ) / 100.0;
    }


    /*
     * ============================================================
     * FINITE VALUE VALIDATION
     * ============================================================
     */

    private void validateFiniteValue(
            Double value,
            String name
    ) {

        if (
                value == null
                        ||
                        Double.isNaN(value)
                        ||
                        Double.isInfinite(value)
        ) {

            throw new IllegalStateException(
                    "Invalid non-finite "
                            + name
                            + " received from Copernicus."
            );
        }
    }


    /*
     * ============================================================
     * CLAMP
     * ============================================================
     */

    private double clamp(
            double value,
            double min,
            double max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }


    /*
     * ============================================================
     * JSON PARSER
     * ============================================================
     */

    private JsonNode parseJson(
            String responseBody,
            String source
    ) {

        if (
                responseBody == null
                        ||
                        responseBody.isBlank()
        ) {

            throw new IllegalStateException(
                    source
                            + " returned an empty response."
            );
        }


        try {

            return objectMapper.readTree(
                    responseBody
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to parse JSON response from "
                            + source
                            + ". Response: "
                            + responseBody,
                    exception
            );
        }
    }


    /*
     * ============================================================
     * JSON TEXT HELPER
     * ============================================================
     */

    private String text(
            JsonNode node,
            String... path
    ) {

        JsonNode current =
                node;


        for (String part : path) {

            current =
                    current.path(
                            part
                    );
        }


        return current.isTextual()
                ? current.asText()
                : null;
    }


    /*
     * ============================================================
     * JSON NUMBER HELPER
     * ============================================================
     */

    private Double number(
            JsonNode node,
            String... path
    ) {

        JsonNode current =
                node;


        for (String part : path) {

            current =
                    current.path(
                            part
                    );
        }


        if (!current.isNumber()) {

            return null;
        }


        double value =
                current.asDouble();


        if (
                Double.isNaN(value)
                        ||
                        Double.isInfinite(value)
        ) {

            return null;
        }


        return value;
    }


    /*
     * ============================================================
     * VALIDATE COPERNICUS CREDENTIALS
     * ============================================================
     */

    private void validateCredentials() {

        if (
                properties.getClientId() == null
                        ||
                        properties
                                .getClientId()
                                .isBlank()
                        ||
                        properties.getClientSecret() == null
                        ||
                        properties
                                .getClientSecret()
                                .isBlank()
        ) {

            throw new IllegalStateException(
                    "Copernicus credentials are not configured. "
                            + "Set COPERNICUS_CLIENT_ID and "
                            + "COPERNICUS_CLIENT_SECRET."
            );
        }
    }


    /*
     * ============================================================
     * VALIDATE FARM COORDINATES
     * ============================================================
     */

    private void validateCoordinates(
            Farm farm
    ) {

        if (
                farm.getLatitude() == null
                        ||
                        farm.getLongitude() == null
        ) {

            throw new IllegalStateException(
                    "Farm must have latitude and longitude "
                            + "before satellite sync."
            );
        }


        if (
                farm.getLatitude() < -90
                        ||
                        farm.getLatitude() > 90
                        ||
                        farm.getLongitude() < -180
                        ||
                        farm.getLongitude() > 180
        ) {

            throw new IllegalStateException(
                    "Farm latitude/longitude values are invalid."
            );
        }
    }


    /*
     * ============================================================
     * INTERNAL RECORDS
     * ============================================================
     */

    private record CatalogScene(

            String id,

            LocalDateTime observedAt,

            Double cloudCover

    ) {
    }


    private record SatelliteStatistics(

            Double ndvi,

            Double ndwi,

            Double cloudCoverPercent

    ) {
    }
}