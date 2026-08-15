package com.raiz.bakcend.integration.wordpress.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordPressMediaResponse {

    private Long id;

    @JsonProperty("source_url")
    private String sourceUrl;

    private String link;
}
