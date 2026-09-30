package com.agrinexus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "agrinexus.satellite.copernicus")
public class CopernicusSatelliteProperties {

    private String clientId;

    private String clientSecret;

    private String identityUrl =
            "https://identity.dataspace.copernicus.eu/auth/realms/CDSE/protocol/openid-connect/token";

    private String statisticsUrl =
            "https://sh.dataspace.copernicus.eu/statistics/v1";

    private String catalogUrl =
            "https://sh.dataspace.copernicus.eu/catalog/v1/search";

    private String collection =
            "sentinel-2-l2a";

    private int lookbackDays =
            45;

    private double maxCloudCover =
            60.0;

    /*
     * Farm AOI half-size in WGS84 degrees.
     *
     * 0.002 degrees is approximately a few hundred metres
     * around the farm coordinate.
     */
    private double aoiHalfSizeDegrees =
            0.002;
}