package com.raiz.bakcend.integration.wordpress.service;

import com.raiz.bakcend.integration.wordpress.client.WordPressClient;
import com.raiz.bakcend.integration.wordpress.dto.WordPressImportabilidad;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPageClasificacionResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPageResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPropertyImportRequest;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPropertyPreviewResponse;
import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.model.Propiedad;
import com.raiz.bakcend.model.PropiedadOrigen;
import com.raiz.bakcend.model.PublicacionEstado;
import com.raiz.bakcend.repository.AgenteRepository;
import com.raiz.bakcend.repository.PropiedadRepository;
import com.raiz.bakcend.service.AdminAgentesCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Importa una página WordPress IMPORTABLE como Propiedad en BORRADOR.
 * Los campos exigidos por BD que WP no aporta (direccion, precio, tipoId)
 * deben venir en el request del agente. No importa imágenes.
 */
@Service
public class WordPressPropertyImportService {

    private final WordPressClient wordPressClient;
    private final WordPressPageClassifierService classifierService;
    private final WordPressPropertyPreviewParser previewParser;
    private final PropiedadRepository propiedadRepository;
    private final AgenteRepository agenteRepository;
    private final AdminAgentesCacheService adminAgentesCacheService;

    public WordPressPropertyImportService(
            WordPressClient wordPressClient,
            WordPressPageClassifierService classifierService,
            WordPressPropertyPreviewParser previewParser,
            PropiedadRepository propiedadRepository,
            AgenteRepository agenteRepository,
            AdminAgentesCacheService adminAgentesCacheService) {
        this.wordPressClient = wordPressClient;
        this.classifierService = classifierService;
        this.previewParser = previewParser;
        this.propiedadRepository = propiedadRepository;
        this.agenteRepository = agenteRepository;
        this.adminAgentesCacheService = adminAgentesCacheService;
    }

    @Transactional
    public Propiedad importar(
            Long wordpressPageId,
            WordPressPropertyImportRequest request,
            Authentication authentication) {
        if (wordpressPageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id de página WordPress es obligatorio");
        }

        validarRequest(request);

        String externalId = String.valueOf(wordpressPageId);
        if (propiedadRepository.existsByOrigenAndExternalId(PropiedadOrigen.WORDPRESS, externalId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La propiedad ya fue importada desde WordPress (externalId=" + externalId + ")");
        }

        WordPressPageResponse pagina = wordPressClient.obtenerPagina(wordpressPageId);
        WordPressPageClasificacionResponse clasificacion = classifierService.clasificarUna(pagina);
        if (clasificacion.clasificacion() != WordPressImportabilidad.IMPORTABLE) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "La página no es IMPORTABLE: " + clasificacion.motivo());
        }

        WordPressPropertyPreviewResponse preview = previewParser.parse(pagina);
        Agente agente = resolverAgenteAutenticado(authentication);

        Propiedad propiedad = new Propiedad();
        propiedad.setTitulo(preview.titulo());
        propiedad.setDescripcion(preview.descripcion());
        propiedad.setDireccion(request.getDireccion().trim());
        propiedad.setPrecio(request.getPrecio());
        propiedad.setTipoId(request.getTipoId());
        propiedad.setAgenteId(agente.getId());
        // Explícito: no usar el default PUBLICADA ni resolverPublicacionEstado(null).
        propiedad.setPublicacionEstado(PublicacionEstado.BORRADOR);
        propiedad.setOrigen(PropiedadOrigen.WORDPRESS);
        propiedad.setExternalId(externalId);

        Propiedad creada = propiedadRepository.save(propiedad);
        adminAgentesCacheService.evictAll(
                "wordpress-import propiedadId=" + creada.getId() + " externalId=" + externalId);
        return creada;
    }

    private void validarRequest(WordPressPropertyImportRequest request) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Body obligatorio: direccion, precio, tipoId");
        }

        List<String> faltantes = new ArrayList<>();
        if (request.getDireccion() == null || request.getDireccion().isBlank()) {
            faltantes.add("direccion");
        }
        if (request.getPrecio() == null) {
            faltantes.add("precio");
        }
        if (request.getTipoId() == null) {
            faltantes.add("tipoId");
        }

        if (!faltantes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Campos obligatorios inválidos o ausentes: " + String.join(", ", faltantes));
        }

        if (request.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "precio no puede ser negativo");
        }

        if (request.getTipoId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipoId debe ser un entero positivo");
        }
    }

    private Agente resolverAgenteAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticación requerida");
        }

        UUID usuarioId = UUID.fromString(authentication.getName());
        return agenteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Agente no encontrado"));
    }
}
