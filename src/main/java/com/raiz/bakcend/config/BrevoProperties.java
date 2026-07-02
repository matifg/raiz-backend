package com.raiz.bakcend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "brevo")
public class BrevoProperties {

    private Api api = new Api();
    private Sender sender = new Sender();

    @Data
    public static class Api {
        private String key;
        private String url;
    }

    @Data
    public static class Sender {
        private String email;
        private String name = "Inmo360";
    }
}
