package com.raiz.bakcend.integration.wordpress.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WordPressPageResponse {

    private Long id;
    private String slug;
    private String link;
    private String status;
    private String date;

    @Setter(AccessLevel.NONE)
    private String title;

    /**
     * HTML Elementor completo. Solo se deserializa desde WP; no se reexpone en JSON propio.
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String content;

    @JsonIgnore
    public String getContent() {
        return content;
    }

    @JsonProperty("title")
    public void unpackTitle(Title titleObj) {
        if (titleObj != null) {
            this.title = titleObj.getRendered();
        }
    }

    @JsonProperty("content")
    public void unpackContent(RenderedBlock contentObj) {
        if (contentObj != null) {
            this.content = contentObj.getRendered();
        }
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Title {
        private String rendered;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RenderedBlock {
        private String rendered;
    }
}
