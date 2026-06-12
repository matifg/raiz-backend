package com.raiz.bakcend.dto;

import com.raiz.bakcend.model.ImagenPropiedad;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ImagenPropiedadResponse {
    private UUID id;
    private String url;
    private Integer orden;

    public static ImagenPropiedadResponse from(ImagenPropiedad imagen) {
        return new ImagenPropiedadResponse(imagen.getId(), imagen.getUrl(), imagen.getOrden());
    }
}
