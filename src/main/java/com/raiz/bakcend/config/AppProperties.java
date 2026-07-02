package com.raiz.bakcend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Frontend frontend = new Frontend();

    @Data
    public static class Frontend {
        private String url = "http://localhost:3000";
    }
}
