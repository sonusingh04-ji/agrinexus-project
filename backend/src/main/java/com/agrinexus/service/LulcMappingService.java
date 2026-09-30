package com.agrinexus.service;

import com.agrinexus.config.LulcProperties;
import com.agrinexus.dto.LulcResponse;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.LulcObservation;
import com.agrinexus.repository.FarmRepository;
import com.agrinexus.repository.LulcObservationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class LulcMappingService {

    private static final String PRODUCT_NAME =
            "Land Cover Map at 10m - Annual V1";

    private static final String DATA_SOURCE =
            "COPERNICUS_CLMS";

    private static final String CRS84 =
            "http://www.opengis.net/def/crs/OGC/1.3/CRS84";

    private final LulcProperties properties;

    private final FarmRepository farmRepository;

    private final LulcObservationRepository repository;

    /*
     * Use a local ObjectMapper so this service does not depend on
     * Spring's Jackson bean configuration.
     */
    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final RestClient restClient =
            RestClient.builder().build();

    private final ReentrantLock tokenLock =
            new ReentrantLock();

    private volatile String cachedAccessToken;

    private volatile Instant tokenExpiresAt =
            Instant.EPOCH;


    // =========================================================
    // SYNC LULC
    // =========================================================

    @Transactional
    public LulcResponse syncFarm(UUID farmId) {

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Farm not found with ID: " + farmId
                        )
                );

        validateFarmCoordinates(farm);

        validateCredentials();

        String accessToken = getAccessToken();

        Map<String, Object> request =
                buildStatisticsRequest(farm);

        String responseBody;

        try {

            responseBody = restClient.post()
                    .uri(properties.getStatisticsUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(
                            "Authorization",
                            "Bearer " + accessToken
                    )
                    .body(request)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (requestInfo, response) -> {

                                String errorBody = "";

                                try {
                                    errorBody = new String(
                                            response.getBody()
                                                    .readAllBytes()
                                    );
                                } catch (Exception ignored) {
                                    // Keep the status message if
                                    // response body cannot be read.
                                }

                                if (errorBody.length() > 2000) {
                                    errorBody =
                                            errorBody.substring(0, 2000);
                                }

                                throw new IllegalStateException(
                                        "Copernicus Statistical API request failed. "
                                                + "HTTP "
                                                + response.getStatusCode().value()
                                                + ". "
                                                + (
                                                errorBody.isBlank()
                                                        ? "No error body returned."
                                                        : "Response: "
                                                        + errorBody
                                        )
                                );
                            }
                    )
                    .body(String.class);

        } catch (IllegalStateException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to call Copernicus Statistical API: "
                            + exception.getMessage(),
                    exception
            );
        }

        if (responseBody == null ||
                responseBody.isBlank()) {

            throw new IllegalStateException(
                    "Copernicus Statistical API returned an empty response."
            );
        }

        try {

            JsonNode root =
                    objectMapper.readTree(responseBody);

            validateCopernicusResponse(root);

            JsonNode outputs =
                    extractOutputs(root);

            // -------------------------------------------------
            // READ LULC CLASS PERCENTAGES
            // -------------------------------------------------

            double tree =
                    percent(outputs, "tree");

            double shrubland =
                    percent(outputs, "shrubland");

            double grassland =
                    percent(outputs, "grassland");

            double cropland =
                    percent(outputs, "cropland");

            double wetland =
                    percent(outputs, "wetland");

            double mangrove =
                    percent(outputs, "mangrove");

            double moss =
                    percent(outputs, "moss");

            double bare =
                    percent(outputs, "bare");

            double builtup =
                    percent(outputs, "builtup");

            double water =
                    percent(outputs, "water");

            double snow =
                    percent(outputs, "snow");

            double unclassified =
                    percent(outputs, "unclassified");

            double classified =
                    percent(outputs, "classified");

            // -------------------------------------------------
            // NATURAL VEGETATION
            // -------------------------------------------------

            double naturalVegetation =
                    sum(
                            tree,
                            shrubland,
                            grassland,
                            mangrove,
                            moss
                    );

            // -------------------------------------------------
            // BUILD OBSERVATION
            // -------------------------------------------------

            LulcObservation observation =
                    LulcObservation.builder()

                            .farm(farm)

                            .dataSource(DATA_SOURCE)

                            .productName(PRODUCT_NAME)

                            .collectionId(
                                    properties.getCollectionId()
                            )

                            .baseYear(
                                    properties.getBaseYear()
                            )

                            .spatialResolutionMeters(
                                    properties
                                            .getSpatialResolutionMeters()
                            )

                            .aoiRadiusMeters(
                                    properties
                                            .getAoiRadiusMeters()
                            )

                            .treeCoverPercent(tree)

                            .shrublandPercent(shrubland)

                            .grasslandPercent(grassland)

                            .croplandPercent(cropland)

                            .herbaceousWetlandPercent(
                                    wetland
                            )

                            .mangrovePercent(mangrove)

                            .mossLichenPercent(moss)

                            .bareSparseVegetationPercent(
                                    bare
                            )

                            .builtUpPercent(builtup)

                            .waterPercent(water)

                            .snowIcePercent(snow)

                            .unclassifiedPercent(
                                    unclassified
                            )

                            .classifiedCoveragePercent(
                                    classified
                            )

                            .naturalVegetationPercent(
                                    naturalVegetation
                            )

                            .analysisNote(
                                    buildAnalysisNote()
                            )

                            .analyzedAt(
                                    LocalDateTime.now()
                            )

                            .build();

            // -------------------------------------------------
            // DOMINANT CLASS
            // -------------------------------------------------

            observation.setDominantClass(
                    determineDominantClass(observation)
            );

            // -------------------------------------------------
            // SAVE
            // -------------------------------------------------

            LulcObservation saved =
                    repository.save(observation);

            return toResponse(saved);

        } catch (Exception exception) {

            if (exception instanceof IllegalStateException) {

                throw (IllegalStateException)
                        exception;
            }

            throw new IllegalStateException(
                    "Unable to parse the Copernicus LULC response: "
                            + exception.getMessage(),
                    exception
            );
        }
    }


    // =========================================================
    // LATEST
    // =========================================================

    @Transactional(readOnly = true)
    public LulcResponse getLatest(UUID farmId) {

        return repository
                .findFirstByFarmIdOrderByAnalyzedAtDesc(farmId)
                .map(this::toResponse)
                .orElse(null);
    }


    // =========================================================
    // HISTORY
    // =========================================================

    @Transactional(readOnly = true)
    public List<LulcResponse> getHistory(UUID farmId) {

        return repository
                .findByFarmIdOrderByAnalyzedAtDesc(farmId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // =========================================================
    // COPERNICUS AUTHENTICATION
    // =========================================================

    private String getAccessToken() {

        Instant now = Instant.now();

        if (cachedAccessToken != null &&
                now.isBefore(
                        tokenExpiresAt.minusSeconds(60)
                )) {

            return cachedAccessToken;
        }

        tokenLock.lock();

        try {

            now = Instant.now();

            if (cachedAccessToken != null &&
                    now.isBefore(
                            tokenExpiresAt.minusSeconds(60)
                    )) {

                return cachedAccessToken;
            }

            Map<String, String> form =
                    new LinkedHashMap<>();

            form.put(
                    "grant_type",
                    "client_credentials"
            );

            form.put(
                    "client_id",
                    properties.getClientId()
            );

            form.put(
                    "client_secret",
                    properties.getClientSecret()
            );

            String formBody =
                    buildFormBody(form);

            String responseBody =
                    restClient.post()

                            .uri(
                                    properties
                                            .getIdentityUrl()
                            )

                            .contentType(
                                    MediaType
                                            .APPLICATION_FORM_URLENCODED
                            )

                            .accept(
                                    MediaType.APPLICATION_JSON
                            )

                            .body(formBody)

                            .retrieve()

                            .onStatus(
                                    HttpStatusCode::isError,
                                    (requestInfo, response) -> {

                                        String errorBody = "";

                                        try {
                                            errorBody =
                                                    new String(
                                                            response
                                                                    .getBody()
                                                                    .readAllBytes()
                                                    );
                                        } catch (Exception ignored) {
                                            // Keep status only.
                                        }

                                        if (errorBody.length() > 1000) {
                                            errorBody =
                                                    errorBody.substring(0, 1000);
                                        }

                                        throw new IllegalStateException(
                                                "Copernicus OAuth request failed. "
                                                        + "HTTP "
                                                        + response
                                                        .getStatusCode()
                                                        .value()
                                                        + ". "
                                                        + (
                                                        errorBody.isBlank()
                                                                ? ""
                                                                : "Response: "
                                                                + errorBody
                                                )
                                        );
                                    }
                            )

                            .body(String.class);

            if (responseBody == null ||
                    responseBody.isBlank()) {

                throw new IllegalStateException(
                        "Copernicus OAuth returned an empty response."
                );
            }

            JsonNode root =
                    objectMapper.readTree(
                            responseBody
                    );

            String token =
                    root.path(
                            "access_token"
                    ).asText(null);

            long expiresIn =
                    root.path(
                            "expires_in"
                    ).asLong(1800);

            if (token == null ||
                    token.isBlank()) {

                throw new IllegalStateException(
                        "Copernicus OAuth response did not contain access_token."
                );
            }

            cachedAccessToken = token;

            tokenExpiresAt =
                    Instant.now()
                            .plusSeconds(
                                    Math.max(
                                            60,
                                            expiresIn
                                    )
                            );

            return cachedAccessToken;

        } catch (Exception exception) {

            if (exception instanceof IllegalStateException) {

                throw (IllegalStateException)
                        exception;
            }

            throw new IllegalStateException(
                    "Unable to authenticate with Copernicus: "
                            + exception.getMessage(),
                    exception
            );

        } finally {

            tokenLock.unlock();
        }
    }


    // =========================================================
    // STATISTICS REQUEST
    // =========================================================

    private Map<String, Object> buildStatisticsRequest(
            Farm farm
    ) {

        double radius =
                properties.getAoiRadiusMeters();

        double latitude =
                farm.getLatitude();

        double longitude =
                farm.getLongitude();

        double latitudeDelta =
                radius / 111_320.0;

        double cosLatitude =
                Math.cos(
                        Math.toRadians(latitude)
                );

        if (Math.abs(cosLatitude) < 0.000001) {

            throw new IllegalStateException(
                    "Farm longitude calculation is invalid near the geographic poles."
            );
        }

        double longitudeDelta =
                radius /
                        (111_320.0 * cosLatitude);

        double west =
                longitude - longitudeDelta;

        double south =
                latitude - latitudeDelta;

        double east =
                longitude + longitudeDelta;

        double north =
                latitude + latitudeDelta;


        // -----------------------------------------------------
        // BOUNDS
        // -----------------------------------------------------

        Map<String, Object> bounds =
                new LinkedHashMap<>();

        bounds.put(
                "bbox",
                List.of(
                        west,
                        south,
                        east,
                        north
                )
        );

        Map<String, Object> boundsProperties =
                new LinkedHashMap<>();

        boundsProperties.put(
                "crs",
                CRS84
        );

        bounds.put(
                "properties",
                boundsProperties
        );


        // -----------------------------------------------------
        // TIME RANGE
        // -----------------------------------------------------

        /*
         * The LULC product is the 2020 annual product.
         *
         * Use an exclusive end boundary:
         *
         * 2020-01-01 -> 2021-01-01
         */
        int baseYear =
                properties.getBaseYear();

        String from =
                baseYear
                        + "-01-01T00:00:00Z";

        String to =
                (baseYear + 1)
                        + "-01-01T00:00:00Z";

        Map<String, Object> timeRange =
                new LinkedHashMap<>();

        timeRange.put(
                "from",
                from
        );

        timeRange.put(
                "to",
                to
        );


        // -----------------------------------------------------
        // DATA
        // -----------------------------------------------------

        Map<String, Object> dataFilter =
                new LinkedHashMap<>();

        dataFilter.put(
                "timeRange",
                timeRange
        );

        Map<String, Object> dataItem =
                new LinkedHashMap<>();

        dataItem.put(
                "type",
                properties.getDataType()
        );

        /*
         * Important for the BYOC LULC collection.
         *
         * This explicitly restricts the requested data item
         * to the same 2020 time range used by aggregation.
         */
        dataItem.put(
                "dataFilter",
                dataFilter
        );

        Map<String, Object> input =
                new LinkedHashMap<>();

        input.put(
                "bounds",
                bounds
        );

        input.put(
                "data",
                List.of(dataItem)
        );


        // -----------------------------------------------------
        // AGGREGATION
        // -----------------------------------------------------

        Map<String, Object> aggregation =
                new LinkedHashMap<>();

        aggregation.put(
                "timeRange",
                timeRange
        );

        Map<String, Object> interval =
                new LinkedHashMap<>();

        interval.put(
                "of",
                "P1Y"
        );

        aggregation.put(
                "aggregationInterval",
                interval
        );

        aggregation.put(
                "evalscript",
                buildEvalscript()
        );

        aggregation.put(
                "resx",
                properties.getSpatialResolutionMeters()
        );

        aggregation.put(
                "resy",
                properties.getSpatialResolutionMeters()
        );


        // -----------------------------------------------------
        // FINAL REQUEST
        // -----------------------------------------------------

        Map<String, Object> request =
                new LinkedHashMap<>();

        request.put(
                "input",
                input
        );

        request.put(
                "aggregation",
                aggregation
        );

        return request;
    }


    // =========================================================
    // EVALSCRIPT
    // =========================================================

    private String buildEvalscript() {

        return """
                //VERSION=3

                function setup() {

                    return {

                        input: [
                            {
                                bands: [
                                    "LCM10",
                                    "dataMask"
                                ]
                            }
                        ],

                        output: [

                            {
                                id: "tree",
                                bands: 1,
                                sampleType: "FLOAT32"
                            },

                            {
                                id: "shrubland",
                                bands: 1,
                                sampleType: "FLOAT32"
                            },

                            {
                                id: "grassland",
                                bands: 1,
                                sampleType: "FLOAT32"
                            },

                            {
                                id: "cropland",
                                bands: 1,
                                sampleType: "FLOAT32"
                            },

                            {
                                id: "wetland",
                                bands: 1,
                                sampleType: "FLOAT32"
                            },

                            {
                                id: "mangrove",
                                bands: 1,
                                sampleType: "FLOAT32"
                            },

                            {
                                id: "moss",
                                bands: 1,
                                sampleType: "FLOAT32"
                            },

                            {
                                id: "bare",
                                bands: 1,
                                sampleType: "FLOAT32"
                            },

                            {
                                id: "builtup",
                                bands: 1,
                                sampleType: "FLOAT32"
                            },

                            {
                                id: "water",
                                bands: 1,
                                sampleType: "FLOAT32"
                            },

                            {
                                id: "snow",
                                bands: 1,
                                sampleType: "FLOAT32"
                            },

                            {
                                id: "unclassified",
                                bands: 1,
                                sampleType: "FLOAT32"
                            },

                            {
                                id: "classified",
                                bands: 1,
                                sampleType: "FLOAT32"
                            },

                            {
                                id: "dataMask",
                                bands: 1
                            }
                        ]
                    };
                }


                function evaluatePixel(sample) {

                    const value =
                            sample.LCM10;

                    return {

                        tree: [
                            value === 10 ? 1 : 0
                        ],

                        shrubland: [
                            value === 20 ? 1 : 0
                        ],

                        grassland: [
                            value === 30 ? 1 : 0
                        ],

                        cropland: [
                            value === 40 ? 1 : 0
                        ],

                        wetland: [
                            value === 50 ? 1 : 0
                        ],

                        mangrove: [
                            value === 60 ? 1 : 0
                        ],

                        moss: [
                            value === 70 ? 1 : 0
                        ],

                        bare: [
                            value === 80 ? 1 : 0
                        ],

                        builtup: [
                            value === 90 ? 1 : 0
                        ],

                        water: [
                            value === 100 ? 1 : 0
                        ],

                        snow: [
                            value === 110 ? 1 : 0
                        ],

                        unclassified: [
                            value === 254 ? 1 : 0
                        ],

                        classified: [
                            sample.dataMask === 1 &&
                            value !== 254
                                ? 1
                                : 0
                        ],

                        dataMask: [
                            sample.dataMask
                        ]
                    };
                }
                """;
    }


    // =========================================================
    // COPERNICUS JSON PARSER
    // =========================================================

    private JsonNode extractOutputs(
            JsonNode root
    ) {

        JsonNode data =
                root.path("data");

        if (!data.isArray() ||
                data.isEmpty()) {

            String rawResponse =
                    root.toString();

            if (rawResponse.length() > 2000) {

                rawResponse =
                        rawResponse.substring(
                                0,
                                2000
                        );
            }

            throw new IllegalStateException(
                    "Copernicus Statistical API returned no statistical interval. "
                            + "Response: "
                            + rawResponse
            );
        }

        JsonNode firstInterval =
                data.get(0);

        JsonNode outputs =
                firstInterval.path(
                        "outputs"
                );

        if (!outputs.isObject()) {

            throw new IllegalStateException(
                    "Copernicus response contains no outputs object."
            );
        }

        return outputs;
    }


    // =========================================================
    // STATISTICS VALUE
    // =========================================================

    private Double percent(
            JsonNode outputs,
            String outputName
    ) {

        JsonNode output =
                outputs.path(outputName);

        if (output.isMissingNode()) {

            return 0.0;
        }

        JsonNode bands =
                output.path("bands");

        if (!bands.isObject()) {

            return 0.0;
        }

        JsonNode band =
                bands.path("B0");

        if (!band.isObject()) {

            /*
             * Some responses may use another
             * band key. Try the first available band.
             */

            var fields =
                    bands.fields();

            if (fields.hasNext()) {

                band =
                        fields.next().getValue();

            } else {

                return 0.0;
            }
        }

        JsonNode stats =
                band.path("stats");

        if (!stats.isObject()) {

            return 0.0;
        }

        JsonNode mean =
                stats.path("mean");

        if (!mean.isNumber()) {

            return 0.0;
        }

        double value =
                mean.asDouble();

        if (Double.isNaN(value) ||
                Double.isInfinite(value)) {

            return 0.0;
        }

        /*
         * Binary evalscript output has a mean
         * between 0 and 1.
         *
         * Example:
         *
         * 0.42 -> 42%
         */

        return round(
                value * 100.0
        );
    }


    // =========================================================
    // VALIDATE COPERNICUS RESPONSE
    // =========================================================

    private void validateCopernicusResponse(
            JsonNode root
    ) {

        String status =
                root.path("status")
                        .asText("");

        if (!"OK".equalsIgnoreCase(status)) {

            String error =
                    root.path("error")
                            .asText("");

            String details =
                    root.path("errorDescription")
                            .asText("");

            String message =
                    "Copernicus Statistical API did not return OK.";

            if (!error.isBlank()) {

                message +=
                        " Error: " + error;
            }

            if (!details.isBlank()) {

                message +=
                        " Details: " + details;
            }

            throw new IllegalStateException(
                    message
            );
        }

        JsonNode data =
                root.path("data");

        if (!data.isArray() ||
                data.isEmpty()) {

            String rawResponse =
                    root.toString();

            if (rawResponse.length() > 2000) {

                rawResponse =
                        rawResponse.substring(
                                0,
                                2000
                        );
            }

            throw new IllegalStateException(
                    "Copernicus Statistical API returned no statistical interval. "
                            + "Response: "
                            + rawResponse
            );
        }
    }


    // =========================================================
    // DOMINANT CLASS
    // =========================================================

    private String determineDominantClass(
            LulcObservation observation
    ) {

        Map<String, Double> values =
                new LinkedHashMap<>();

        values.put(
                "Tree cover",
                safe(
                        observation
                                .getTreeCoverPercent()
                )
        );

        values.put(
                "Shrubland",
                safe(
                        observation
                                .getShrublandPercent()
                )
        );

        values.put(
                "Grassland",
                safe(
                        observation
                                .getGrasslandPercent()
                )
        );

        values.put(
                "Cropland",
                safe(
                        observation
                                .getCroplandPercent()
                )
        );

        values.put(
                "Herbaceous wetland",
                safe(
                        observation
                                .getHerbaceousWetlandPercent()
                )
        );

        values.put(
                "Mangroves",
                safe(
                        observation
                                .getMangrovePercent()
                )
        );

        values.put(
                "Moss and lichen",
                safe(
                        observation
                                .getMossLichenPercent()
                )
        );

        values.put(
                "Bare / sparse vegetation",
                safe(
                        observation
                                .getBareSparseVegetationPercent()
                )
        );

        values.put(
                "Built-up",
                safe(
                        observation
                                .getBuiltUpPercent()
                )
        );

        values.put(
                "Permanent water bodies",
                safe(
                        observation
                                .getWaterPercent()
                )
        );

        values.put(
                "Snow and ice",
                safe(
                        observation
                                .getSnowIcePercent()
                )
        );

        values.put(
                "Unclassifiable",
                safe(
                        observation
                                .getUnclassifiedPercent()
                )
        );

        return values.entrySet()
                .stream()
                .max(
                        Map.Entry.comparingByValue(
                                Comparator.naturalOrder()
                        )
                )
                .map(
                        Map.Entry::getKey
                )
                .orElse("Unknown");
    }


    // =========================================================
    // SUM
    // =========================================================

    private double sum(
            Double... values
    ) {

        double total = 0.0;

        for (Double value : values) {

            total += safe(value);
        }

        return round(total);
    }


    // =========================================================
    // SAFE DOUBLE
    // =========================================================

    private double safe(
            Double value
    ) {

        return value == null
                ? 0.0
                : value;
    }


    // =========================================================
    // ROUND
    // =========================================================

    private double round(
            double value
    ) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }


    // =========================================================
    // FORM BODY
    // =========================================================

    private String buildFormBody(
            Map<String, String> values
    ) {

        return values.entrySet()
                .stream()
                .map(
                        entry ->
                                encode(
                                        entry.getKey()
                                )
                                        +
                                        "="
                                        +
                                        encode(
                                                entry.getValue()
                                        )
                )
                .reduce(
                        (first, second) ->
                                first + "&" + second
                )
                .orElse("");
    }


    private String encode(
            String value
    ) {

        return java.net.URLEncoder.encode(
                value,
                java.nio.charset.StandardCharsets.UTF_8
        );
    }


    // =========================================================
    // ANALYSIS NOTE
    // =========================================================

    private String buildAnalysisNote() {

        return "Source: Copernicus Data Space Ecosystem / "
                + "CLMS. This is the documented "
                + properties.getBaseYear()
                + " base-year 10 m land-cover map, "
                + "not live 2026 land cover. "
                + "Percentages describe the farm-area AOI "
                + "and should be treated as spatial context, "
                + "not crop diagnosis.";
    }


    // =========================================================
    // FARM VALIDATION
    // =========================================================

    private void validateFarmCoordinates(
            Farm farm
    ) {

        if (farm.getLatitude() == null ||
                farm.getLongitude() == null) {

            throw new IllegalStateException(
                    "Farm latitude and longitude are required for LULC mapping."
            );
        }

        if (farm.getLatitude() < -90 ||
                farm.getLatitude() > 90 ||
                farm.getLongitude() < -180 ||
                farm.getLongitude() > 180) {

            throw new IllegalStateException(
                    "Farm latitude/longitude are outside valid geographic ranges."
            );
        }
    }


    // =========================================================
    // CREDENTIAL VALIDATION
    // =========================================================

    private void validateCredentials() {

        if (properties.getClientId() == null ||
                properties.getClientId().isBlank()) {

            throw new IllegalStateException(
                    "Copernicus Client ID is not configured. "
                            + "Set COPERNICUS_CLIENT_ID."
            );
        }

        if (properties.getClientSecret() == null ||
                properties.getClientSecret().isBlank()) {

            throw new IllegalStateException(
                    "Copernicus Client Secret is not configured. "
                            + "Set COPERNICUS_CLIENT_SECRET."
            );
        }
    }


    // =========================================================
    // ENTITY -> RESPONSE
    // =========================================================

    private LulcResponse toResponse(
            LulcObservation observation
    ) {

        Farm farm =
                observation.getFarm();

        return LulcResponse.builder()

                .id(
                        observation.getId()
                )

                .farmId(
                        farm.getId()
                )

                .farmName(
                        farm.getName()
                )

                .location(
                        farm.getLocation()
                )

                .latitude(
                        farm.getLatitude()
                )

                .longitude(
                        farm.getLongitude()
                )

                .dataSource(
                        observation.getDataSource()
                )

                .productName(
                        observation.getProductName()
                )

                .collectionId(
                        observation.getCollectionId()
                )

                .baseYear(
                        observation.getBaseYear()
                )

                .spatialResolutionMeters(
                        observation
                                .getSpatialResolutionMeters()
                )

                .aoiRadiusMeters(
                        observation
                                .getAoiRadiusMeters()
                )

                .treeCoverPercent(
                        observation
                                .getTreeCoverPercent()
                )

                .shrublandPercent(
                        observation
                                .getShrublandPercent()
                )

                .grasslandPercent(
                        observation
                                .getGrasslandPercent()
                )

                .croplandPercent(
                        observation
                                .getCroplandPercent()
                )

                .herbaceousWetlandPercent(
                        observation
                                .getHerbaceousWetlandPercent()
                )

                .mangrovePercent(
                        observation
                                .getMangrovePercent()
                )

                .mossLichenPercent(
                        observation
                                .getMossLichenPercent()
                )

                .bareSparseVegetationPercent(
                        observation
                                .getBareSparseVegetationPercent()
                )

                .builtUpPercent(
                        observation
                                .getBuiltUpPercent()
                )

                .waterPercent(
                        observation
                                .getWaterPercent()
                )

                .snowIcePercent(
                        observation
                                .getSnowIcePercent()
                )

                .unclassifiedPercent(
                        observation
                                .getUnclassifiedPercent()
                )

                .dominantClass(
                        observation
                                .getDominantClass()
                )

                .naturalVegetationPercent(
                        observation
                                .getNaturalVegetationPercent()
                )

                .classifiedCoveragePercent(
                        observation
                                .getClassifiedCoveragePercent()
                )

                .analysisNote(
                        observation
                                .getAnalysisNote()
                )

                .analyzedAt(
                        observation
                                .getAnalyzedAt()
                )

                .build();
    }
}