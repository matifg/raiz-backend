package com.raiz.bakcend.integration.wordpress.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordPressJwtTokenResponse {
    private String token;
    private String userEmail;
    private String userNicename;
    private String userDisplayName;
}
