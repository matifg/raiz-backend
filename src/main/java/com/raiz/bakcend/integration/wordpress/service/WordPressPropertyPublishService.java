package com.raiz.bakcend.integration.wordpress.service;

import com.raiz.bakcend.integration.wordpress.auth.WordPressJwtAuthService;
import com.raiz.bakcend.integration.wordpress.client.WordPressClient;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPageEditResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPublishResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressUnpublishResponse;
import com.raiz.bakcend.model.ImagenPropiedad;
import com.raiz.bakcend.model.Propiedad;
import com.raiz.bakcend.repository.ImagenPropiedadRepository;
import com.raiz.bakcend.repository.PropiedadRepository;
import com.raiz.bakcend.service.PropiedadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WordPressPropertyPublishService {

    private static final Logger log = LoggerFactory.getLogger(WordPressPropertyPublishService.class);

    private final PropiedadRepository propiedadRepository;
    private final ImagenPropiedadRepository imagenPropiedadRepository;
    private final PropiedadService propiedadService;
    private final WordPressJwtAuthService jwtAuthService;
    private final WordPressClient wordPressClient;
    private final WordPressMediaUploadService mediaUploadService;
    private final WordPressElementorCloneService elementorCloneService;
    private final WordPressPropertyListingService listingService;
    private final Long templatePageId;
    private final String initialStatus;

    public WordPressPropertyPublishService(
            PropiedadRepository propiedadRepository,
            ImagenPropiedadRepository imagenPropiedadRepository,
            PropiedadService propiedadService,
            WordPressJwtAuthService jwtAuthService,
            WordPressClient wordPressClient,
            WordPressMediaUploadService mediaUploadService,
            WordPressElementorCloneService elementorCloneService,
            WordPressPropertyListingService listingService,
            @Value("${wordpress.template.page-id}") Long templatePageId,
            @Value("${wordpress.publish.initial-status:draft}") String initialStatus) {
        this.propiedadRepository = propiedadRepository;
        this.imagenPropiedadRepository = imagenPropiedadRepository;
        this.propiedadService = propiedadService;
        this.jwtAuthService = jwtAuthService;
        this.wordPressClient = wordPressClient;
        this.mediaUploadService = mediaUploadService;
        this.elementorCloneService = elementorCloneService;
        this.listingService = listingService;
        this.templatePageId = templatePageId;
        this.initialStatus = initialStatus;
    }

    /**
     * Publicación idempotente: lock pesimista por propiedad antes de chequear/crear ficha WP.
     * Dos POST concurrentes: el segundo espera el lock, releerá wordpressPageId y responderá 409.
     */
    @Transactional
    public WordPressPublishResponse publicar(UUID propiedadId, Authentication authentication) {
        if (propiedadId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "propiedadId es obligatorio");
        }

        // Lock primero: el check de wordpressPageId solo es seguro bajo exclusividad.
        Propiedad propiedad = propiedadRepository.findByIdForUpdate(propiedadId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Propiedad no encontrada"));

        if (!propiedadService.puedeGestionarAgente(authentication, propiedad.getAgenteId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        if (StringUtils.hasText(propiedad.getWordpressPageId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La propiedad ya fue publicada en WordPress (wordpressPageId="
                            + propiedad.getWordpressPageId() + ")");
        }

        List<ImagenPropiedad> imagenes = imagenPropiedadRepository
                .findByPropiedadIdOrderByOrdenAscCreadoEnAsc(propiedadId);
        validarMinimos(propiedad, imagenes);

        String token = jwtAuthService.getToken();
        WordPressPageEditResponse plantilla = wordPressClient.obtenerPaginaEdit(templatePageId, token);
        if (plantilla.getMeta() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Plantilla WordPress sin meta (¿JWT inválido o meta no expuesta?)");
        }

        List<WordPressMediaUploadService.UploadedMedia> medias =
                mediaUploadService.subirImagenes(imagenes, token);

        String slideTitle = resolverSlideTitle(propiedad);
        String descripcionHtml = aHtmlParrafo(propiedad.getDescripcion());
        String elementorData = elementorCloneService.clonar(
                plantilla.getMeta(), slideTitle, descripcionHtml, medias);

        Map<String, Object> meta = new HashMap<>();
        meta.put("_elementor_edit_mode", "builder");
        meta.put("_elementor_template_type", "wp-page");
        meta.put("_elementor_data", elementorData);
        meta.put("_elementor_page_settings", elementorCloneService.pageSettingsFromMeta(plantilla.getMeta()));

        Map<String, Object> body = new HashMap<>();
        body.put("title", propiedad.getTitulo().trim());
        body.put("status", initialStatus);
        body.put("meta", meta);

        WordPressPageEditResponse creada;
        try {
            creada = wordPressClient.crearPagina(body, token);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.BAD_GATEWAY
                    || ex.getStatusCode().value() == 401) {
                jwtAuthService.invalidate();
                token = jwtAuthService.getToken();
                creada = wordPressClient.crearPagina(body, token);
            } else {
                throw ex;
            }
        }

        // Persistir lo antes posible (flush) para acortar la ventana sin ID en BD
        // mientras aún se sostiene el lock hasta el commit al final del método.
        propiedad.setWordpressPageId(String.valueOf(creada.getId()));
        propiedadRepository.saveAndFlush(propiedad);

        log.info(
                "[WORDPRESS-PUBLISH] propiedadId={} wordpressPageId={} status={} imagenes={}",
                propiedadId, creada.getId(), creada.getStatus(), medias.size());

        boolean listadoOk = false;
        String listadoMensaje;
        try {
            // Token fresco por si el upload/create demoró cerca del TTL del JWT WP.
            String listingToken = jwtAuthService.getToken();
            listingService.agregarCardCasas(propiedad, creada.getId(), medias.getFirst(), listingToken);
            listadoOk = true;
            listadoMensaje = "Card en Casas y rebuild Elementor OK (página listado regenerada)";
            log.info(
                    "[WORDPRESS-PUBLISH] Listado Casas + rebuild OK para wordpressPageId={}",
                    creada.getId());
        } catch (Exception ex) {
            // Etapa 1 ya persistió: no revertir ni borrar la ficha individual.
            listadoMensaje = "Ficha creada (wordpressPageId=" + creada.getId()
                    + ") pero falló listado Casas y/o rebuild Elementor: " + ex.getMessage();
            log.error("[WORDPRESS-PUBLISH] {}", listadoMensaje, ex);
        }

        return new WordPressPublishResponse(
                propiedad.getId(),
                creada.getId(),
                creada.getStatus() != null ? creada.getStatus() : initialStatus,
                creada.getLink(),
                propiedad.getTitulo(),
                medias.size(),
                listadoOk,
                listadoMensaje);
    }

    /**
     * Despublicación idempotente: lock → si no hay wordpressPageId → 409;
     * Papelera WP → quitar cards Casas + rebuild → limpiar wordpressPageId.
     */
    @Transactional
    public WordPressUnpublishResponse despublicar(UUID propiedadId, Authentication authentication) {
        if (propiedadId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "propiedadId es obligatorio");
        }

        Propiedad propiedad = propiedadRepository.findByIdForUpdate(propiedadId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Propiedad no encontrada"));

        if (!propiedadService.puedeGestionarAgente(authentication, propiedad.getAgenteId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        if (!StringUtils.hasText(propiedad.getWordpressPageId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La propiedad no está publicada en WordPress (wordpressPageId=null)");
        }

        final Long wordpressPageId;
        try {
            wordpressPageId = Long.valueOf(propiedad.getWordpressPageId().trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "wordpressPageId inválido en propiedad: " + propiedad.getWordpressPageId());
        }

        String token = jwtAuthService.getToken();
        try {
            wordPressClient.moverPaginaAPapelera(wordpressPageId, token);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.BAD_GATEWAY
                    || ex.getStatusCode().value() == 401) {
                jwtAuthService.invalidate();
                token = jwtAuthService.getToken();
                wordPressClient.moverPaginaAPapelera(wordpressPageId, token);
            } else {
                throw ex;
            }
        }

        // Solo tras Papelera OK: mutar Casas. Si falla listing, no limpiar wordpressPageId.
        String listingToken = jwtAuthService.getToken();
        int cardsEliminadas = listingService.eliminarCardsCasas(
                propiedad.getId(), wordpressPageId, listingToken);

        propiedad.setWordpressPageId(null);
        propiedadRepository.saveAndFlush(propiedad);

        log.info(
                "[WORDPRESS-UNPUBLISH] propiedadId={} wordpressPageIdAnterior={} cardsEliminadas={}",
                propiedadId, wordpressPageId, cardsEliminadas);

        return new WordPressUnpublishResponse(
                propiedad.getId(),
                wordpressPageId,
                "trash",
                cardsEliminadas,
                true,
                "Página en Papelera, cards Casas eliminadas (" + cardsEliminadas
                        + ") y rebuild OK; wordpressPageId=null");
    }

    private void validarMinimos(Propiedad propiedad, List<ImagenPropiedad> imagenes) {
        if (!StringUtils.hasText(propiedad.getTitulo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "titulo es obligatorio");
        }
        if (!StringUtils.hasText(propiedad.getDescripcion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "descripcion es obligatoria");
        }
        if (imagenes == null || imagenes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Se requiere al menos una imagen para publicar en WordPress");
        }
        boolean algunaConUrl = imagenes.stream()
                .anyMatch(img -> img != null && StringUtils.hasText(img.getUrl()));
        if (!algunaConUrl) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Se requiere al menos una imagen con URL");
        }
    }

    private String resolverSlideTitle(Propiedad propiedad) {
        if (StringUtils.hasText(propiedad.getOperacion())) {
            return propiedad.getOperacion().trim();
        }
        return "Propiedad a la venta";
    }

    private String aHtmlParrafo(String descripcion) {
        String trimmed = descripcion.trim();
        if (trimmed.startsWith("<")) {
            return trimmed;
        }
        return "<p>" + escaparHtml(trimmed) + "</p>";
    }

    private String escaparHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
