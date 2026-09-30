package com.agrinexus.service;

import com.agrinexus.config.Sentinel2Properties;
import com.agrinexus.entity.Farm;
import com.agrinexus.entity.SatelliteObservation;
import com.agrinexus.repository.FarmRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CopernicusSentinel2Service {

    private final FarmRepository farmRepository;

    private final SatelliteObservationService observationService;

    private final Sentinel2Properties properties;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final RestClient restClient =
            RestClient.builder().build();

    /*
     * Cached OAuth token.
     */
    private String cachedAccessToken;

    private Instant tokenExpiresAt;


    /*
     * -------------------------------------------------------------
     * Synchronize latest real Sentinel-2 observation.
     *
     * POST
     * /api/satellite-observations/farm/{farmId}/sync
     * -------------------------------------------------------------
     */
    public SatelliteObservation syncFarm(
            UUID farmId
    ) {

        Farm farm =
                farmRepository.findById(
                        farmId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Farm not found with ID: "
                                        + farmId
                        )
                );


        /*
         * Farm coordinates are mandatory.
         */
        if (farm.getLatitude() == null
                || farm.getLongitude() == null) {

            throw new RuntimeException(
                    "INSUFFICIENT_DATA: Farm does not have valid "
                            + "latitude and longitude."
            );
        }


        /*
         * Get Copernicus OAuth token.
         */
        String token =
                getAccessToken();


        /*
         * Find latest suitable real Sentinel-2 scene.
         */
        CatalogScene scene =
                findLatestSuitableScene(
                        farm,
                        token
                );


        if (scene == null) {

            throw new RuntimeException(
                    "INSUFFICIENT_DATA: No suitable Sentinel-2 L2A "
                            + "observation was found for this farm "
                            + "within the configured period."
            );
        }


        /*
         * Calculate real satellite metrics.
         */
        SatelliteMetrics metrics =
                calculateSatelliteMetrics(
                        farm,
                        scene,
                        token
                );


        /*
         * NDVI and NDWI are required.
         */
        if (metrics.ndvi() == null
                || metrics.ndwi() == null) {

            throw new RuntimeException(
                    "INSUFFICIENT_DATA: Copernicus returned no valid "
                            + "NDVI/NDWI statistics for the selected "
                            + "observation."
            );
        }


        /*
         * Calculate confidence from:
         *
         * 1. Scene cloud coverage
         * 2. Valid pixel coverage
         */
        Double confidence =
                calculateConfidence(
                        scene.cloudCover(),
                        metrics.validCoveragePercent()
                );


        /*
         * Send real Copernicus data into the existing
         * SatelliteObservationService.
         *
         * No synthetic values are created here.
         */
        return observationService.create(
                farmId,
                null,
                scene.observedAt(),
                "Sentinel-2 L2A",
                scene.sceneId(),
                metrics.ndvi(),
                metrics.ndwi(),
                scene.cloudCover(),
                properties.getSpatialResolutionMeters(),
                "OBSERVED",
                "COPERNICUS",
                confidence,
                false
        );
    }


    /*
     * -------------------------------------------------------------
     * Find latest suitable Sentinel-2 scene.
     * -------------------------------------------------------------
     */
    private CatalogScene findLatestSuitableScene(
            Farm farm,
            String token
    ) {

        LocalDate today =
                LocalDate.now(
                        ZoneOffset.UTC
                );


        LocalDate startDate =
                today.minusDays(
                        properties.getLookbackDays()
                );


        String datetime =
                startDate
                        + "T00:00:00Z/"
                        + today
                        + "T23:59:59Z";


        /*
         * Build farm-centered bounding box.
         */
        double radius =
                properties.getAoiRadiusMeters();

        double latitude =
                farm.getLatitude();

        double longitude =
                farm.getLongitude();


        double latitudeDelta =
                radius / 111_320.0;


        double longitudeCos =
                Math.cos(
                        Math.toRadians(
                                latitude
                        )
                );


        /*
         * Avoid division by zero for extreme latitudes.
         */
        if (Math.abs(longitudeCos) < 0.000001) {

            throw new RuntimeException(
                    "Invalid farm latitude for satellite AOI."
            );
        }


        double longitudeDelta =
                radius
                        / (
                        111_320.0
                                * longitudeCos
                );


        double minLon =
                longitude
                        - longitudeDelta;

        double minLat =
                latitude
                        - latitudeDelta;

        double maxLon =
                longitude
                        + longitudeDelta;

        double maxLat =
                latitude
                        + latitudeDelta;


        /*
         * Catalog search request.
         */
        Map<String, Object> request =
                new LinkedHashMap<>();


        request.put(
                "collections",
                List.of(
                        properties.getCollection()
                )
        );


        request.put(
                "datetime",
                datetime
        );


        request.put(
                "bbox",
                List.of(
                        minLon,
                        minLat,
                        maxLon,
                        maxLat
                )
        );


        request.put(
                "limit",
                20
        );


        /*
         * Ask Catalog API for required metadata.
         */
        Map<String, Object> fields =
                new LinkedHashMap<>();


        fields.put(
                "include",
                List.of(
                        "id",
                        "properties.datetime",
                        "properties.eo:cloud_cover",
                        "properties.platform",
                        "properties.processing:baseline"
                )
        );


        request.put(
                "fields",
                fields
        );


        String response;

        try {

            response =
                    restClient
                            .post()
                            .uri(
                                    properties.getBaseUrl()
                                            + "/catalog/v1/search"
                            )
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .accept(
                                    MediaType.APPLICATION_JSON
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + token
                            )
                            .body(request)
                            .retrieve()
                            .body(String.class);

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Copernicus Catalog API request failed: "
                            + exception.getMessage(),
                    exception
            );
        }


        try {

            JsonNode root =
                    objectMapper.readTree(
                            response
                    );


            JsonNode features =
                    root.path(
                            "features"
                    );


            if (!features.isArray()
                    || features.isEmpty()) {

                return null;
            }


            List<CatalogScene> scenes =
                    new ArrayList<>();


            for (JsonNode feature : features) {

                String sceneId =
                        textValue(
                                feature.path(
                                        "id"
                                )
                        );


                JsonNode propertiesNode =
                        feature.path(
                                "properties"
                        );


                String dateTime =
                        textValue(
                                propertiesNode.path(
                                        "datetime"
                                )
                        );


                Double cloudCover =
                        doubleValue(
                                propertiesNode.path(
                                        "eo:cloud_cover"
                                )
                        );


                if (sceneId == null
                        || dateTime == null) {

                    continue;
                }


                /*
                 * Reject scenes whose cloud coverage is above
                 * configured maximum.
                 */
                if (cloudCover != null
                        && cloudCover
                        > properties.getMaxCloudCoverage()) {

                    continue;
                }


                LocalDateTime observedAt;

                try {

                    observedAt =
                            Instant.parse(
                                            dateTime
                                    )
                                    .atOffset(
                                            ZoneOffset.UTC
                                    )
                                    .toLocalDateTime();

                } catch (Exception exception) {

                    continue;
                }


                scenes.add(
                        new CatalogScene(
                                sceneId,
                                observedAt,
                                cloudCover
                        )
                );
            }


            /*
             * Newest observation first.
             *
             * If dates are equal, lower cloud cover wins.
             */
            scenes.sort(
                    Comparator
                            .comparing(
                                    CatalogScene::observedAt
                            )
                            .reversed()
                            .thenComparing(
                                    scene ->
                                            scene.cloudCover()
                                                    == null
                                                    ? Double.MAX_VALUE
                                                    : scene.cloudCover()
                            )
            );


            return scenes.isEmpty()
                    ? null
                    : scenes.get(0);


        } catch (Exception exception) {

            throw new RuntimeException(
                    "Failed to parse Copernicus Catalog response.",
                    exception
            );
        }
    }


    /*
     * -------------------------------------------------------------
     * Calculate NDVI / NDWI / valid coverage.
     * -------------------------------------------------------------
     */
    private SatelliteMetrics calculateSatelliteMetrics(
            Farm farm,
            CatalogScene scene,
            String token
    ) {

        double radius =
                properties.getAoiRadiusMeters();


        double latitude =
                farm.getLatitude();


        double longitude =
                farm.getLongitude();


        double latitudeDelta =
                radius / 111_320.0;


        double longitudeCos =
                Math.cos(
                        Math.toRadians(
                                latitude
                        )
                );


        if (Math.abs(longitudeCos) < 0.000001) {

            throw new RuntimeException(
                    "Invalid farm latitude for satellite AOI."
            );
        }


        double longitudeDelta =
                radius
                        / (
                        111_320.0
                                * longitudeCos
                );


        List<Double> bbox =
                List.of(
                        longitude - longitudeDelta,
                        latitude - latitudeDelta,
                        longitude + longitudeDelta,
                        latitude + latitudeDelta
                );


        /*
         * Build Statistical API evalscript.
         */
        String evalscript =
                buildEvalscript();


        Map<String, Object> request =
                new LinkedHashMap<>();


        /*
         * ---------------------------------------------------------
         * INPUT
         * ---------------------------------------------------------
         */
        Map<String, Object> input =
                new LinkedHashMap<>();


        Map<String, Object> bounds =
                new LinkedHashMap<>();


        bounds.put(
                "bbox",
                bbox
        );


        bounds.put(
                "properties",
                Map.of(
                        "crs",
                        "http://www.opengis.net/def/crs/OGC/1.3/CRS84"
                )
        );


        input.put(
                "bounds",
                bounds
        );


        /*
         * Sentinel-2 data item.
         */
        Map<String, Object> dataItem =
                new LinkedHashMap<>();


        dataItem.put(
                "type",
                properties.getCollection()
        );


        Map<String, Object> dataFilter =
                new LinkedHashMap<>();


        /*
         * The Catalog-selected scene timestamp is used as a
         * narrow time window.
         */
        Map<String, String> timeRange =
                new LinkedHashMap<>();


        String from =
                scene.observedAt()
                        .minusMinutes(5)
                        .toInstant(
                                ZoneOffset.UTC
                        )
                        .toString();


        String to =
                scene.observedAt()
                        .plusMinutes(5)
                        .toInstant(
                                ZoneOffset.UTC
                        )
                        .toString();


        timeRange.put(
                "from",
                from
        );


        timeRange.put(
                "to",
                to
        );


        dataFilter.put(
                "timeRange",
                timeRange
        );


        /*
         * Prefer the most recent acquisition inside this
         * narrow interval.
         */
        dataFilter.put(
                "mosaickingOrder",
                "mostRecent"
        );


        dataItem.put(
                "dataFilter",
                dataFilter
        );


        input.put(
                "data",
                List.of(
                        dataItem
                )
        );


        request.put(
                "input",
                input
        );


        /*
         * ---------------------------------------------------------
         * AGGREGATION
         *
         * IMPORTANT:
         * Copernicus Statistical API expects evalscript inside
         * aggregation.
         * ---------------------------------------------------------
         */
        Map<String, Object> aggregation =
                new LinkedHashMap<>();


        aggregation.put(
                "timeRange",
                timeRange
        );


        aggregation.put(
                "aggregationInterval",
                Map.of(
                        "of",
                        "P1D"
                )
        );


        aggregation.put(
                "resx",
                properties.getSpatialResolutionMeters()
        );


        aggregation.put(
                "resy",
                properties.getSpatialResolutionMeters()
        );


        /*
         * IMPORTANT:
         * evalscript belongs inside aggregation.
         */
        aggregation.put(
                "evalscript",
                evalscript
        );


        request.put(
                "aggregation",
                aggregation
        );


        String response;

        try {

            response =
                    restClient
                            .post()
                            .uri(
                                    properties.getBaseUrl()
                                            + "/statistics/v1"
                            )
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .accept(
                                    MediaType.APPLICATION_JSON
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + token
                            )
                            .body(request)
                            .retrieve()
                            .body(String.class);

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Copernicus Statistical API request failed: "
                            + exception.getMessage(),
                    exception
            );
        }


        try {

            JsonNode root =
                    objectMapper.readTree(
                            response
                    );


            /*
             * Copernicus returns:
             *
             * {
             *   "data": [
             *      {
             *          "interval": {...},
             *          "outputs": {...}
             *      }
             *   ],
             *   "status": "OK"
             * }
             */
            JsonNode interval =
                    root.path(
                            "data"
                    );


            if (!interval.isArray()
                    || interval.isEmpty()) {

                return new SatelliteMetrics(
                        null,
                        null,
                        null
                );
            }


            JsonNode first =
                    interval.get(0);


            JsonNode outputs =
                    first.path(
                            "outputs"
                    );


            /*
             * Extract NDVI.
             */
            Double ndvi =
                    extractMean(
                            outputs,
                            "ndvi"
                    );


            /*
             * Extract NDWI.
             */
            Double ndwi =
                    extractMean(
                            outputs,
                            "ndwi"
                    );


            /*
             * dataMask statistics are used to determine how much
             * of the requested AOI contained usable source data.
             */
            Double validCoveragePercent =
                    calculateValidCoveragePercent(
                            outputs
                    );


            return new SatelliteMetrics(
                    ndvi,
                    ndwi,
                    validCoveragePercent
            );


        } catch (Exception exception) {

            throw new RuntimeException(
                    "Failed to parse Copernicus Statistical API response.",
                    exception
            );
        }
    }


    /*
     * -------------------------------------------------------------
     * Sentinel Hub Statistical API evalscript.
     *
     * NDVI:
     *
     * (B08 - B04)
     * ------------
     * (B08 + B04)
     *
     *
     * NDWI:
     *
     * (B03 - B11)
     * ------------
     * (B03 + B11)
     *
     * -------------------------------------------------------------
     *
     * IMPORTANT:
     *
     * Statistical API requires a dataMask output.
     *
     * We use:
     *
     * dataMask = 0 -> exclude pixel
     * dataMask = 1 -> include pixel
     *
     * -------------------------------------------------------------
     */
    private String buildEvalscript() {

        return """
                //VERSION=3

                function setup() {

                    return {

                        input: [
                            "B03",
                            "B04",
                            "B08",
                            "B11",
                            "dataMask"
                        ],

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
                                id: "dataMask",
                                bands: 1
                            }
                        ]
                    };
                }


                function evaluatePixel(sample) {

                    var denominatorNdvi =
                        sample.B08 + sample.B04;

                    var denominatorNdwi =
                        sample.B03 + sample.B11;


                    var ndvi = 0;

                    var ndwi = 0;


                    if (denominatorNdvi !== 0) {

                        ndvi =
                            (sample.B08 - sample.B04)
                            / denominatorNdvi;
                    }


                    if (denominatorNdwi !== 0) {

                        ndwi =
                            (sample.B03 - sample.B11)
                            / denominatorNdwi;
                    }


                    return {

                        ndvi: [ndvi],

                        ndwi: [ndwi],

                        dataMask: [sample.dataMask]
                    };
                }
                """;
    }


    /*
     * -------------------------------------------------------------
     * Extract mean from Statistical API output.
     * -------------------------------------------------------------
     */
    private Double extractMean(
            JsonNode outputs,
            String outputId
    ) {

        JsonNode output =
                outputs.path(
                        outputId
                );


        if (output.isMissingNode()
                || output.isNull()) {

            return null;
        }


        JsonNode bands =
                output.path(
                        "bands"
                );


        if (!bands.isObject()
                || bands.isEmpty()) {

            return null;
        }


        JsonNode band =
                bands.path(
                        "B0"
                );


        /*
         * Fallback in case the API uses another band key.
         */
        if (band.isMissingNode()) {

            var iterator =
                    bands.elements();

            if (iterator.hasNext()) {

                band =
                        iterator.next();

            } else {

                return null;
            }
        }


        JsonNode stats =
                band.path(
                        "stats"
                );


        JsonNode mean =
                stats.path(
                        "mean"
                );


        if (!mean.isNumber()) {

            return null;
        }


        return mean.asDouble();
    }


    /*
     * -------------------------------------------------------------
     * Calculate valid AOI coverage.
     *
     * Statistical API returns:
     *
     * sampleCount
     * noDataCount
     *
     * for the dataMask output.
     *
     * Valid percentage =
     *
     * sampleCount
     * -------------------------
     * sampleCount + noDataCount
     *
     * -------------------------------------------------------------
     */
    private Double calculateValidCoveragePercent(
            JsonNode outputs
    ) {

        JsonNode output =
                outputs.path(
                        "dataMask"
                );


        if (output.isMissingNode()
                || output.isNull()) {

            return null;
        }


        JsonNode bands =
                output.path(
                        "bands"
                );


        if (!bands.isObject()
                || bands.isEmpty()) {

            return null;
        }


        JsonNode band =
                bands.path(
                        "B0"
                );


        if (band.isMissingNode()) {

            var iterator =
                    bands.elements();

            if (iterator.hasNext()) {

                band =
                        iterator.next();

            } else {

                return null;
            }
        }


        JsonNode stats =
                band.path(
                        "stats"
                );


        JsonNode sampleCountNode =
                stats.path(
                        "sampleCount"
                );


        JsonNode noDataCountNode =
                stats.path(
                        "noDataCount"
                );


        if (!sampleCountNode.isNumber()
                || !noDataCountNode.isNumber()) {

            return null;
        }


        double sampleCount =
                sampleCountNode.asDouble();


        double noDataCount =
                noDataCountNode.asDouble();


        double total =
                sampleCount
                        + noDataCount;


        if (total <= 0) {

            return null;
        }


        return (
                sampleCount
                        / total
        ) * 100.0;
    }


    /*
     * -------------------------------------------------------------
     * Confidence calculation.
     * -------------------------------------------------------------
     */
    private Double calculateConfidence(
            Double cloudCover,
            Double validCoverage
    ) {

        double confidence =
                1.0;


        /*
         * Reduce confidence for cloud coverage.
         */
        if (cloudCover != null) {

            if (cloudCover > 20) {

                confidence -= 0.20;
            }


            if (cloudCover > 40) {

                confidence -= 0.20;
            }


            if (cloudCover > 60) {

                confidence -= 0.20;
            }
        }


        /*
         * Reduce confidence for insufficient valid pixels.
         */
        if (validCoverage != null) {

            if (validCoverage < 90) {

                confidence -= 0.15;
            }


            if (validCoverage < 70) {

                confidence -= 0.15;
            }
        }


        confidence =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                confidence
                        )
                );


        return confidence * 100.0;
    }


    /*
     * -------------------------------------------------------------
     * OAuth access token.
     * -------------------------------------------------------------
     */
    private synchronized String getAccessToken() {

        /*
         * Reuse cached token while it is still valid.
         */
        if (cachedAccessToken != null
                && tokenExpiresAt != null
                && Instant.now()
                .isBefore(
                        tokenExpiresAt.minusSeconds(
                                60
                        )
                )) {

            return cachedAccessToken;
        }


        String clientId =
                System.getenv(
                        "COPERNICUS_CLIENT_ID"
                );


        String clientSecret =
                System.getenv(
                        "COPERNICUS_CLIENT_SECRET"
                );


        if (clientId == null
                || clientId.isBlank()
                || clientSecret == null
                || clientSecret.isBlank()) {

            throw new RuntimeException(
                    "Copernicus credentials are not configured. "
                            + "Set COPERNICUS_CLIENT_ID and "
                            + "COPERNICUS_CLIENT_SECRET."
            );
        }


        String response;

        try {

            response =
                    restClient
                            .post()
                            .uri(
                                    "https://identity.dataspace.copernicus.eu"
                                            + "/auth/realms/CDSE"
                                            + "/protocol/openid-connect/token"
                            )
                            .contentType(
                                    MediaType.APPLICATION_FORM_URLENCODED
                            )
                            .accept(
                                    MediaType.APPLICATION_JSON
                            )
                            .body(
                                    "grant_type=client_credentials"
                                            + "&client_id="
                                            + urlEncode(
                                            clientId
                                    )
                                            + "&client_secret="
                                            + urlEncode(
                                            clientSecret
                                    )
                            )
                            .retrieve()
                            .body(String.class);

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Copernicus OAuth request failed: "
                            + exception.getMessage(),
                    exception
            );
        }


        try {

            JsonNode root =
                    objectMapper.readTree(
                            response
                    );


            String token =
                    textValue(
                            root.path(
                                    "access_token"
                            )
                    );


            long expiresIn =
                    root.path(
                                    "expires_in"
                            )
                            .asLong(
                                    1800
                            );


            if (token == null
                    || token.isBlank()) {

                throw new RuntimeException(
                        "Copernicus OAuth response did not "
                                + "contain an access token."
                );
            }


            cachedAccessToken =
                    token;


            tokenExpiresAt =
                    Instant.now()
                            .plus(
                                    expiresIn,
                                    ChronoUnit.SECONDS
                            );


            return cachedAccessToken;


        } catch (Exception exception) {

            throw new RuntimeException(
                    "Failed to parse Copernicus OAuth response.",
                    exception
            );
        }
    }


    /*
     * -------------------------------------------------------------
     * URL encode helper.
     * -------------------------------------------------------------
     */
    private String urlEncode(
            String value
    ) {

        return java.net.URLEncoder
                .encode(
                        value,
                        java.nio.charset.StandardCharsets.UTF_8
                );
    }


    /*
     * -------------------------------------------------------------
     * JSON text helper.
     * -------------------------------------------------------------
     */
    private String textValue(
            JsonNode node
    ) {

        if (node == null
                || node.isMissingNode()
                || node.isNull()) {

            return null;
        }


        String value =
                node.asText();


        return value == null
                || value.isBlank()
                ? null
                : value;
    }


    /*
     * -------------------------------------------------------------
     * JSON number helper.
     * -------------------------------------------------------------
     */
    private Double doubleValue(
            JsonNode node
    ) {

        if (node == null
                || node.isMissingNode()
                || node.isNull()
                || !node.isNumber()) {

            return null;
        }


        return node.asDouble();
    }


    /*
     * -------------------------------------------------------------
     * Catalog scene.
     * -------------------------------------------------------------
     */
    private record CatalogScene(
            String sceneId,
            LocalDateTime observedAt,
            Double cloudCover
    ) {
    }


    /*
     * -------------------------------------------------------------
     * Calculated satellite metrics.
     * -------------------------------------------------------------
     */
    private record SatelliteMetrics(
            Double ndvi,
            Double ndwi,
            Double validCoveragePercent
    ) {
    }
}