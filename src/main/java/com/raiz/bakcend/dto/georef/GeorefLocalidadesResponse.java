package com.raiz.bakcend.dto.georef;

import java.util.List;

import lombok.Data;

@Data
public class GeorefLocalidadesResponse {

    private List<GeorefLocalidad> localidades;

    @Data
    public static class GeorefLocalidad {
        private String id;
        private String nombre;
    }
}
