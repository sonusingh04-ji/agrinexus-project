package com.agrinexus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "copernicus.sentinel2")
public class Sentinel2Properties {

    private String baseUrl =
            "https://sh.dataspace.copernicus.eu";

    private String collection =
            "sentinel-2-l2a";

    private Integer lookbackDays = 30;

    private Double maxCloudCoverage = 30.0;

    private Double spatialResolutionMeters = 10.0;

    private Double aoiRadiusMeters = 250.0;
}
