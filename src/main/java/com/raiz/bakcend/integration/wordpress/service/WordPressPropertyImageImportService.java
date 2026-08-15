package com.raiz.bakcend.integration.wordpress.service;

import com.raiz.bakcend.dto.ImagenPropiedadResponse;
import com.raiz.bakcend.integration.wordpress.client.WordPressClient;
import com.raiz.bakcend.integration.wordpress.dto.WordPressImageImportResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPageResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPropertyPreviewResponse;
import com.raiz.bakcend.model.ImagenPropiedad;
import com.raiz.bakcend.model.Propiedad;
import com.raiz.bakcend.model.PropiedadOrigen;
import com.raiz.bakcend.repository.ImagenPropiedadRepository;
import com.raiz.bakcend.repository.PropiedadRepository;
import com.raiz.bakcend.service.PropiedadPortadaService;
import com.raiz.bakcend.service.PropiedadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Asocia imágenes remotas de WordPress a una Propiedad ya importada.
 * Idempotente: no duplica URLs ya existentes. No descarga archivos.
 */
@Service
public class WordPressPropertyImageImportService {

    private final PropiedadRepository propiedadRepository;
    private final ImagenPropiedadRepository imagenPropiedadRepository;
    private final WordPressClient wordPressClient;
    private final WordPressPropertyPreviewParser previewParser;
    private final PropiedadPortadaService portadaService;
    private final PropiedadService propiedadService;

    public WordPressPropertyImageImportService(
            PropiedadRepository propiedadRepository,
            ImagenPropiedadRepository imagenPropiedadRepository,
            WordPressClient wordPressClient,
            WordPressPropertyPreviewParser previewParser,
            PropiedadPortadaService portadaService,
            PropiedadService propiedadService) {
        this.propiedadRepository = propiedadRepository;
        this.imagenPropiedadRepository = imagenPropiedadRepository;
        this.wordPressClient = wordPressClient;
        this.previewParser = previewParser;
        this.portadaService = portadaService;
        this.propiedadService = propiedadService;
    }

    @Transactional
    public WordPressImageImportResponse importarImagenes(Long wordpressPageId, Authentication authentication) {
        if (wordpressPageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id de página WordPress es obligatorio");
        }

        String externalId = String.valueOf(wordpressPageId);
        Propiedad propiedad = propiedadRepository
                .findByOrigenAndExternalId(PropiedadOrigen.WORDPRESS, externalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No hay propiedad importada desde WordPress con externalId=" + externalId));

        if (propiedad.getOrigen() != PropiedadOrigen.WORDPRESS) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "La propiedad no es de origen WORDPRESS; no se modifican sus imágenes");
        }

        if (!propiedadService.puedeGestionarAgente(authentication, propiedad.getAgenteId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        WordPressPageResponse pagina = wordPressClient.obtenerPagina(wordpressPageId);
        WordPressPropertyPreviewResponse preview = previewParser.parse(pagina);
        List<String> urls = preview.imagenes() == null ? List.of() : preview.imagenes();

        List<ImagenPropiedad> existentes =
                imagenPropiedadRepository.findByPropiedadIdOrderByOrdenAscCreadoEnAsc(propiedad.getId());
        Map<String, ImagenPropiedad> porUrl = new HashMap<>();
        for (ImagenPropiedad imagen : existentes) {
            if (imagen.getUrl() != null) {
                porUrl.putIfAbsent(imagen.getUrl(), imagen);
            }
        }

        int creadas = 0;
        int omitidas = 0;
        List<ImagenPropiedad> nuevas = new ArrayList<>();

        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            if (url == null || url.isBlank()) {
                continue;
            }

            ImagenPropiedad existente = porUrl.get(url);
            if (existente != null) {
                omitidas++;
                if (existente.getOrden() == null || existente.getOrden() != i) {
                    existente.setOrden(i);
                }
                continue;
            }

            ImagenPropiedad imagen = new ImagenPropiedad();
            imagen.setPropiedad(propiedad);
            imagen.setUrl(url);
            imagen.setOrden(i);
            nuevas.add(imagen);
            porUrl.put(url, imagen);
            creadas++;
        }

        if (!nuevas.isEmpty()) {
            imagenPropiedadRepository.saveAll(nuevas);
        }
        if (omitidas > 0) {
            // Persistir posibles ajustes de orden en existentes.
            imagenPropiedadRepository.saveAll(
                    existentes.stream().filter(img -> urls.contains(img.getUrl())).toList());
        }

        portadaService.persistirPortada(propiedad.getId());

        Propiedad actualizada = propiedadRepository.findById(propiedad.getId()).orElse(propiedad);
        List<ImagenPropiedadResponse> imagenes = imagenPropiedadRepository
                .findByPropiedadIdOrderByOrdenAscCreadoEnAsc(propiedad.getId())
                .stream()
                .map(ImagenPropiedadResponse::from)
                .toList();

        return new WordPressImageImportResponse(
                actualizada.getId(),
                externalId,
                creadas,
                omitidas,
                actualizada.getImageUrl(),
                imagenes);
    }
}
