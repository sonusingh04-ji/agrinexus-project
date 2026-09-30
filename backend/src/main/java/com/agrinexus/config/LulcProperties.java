package com.agrinexus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "agrinexus.satellite.lulc")
public class LulcProperties {

    private String clientId;

    private String clientSecret;

    private String identityUrl =
            "https://identity.dataspace.copernicus.eu/auth/realms/CDSE/protocol/openid-connect/token";

    private String statisticsUrl =
            "https://sh.dataspace.copernicus.eu/statistics/v1";

    private String collectionId =
            "828f6b20-8ffd-48f8-a1da-fefd271456db";

    private Integer baseYear = 2020;

    private Double aoiRadiusMeters = 250.0;

    private Integer spatialResolutionMeters = 10;

    public String getDataType() {

        return "byoc-" + collectionId;
    }
}