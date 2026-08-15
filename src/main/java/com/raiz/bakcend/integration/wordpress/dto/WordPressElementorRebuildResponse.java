package com.raiz.bakcend.integration.wordpress.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordPressElementorRebuildResponse {

    private Boolean ok;

    @JsonProperty("page_id")
    private Long pageId;

    private String message;
}
