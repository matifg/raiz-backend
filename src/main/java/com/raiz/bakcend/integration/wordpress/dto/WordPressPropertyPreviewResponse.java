package com.raiz.bakcend.integration.wordpress.dto;

import java.util.List;

public record WordPressPropertyPreviewResponse(
        Long wordpressPageId,
        String titulo,
        String descripcion,
        List<String> imagenes) {
}
