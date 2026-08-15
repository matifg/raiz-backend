package com.raiz.bakcend.integration.wordpress.dto;

import com.raiz.bakcend.dto.ImagenPropiedadResponse;

import java.util.List;
import java.util.UUID;

public record WordPressImageImportResponse(
        UUID propiedadId,
        String externalId,
        int creadas,
        int omitidas,
        String imageUrl,
        List<ImagenPropiedadResponse> imagenes) {
}
