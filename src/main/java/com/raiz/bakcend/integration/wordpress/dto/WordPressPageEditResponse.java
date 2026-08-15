package com.raiz.bakcend.integration.wordpress.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordPressPageEditResponse {

    private Long id;
    private String link;
    private String status;
    private Title title;
    private JsonNode meta;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Title {
        private String raw;
        private String rendered;
    }
}
