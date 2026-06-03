package com.raiz.bakcend.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.raiz.bakcend.config.CacheConfig;
import com.raiz.bakcend.dto.LocalidadResponse;
import com.raiz.bakcend.dto.ProvinciaResponse;
import com.raiz.bakcend.dto.georef.GeorefLocalidadesResponse;
import com.raiz.bakcend.dto.georef.GeorefProvinciasResponse;

@Service
public class GeorefService {

    private static final int PROVINCIAS_MAX = 30;
    private static final int LOCALIDADES_MAX = 5000;

    private final RestClient georefRestClient;

    public GeorefService(RestClient georefRestClient) {
        this.georefRestClient = georefRestClient;
    }

    @Cacheable(cacheNames = CacheConfig.GEOREF_PROVINCIAS_CACHE)
    public List<ProvinciaResponse> listarProvincias() {
        GeorefProvinciasResponse response = get(
                uriBuilder -> uriBuilder
                        .path("/provincias")
                        .queryParam("max", PROVINCIAS_MAX)
                        .queryParam("orden", "nombre")
                        .build(),
                GeorefProvinciasResponse.class);

        if (response.getProvincias() == null) {
            return List.of();
        }

        return response.getProvincias().stream()
                .map(p -> new ProvinciaResponse(p.getId(), p.getNombre()))
                .toList();
    }

    @Cacheable(cacheNames = CacheConfig.GEOREF_LOCALIDADES_CACHE, key = "#provinciaId")
    public List<LocalidadResponse> listarLocalidades(String provinciaId) {
        if (provinciaId == null || provinciaId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El parámetro provincia es obligatorio");
        }

        GeorefLocalidadesResponse response = get(
                uriBuilder -> uriBuilder
                        .path("/localidades")
                        .queryParam("provincia", provinciaId.trim())
                        .queryParam("max", LOCALIDADES_MAX)
                        .queryParam("orden", "nombre")
                        .build(),
                GeorefLocalidadesResponse.class);

        if (response.getLocalidades() == null) {
            return List.of();
        }

        return response.getLocalidades().stream()
                .map(l -> new LocalidadResponse(l.getId(), l.getNombre()))
                .toList();
    }

    private <T> T get(
            java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uriFunction,
            Class<T> bodyType) {
        try {
            T body = georefRestClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .body(bodyType);

            if (body == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta vacía de Georef");
            }
            return body;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar Georef: " + ex.getMessage(),
                    ex);
        }
    }
}
