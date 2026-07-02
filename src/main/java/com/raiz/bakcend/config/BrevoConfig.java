package com.raiz.bakcend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BrevoProperties.class)
public class BrevoConfig {

    @Bean
    public RestClient brevoRestClient(BrevoProperties brevoProperties) {
        return RestClient.builder()
                .defaultHeader("api-key", brevoProperties.getApi().getKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
