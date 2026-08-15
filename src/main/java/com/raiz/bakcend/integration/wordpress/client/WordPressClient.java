package com.raiz.bakcend.integration.wordpress.client;

import com.raiz.bakcend.integration.wordpress.dto.WordPressMediaResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPageEditResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPageResponse;
import com.raiz.bakcend.integration.wordpress.dto.WordPressElementorRebuildResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Component
public class WordPressClient {

    private static final Logger log = LoggerFactory.getLogger(WordPressClient.class);

    private final RestClient wordpressRestClient;
    private final String baseUrl;
    private final String elementorRebuildRoute;

    public WordPressClient(
            @Qualifier("wordpressRestClient") RestClient wordpressRestClient,
            @Value("${wordpress.api.base-url}") String baseUrl,
            @Value("${wordpress.listing.elementor-rebuild-route:/inmo360/v1/elementor/rebuild}")
                    String elementorRebuildRoute) {
        this.wordpressRestClient = wordpressRestClient;
        this.baseUrl = baseUrl;
        this.elementorRebuildRoute = elementorRebuildRoute;
    }

    public List<WordPressPageResponse> listarPaginas() {
        log.info("[WORDPRESS] Conectando a {}", baseUrl);

        try {
            List<WordPressPageResponse> paginas = wordpressRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("rest_route", "/wp/v2/pages")
                            .queryParam("per_page", 100)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<WordPressPageResponse>>() {});

            if (paginas == null) {
                paginas = List.of();
            }

            log.info("[WORDPRESS] Páginas obtenidas: {}", paginas.size());
            return paginas;
        } catch (RestClientException ex) {
            log.error("[WORDPRESS] Error al consumir WordPress: {}", ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar WordPress: " + ex.getMessage(),
                    ex);
        }
    }

    public WordPressPageResponse obtenerPagina(Long id) {
        log.info("[WORDPRESS] Obteniendo página id={}", id);

        try {
            WordPressPageResponse pagina = wordpressRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("rest_route", "/wp/v2/pages/" + id)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Página WordPress no encontrada: " + id);
                    })
                    .body(WordPressPageResponse.class);

            if (pagina == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Página WordPress no encontrada: " + id);
            }

            log.info("[WORDPRESS] Página obtenida: id={} title={}", pagina.getId(), pagina.getTitle());
            return pagina;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("[WORDPRESS] Error al obtener página {}: {}", id, ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar WordPress: " + ex.getMessage(),
                    ex);
        }
    }

    public WordPressPageEditResponse obtenerPaginaEdit(Long id, String bearerToken) {
        log.info("[WORDPRESS] Obteniendo página edit id={}", id);

        try {
            WordPressPageEditResponse pagina = wordpressRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("rest_route", "/wp/v2/pages/" + id)
                            .queryParam("context", "edit")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Página plantilla WordPress no encontrada: " + id);
                    })
                    .body(WordPressPageEditResponse.class);

            if (pagina == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Página plantilla WordPress no encontrada: " + id);
            }

            return pagina;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("[WORDPRESS] Error al obtener página edit {}: {}", id, ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar WordPress (edit): " + ex.getMessage(),
                    ex);
        }
    }

    public WordPressMediaResponse subirMedia(byte[] bytes, String filename, String mimeType, String bearerToken) {
        log.info("[WORDPRESS] Subiendo media filename={} bytes={}", filename, bytes == null ? 0 : bytes.length);

        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo de imagen vacío");
        }

        try {
            // WP REST /media acepta body binario + Content-Disposition (evita multipart/reactive-streams).
            String safeName = filename.replace("\"", "");
            MediaType contentType = mimeType != null
                    ? MediaType.parseMediaType(mimeType)
                    : MediaType.APPLICATION_OCTET_STREAM;

            WordPressMediaResponse media = wordpressRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("rest_route", "/wp/v2/media")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "\"")
                    .contentType(contentType)
                    .body(bytes)
                    .retrieve()
                    .body(WordPressMediaResponse.class);

            if (media == null || media.getId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "WordPress media: respuesta sin id");
            }

            log.info("[WORDPRESS] Media subido id={}", media.getId());
            return media;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("[WORDPRESS] Error al subir media: {}", ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo subir media a WordPress: " + ex.getMessage(),
                    ex);
        }
    }

    public WordPressPageEditResponse crearPagina(Map<String, Object> body, String bearerToken) {
        log.info("[WORDPRESS] Creando página status={}", body.get("status"));

        try {
            WordPressPageEditResponse creada = wordpressRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("rest_route", "/wp/v2/pages")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(WordPressPageEditResponse.class);

            if (creada == null || creada.getId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "WordPress pages: respuesta sin id");
            }

            log.info("[WORDPRESS] Página creada id={} status={}", creada.getId(), creada.getStatus());
            return creada;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("[WORDPRESS] Error al crear página: {}", ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo crear página en WordPress: " + ex.getMessage(),
                    ex);
        }
    }

    public WordPressPageEditResponse actualizarPagina(Long id, Map<String, Object> body, String bearerToken) {
        log.info("[WORDPRESS] Actualizando página id={}", id);

        try {
            WordPressPageEditResponse actualizada = wordpressRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("rest_route", "/wp/v2/pages/" + id)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Página WordPress no encontrada: " + id);
                    })
                    .body(WordPressPageEditResponse.class);

            if (actualizada == null || actualizada.getId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "WordPress pages update: respuesta sin id");
            }

            log.info("[WORDPRESS] Página actualizada id={}", actualizada.getId());
            return actualizada;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("[WORDPRESS] Error al actualizar página {}: {}", id, ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo actualizar página en WordPress: " + ex.getMessage(),
                    ex);
        }
    }

    /**
     * Envía la página a la Papelera (trash). No usa {@code force=true} (no borra definitivamente).
     * Si la página ya no existe (404) o ya está en trash (410), se considera OK para reintentos.
     */
    public WordPressPageEditResponse moverPaginaAPapelera(Long id, String bearerToken) {
        log.info("[WORDPRESS] Moviendo página a Papelera id={}", id);

        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page id es obligatorio");
        }

        try {
            WordPressPageEditResponse trashed = wordpressRestClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("rest_route", "/wp/v2/pages/" + id)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .retrieve()
                    .body(WordPressPageEditResponse.class);

            if (trashed != null && trashed.getId() != null) {
                log.info(
                        "[WORDPRESS] Página en Papelera id={} status={}",
                        trashed.getId(), trashed.getStatus());
            }
            return trashed;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            int code = ex.getStatusCode().value();
            if (code == 404 || code == 410) {
                log.warn(
                        "[WORDPRESS] Página id={} ya no disponible para trash (HTTP {}); se continúa",
                        id, code);
                return null;
            }
            log.error(
                    "[WORDPRESS] Error HTTP {} al mover página {} a Papelera: {}",
                    code, id, ex.getResponseBodyAsString());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo enviar la página a Papelera en WordPress (HTTP " + code + "): "
                            + ex.getResponseBodyAsString(),
                    ex);
        } catch (RestClientException ex) {
            log.error("[WORDPRESS] Error al mover página {} a Papelera: {}", id, ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo enviar la página a Papelera en WordPress: " + ex.getMessage(),
                    ex);
        }
    }

    /**
     * Mu-plugin Inmo360: fuerza Document::save de Elementor (regenera post_content).
     */
    public WordPressElementorRebuildResponse rebuildElementor(Long pageId, String bearerToken) {
        log.info("[WORDPRESS] Elementor rebuild pageId={}", pageId);

        try {
            WordPressElementorRebuildResponse response = wordpressRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("rest_route", elementorRebuildRoute)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("page_id", pageId))
                    .retrieve()
                    .body(WordPressElementorRebuildResponse.class);

            if (response == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Elementor rebuild: respuesta vacía");
            }

            if (!Boolean.TRUE.equals(response.getOk())) {
                String msg = response.getMessage() != null
                        ? response.getMessage()
                        : "rebuild devolvió ok=false";
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Elementor rebuild falló: " + msg);
            }

            log.info("[WORDPRESS] Elementor rebuild OK pageId={}", pageId);
            return response;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.error(
                    "[WORDPRESS] Elementor rebuild HTTP {} pageId={}: {}",
                    ex.getStatusCode().value(), pageId, ex.getResponseBodyAsString());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Elementor rebuild HTTP " + ex.getStatusCode().value()
                            + ": " + ex.getResponseBodyAsString(),
                    ex);
        } catch (RestClientException ex) {
            log.error("[WORDPRESS] Error Elementor rebuild pageId={}: {}", pageId, ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo ejecutar Elementor rebuild: " + ex.getMessage(),
                    ex);
        }
    }
}
