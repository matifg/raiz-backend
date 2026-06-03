package com.raiz.bakcend.dto.georef;

import java.util.List;

import lombok.Data;

@Data
public class GeorefProvinciasResponse {

    private List<GeorefProvincia> provincias;

    @Data
    public static class GeorefProvincia {
        private String id;
        private String nombre;
    }
}
