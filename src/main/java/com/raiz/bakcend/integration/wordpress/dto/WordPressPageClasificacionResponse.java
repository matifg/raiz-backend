package com.raiz.bakcend.integration.wordpress.dto;

public record WordPressPageClasificacionResponse(
        Long id,
        String titulo,
        String url,
        WordPressImportabilidad clasificacion,
        String motivo) {
}
