package com.raiz.bakcend.integration.wordpress.dto;

public record WordPressPublishResponse(
        java.util.UUID propiedadId,
        Long wordpressPageId,
        String status,
        String link,
        String titulo,
        int imagenesSubidas,
        boolean listadoCasasActualizado,
        String listadoCasasMensaje) {
}
