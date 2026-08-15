package com.raiz.bakcend.integration.wordpress.dto;

import java.util.UUID;

public record WordPressUnpublishResponse(
        UUID propiedadId,
        Long wordpressPageIdAnterior,
        String status,
        int cardsEliminadasCasas,
        boolean listadoCasasActualizado,
        String mensaje) {
}
