package com.helios.gateway;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HeliosGatewayApplicationTest {

    @Test
    void configuresOnlyHeliosAndLocalDevelopmentOrigins() {
        var application = new HeliosGatewayApplication();
        var configuration = application.heliosCorsConfiguration();

        assertEquals(List.of(
                "https://helios-platform.site",
                "https://*.helios-platform.site",
                "http://localhost:*",
                "http://127.0.0.1:*"
        ), configuration.getAllowedOriginPatterns());
        assertNotNull(application.corsWebFilter(configuration));
    }
}
