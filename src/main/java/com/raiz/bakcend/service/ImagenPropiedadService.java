package com.raiz.bakcend.service;

import com.raiz.bakcend.dto.ImagenOrdenItem;
import com.raiz.bakcend.dto.ImagenPropiedadResponse;
import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.model.ImagenPropiedad;
import com.raiz.bakcend.model.Propiedad;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.repository.AgenteRepository;
import com.raiz.bakcend.repository.ImagenPropiedadRepository;
import com.raiz.bakcend.repository.PropiedadRepository;
import com.raiz.bakcend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ImagenPropiedadService {

    private final ImagenPropiedadRepository imagenRepository;
    private final PropiedadRepository propiedadRepository;
    private final AgenteRepository agenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PropiedadPortadaService portadaService;

    public ImagenPropiedadService(
            ImagenPropiedadRepository imagenRepository,
            PropiedadRepository propiedadRepository,
            AgenteRepository agenteRepository,
            UsuarioRepository usuarioRepository,
            PropiedadPortadaService portadaService) {
        this.imagenRepository = imagenRepository;
        this.propiedadRepository = propiedadRepository;
        this.agenteRepository = agenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.portadaService = portadaService;
    }

    public List<ImagenPropiedadResponse> listarPorPropiedad(UUID propiedadId) {
        return imagenRepository.findByPropiedadIdOrderByOrdenAscCreadoEnAsc(propiedadId)
                .stream()
                .map(ImagenPropiedadResponse::from)
                .toList();
    }

    @Transactional
    public ImagenPropiedadResponse crear(ImagenPropiedad imagen, Authentication authentication) {
        if (imagen.getPropiedad() == null || imagen.getPropiedad().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "propiedad.id es obligatorio");
        }

        UUID propiedadId = imagen.getPropiedad().getId();
        validarAccesoPropiedad(propiedadId, authentication);

        Propiedad propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Propiedad no encontrada"));

        imagen.setId(null);
        imagen.setPropiedad(propiedad);
        ImagenPropiedad guardada = imagenRepository.save(imagen);
        portadaService.persistirPortada(propiedadId);
        return ImagenPropiedadResponse.from(guardada);
    }

    @Transactional
    public ImagenPropiedadResponse actualizarOrden(
            UUID id, Integer orden, Authentication authentication) {
        ImagenPropiedad imagen = imagenRepository.findByIdWithPropiedad(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Imagen no encontrada"));

        UUID propiedadId = imagen.getPropiedad().getId();
        validarAccesoPropiedad(propiedadId, authentication);

        imagen.setOrden(orden);
        ImagenPropiedad guardada = imagenRepository.save(imagen);
        portadaService.persistirPortada(propiedadId);
        return ImagenPropiedadResponse.from(guardada);
    }

    @Transactional
    public List<ImagenPropiedadResponse> reordenarPropiedad(
            UUID propiedadId, List<ImagenOrdenItem> items, Authentication authentication) {
        validarAccesoPropiedad(propiedadId, authentication);

        List<ImagenPropiedad> existentes =
                imagenRepository.findByPropiedadIdOrderByOrdenAscCreadoEnAsc(propiedadId);
        Set<UUID> idsPropiedad = existentes.stream()
                .map(ImagenPropiedad::getId)
                .collect(Collectors.toSet());

        for (ImagenOrdenItem item : items) {
            if (item.getId() == null || !idsPropiedad.contains(item.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La imagen " + item.getId() + " no pertenece a la propiedad");
            }
        }

        Map<UUID, Integer> ordenPorId = items.stream()
                .collect(Collectors.toMap(ImagenOrdenItem::getId, ImagenOrdenItem::getOrden));

        for (ImagenPropiedad imagen : existentes) {
            if (ordenPorId.containsKey(imagen.getId())) {
                imagen.setOrden(ordenPorId.get(imagen.getId()));
            }
        }
        imagenRepository.saveAll(existentes);
        portadaService.persistirPortada(propiedadId);

        return existentes.stream()
                .sorted(Comparator
                        .comparing(ImagenPropiedad::getOrden, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ImagenPropiedad::getCreadoEn, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ImagenPropiedadResponse::from)
                .toList();
    }

    @Transactional
    public void eliminar(UUID id, Authentication authentication) {
        ImagenPropiedad imagen = imagenRepository.findByIdWithPropiedad(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Imagen no encontrada"));

        UUID propiedadId = imagen.getPropiedad().getId();
        validarAccesoPropiedad(propiedadId, authentication);

        imagenRepository.delete(imagen);
        portadaService.persistirPortada(propiedadId);
    }

    private void validarAccesoPropiedad(UUID propiedadId, Authentication authentication) {
        Usuario usuario = usuarioRepository.findById(UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        if ("ADMIN".equalsIgnoreCase(usuario.getRol())) {
            return;
        }

        Propiedad propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Propiedad no encontrada"));

        Agente agente = agenteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No autorizado"));

        if (!propiedad.getAgenteId().equals(agente.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }
    }
}
