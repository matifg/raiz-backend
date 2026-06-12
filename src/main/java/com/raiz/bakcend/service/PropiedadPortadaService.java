package com.raiz.bakcend.service;

import com.raiz.bakcend.model.ImagenPropiedad;
import com.raiz.bakcend.model.Propiedad;
import com.raiz.bakcend.repository.ImagenPropiedadRepository;
import com.raiz.bakcend.repository.PropiedadRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PropiedadPortadaService {

    private final ImagenPropiedadRepository imagenPropiedadRepository;
    private final PropiedadRepository propiedadRepository;

    public PropiedadPortadaService(
            ImagenPropiedadRepository imagenPropiedadRepository,
            PropiedadRepository propiedadRepository) {
        this.imagenPropiedadRepository = imagenPropiedadRepository;
        this.propiedadRepository = propiedadRepository;
    }

    public void persistirPortada(UUID propiedadId) {
        Propiedad propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Propiedad no encontrada"));

        Map<UUID, String> portadas = resolverPortadas(List.of(propiedadId));
        propiedad.setImageUrl(portadas.get(propiedadId));
        propiedadRepository.save(propiedad);
    }

    public Propiedad aplicarPortada(Propiedad propiedad) {
        if (propiedad == null || propiedad.getId() == null) {
            return propiedad;
        }
        aplicarPortadas(List.of(propiedad));
        return propiedad;
    }

    public List<Propiedad> aplicarPortadas(List<Propiedad> propiedades) {
        if (propiedades == null || propiedades.isEmpty()) {
            return propiedades;
        }

        List<UUID> ids = propiedades.stream()
                .map(Propiedad::getId)
                .filter(id -> id != null)
                .toList();

        if (ids.isEmpty()) {
            return propiedades;
        }

        Map<UUID, String> portadaPorPropiedad = resolverPortadas(ids);

        for (Propiedad propiedad : propiedades) {
            if (propiedad.getId() == null) {
                continue;
            }
            String portada = portadaPorPropiedad.get(propiedad.getId());
            propiedad.setImageUrl(portada);
        }

        return propiedades;
    }

    private Map<UUID, String> resolverPortadas(Collection<UUID> propiedadIds) {
        List<ImagenPropiedad> imagenes = imagenPropiedadRepository
                .findByPropiedad_IdInOrderByOrdenAscCreadoEnAsc(propiedadIds);

        Map<UUID, String> portadaPorPropiedad = new HashMap<>();
        for (ImagenPropiedad imagen : imagenes) {
            if (imagen.getPropiedad() == null || imagen.getPropiedad().getId() == null) {
                continue;
            }
            UUID propiedadId = imagen.getPropiedad().getId();
            portadaPorPropiedad.putIfAbsent(propiedadId, imagen.getUrl());
        }
        return portadaPorPropiedad;
    }
}
