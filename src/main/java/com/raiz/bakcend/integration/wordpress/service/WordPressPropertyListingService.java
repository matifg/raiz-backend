package com.raiz.bakcend.integration.wordpress.service;

import com.raiz.bakcend.integration.wordpress.client.WordPressClient;
import com.raiz.bakcend.integration.wordpress.dto.WordPressPageEditResponse;
import com.raiz.bakcend.model.Propiedad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agrega una card al carousel estático de la página "Casas" (Elementor bdt-static-carousel)
 * y fuerza Document::save vía mu-plugin para sincronizar post_content.
 */
@Service
public class WordPressPropertyListingService {

    private static final Logger log = LoggerFactory.getLogger(WordPressPropertyListingService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    /** Campo custom en carousel_items; Element Pack ignora claves desconocidas en el front. */
    static final String CARD_PROPIEDAD_ID_KEY = "inmo360_propiedad_id";

    private final WordPressClient wordPressClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final Long casasPageId;
    private final String casasCarouselId;

    public WordPressPropertyListingService(
            WordPressClient wordPressClient,
            ObjectMapper objectMapper,
            @Value("${wordpress.api.base-url}") String baseUrl,
            @Value("${wordpress.listing.casas-page-id}") Long casasPageId,
            @Value("${wordpress.listing.casas-carousel-id}") String casasCarouselId) {
        this.wordPressClient = wordPressClient;
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.casasPageId = casasPageId;
        this.casasCarouselId = casasCarouselId;
    }

    /**
     * Append de card al carousel Casas + rebuild Elementor.
     * Idempotente por {@code inmo360_propiedad_id} y/o readmore_link → page_id.
     *
     * @return true si meta + rebuild OK (o card ya existía y rebuild OK)
     */
    public boolean agregarCardCasas(
            Propiedad propiedad,
            Long wordpressPageId,
            WordPressMediaUploadService.UploadedMedia portada,
            String bearerToken) {
        if (propiedad == null || wordpressPageId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "propiedad y wordpressPageId son obligatorios para el listado Casas");
        }
        if (propiedad.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "propiedad.id es obligatorio para el listado Casas");
        }
        if (portada == null || portada.id() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Se requiere la primera imagen subida (media) para la card de Casas");
        }

        String propiedadIdStr = propiedad.getId().toString();
        String fichaUrl = baseUrl + "/?page_id=" + wordpressPageId;
        WordPressPageEditResponse listado = wordPressClient.obtenerPaginaEdit(casasPageId, bearerToken);
        if (listado.getMeta() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Página Casas sin meta Elementor");
        }

        JsonNode rawData = listado.getMeta().get("_elementor_data");
        if (rawData == null || rawData.isNull() || !StringUtils.hasText(rawData.asText())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Página Casas sin meta._elementor_data");
        }

        try {
            JsonNode root = objectMapper.readTree(rawData.asText());
            ObjectNode carousel = encontrarCarousel(root, casasCarouselId);
            if (carousel == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "No se encontró bdt-static-carousel id=" + casasCarouselId
                                + " en página " + casasPageId);
            }

            ObjectNode settings = carousel.has("settings") && carousel.get("settings").isObject()
                    ? (ObjectNode) carousel.get("settings")
                    : carousel.putObject("settings");

            ArrayNode items;
            JsonNode existing = settings.get("carousel_items");
            if (existing != null && existing.isArray()) {
                items = (ArrayNode) existing;
            } else {
                items = objectMapper.createArrayNode();
                settings.set("carousel_items", items);
            }

            ObjectNode cardExistente = encontrarCard(items, wordpressPageId, fichaUrl, propiedadIdStr);
            boolean mutoMeta = false;
            if (cardExistente == null) {
                items.add(construirItem(propiedad, fichaUrl, portada));
                mutoMeta = true;
                log.info(
                        "[WORDPRESS-LISTING] Card agregada a Casas pageId={} carousel={} "
                                + "propiedadId={} fichaPageId={} items={}",
                        casasPageId, casasCarouselId, propiedadIdStr, wordpressPageId, items.size());
            } else {
                mutoMeta = actualizarCardSiCorresponde(cardExistente, propiedad, fichaUrl, portada);
                if (mutoMeta) {
                    log.info(
                            "[WORDPRESS-LISTING] Card reutilizada/actualizada Casas pageId={} "
                                    + "propiedadId={} fichaPageId={}",
                            casasPageId, propiedadIdStr, wordpressPageId);
                } else {
                    log.info(
                            "[WORDPRESS-LISTING] Card ya existe en Casas pageId={} carousel={} "
                                    + "propiedadId={} fichaPageId={}",
                            casasPageId, casasCarouselId, propiedadIdStr, wordpressPageId);
                }
            }

            if (mutoMeta) {
                String elementorData = objectMapper.writeValueAsString(root);
                Map<String, Object> meta = new HashMap<>();
                meta.put("_elementor_data", elementorData);

                Map<String, Object> body = new HashMap<>();
                body.put("meta", meta);

                wordPressClient.actualizarPagina(casasPageId, body, bearerToken);
                esperarCardVisibleEnMeta(
                        casasPageId, wordpressPageId, fichaUrl, propiedadIdStr, bearerToken);
            }

            // Siempre después de la mutación (o si ya existía): regenerar post_content (sync, espera HTTP).
            wordPressClient.rebuildElementor(casasPageId, bearerToken);
            log.info(
                    "[WORDPRESS-LISTING] Elementor rebuild OK pageId={} fichaPageId={}",
                    casasPageId, wordpressPageId);
            return true;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo actualizar listado Casas: " + ex.getMessage(),
                    ex);
        }
    }

    /**
     * Elimina todas las cards de Casas asociadas a la propiedad (por {@code inmo360_propiedad_id})
     * o, como fallback, al {@code wordpressPageId} / URL {@code page_id=...}. Luego rebuild Elementor.
     *
     * @return cantidad de cards eliminadas
     */
    public int eliminarCardsCasas(UUID propiedadId, Long wordpressPageId, String bearerToken) {
        if (propiedadId == null || wordpressPageId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "propiedadId y wordpressPageId son obligatorios para eliminar cards Casas");
        }

        String propiedadIdStr = propiedadId.toString();
        String fichaUrl = baseUrl + "/?page_id=" + wordpressPageId;
        WordPressPageEditResponse listado = wordPressClient.obtenerPaginaEdit(casasPageId, bearerToken);
        if (listado.getMeta() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Página Casas sin meta Elementor");
        }

        JsonNode rawData = listado.getMeta().get("_elementor_data");
        if (rawData == null || rawData.isNull() || !StringUtils.hasText(rawData.asText())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Página Casas sin meta._elementor_data");
        }

        try {
            JsonNode root = objectMapper.readTree(rawData.asText());
            ObjectNode carousel = encontrarCarousel(root, casasCarouselId);
            if (carousel == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "No se encontró bdt-static-carousel id=" + casasCarouselId
                                + " en página " + casasPageId);
            }

            ObjectNode settings = carousel.has("settings") && carousel.get("settings").isObject()
                    ? (ObjectNode) carousel.get("settings")
                    : carousel.putObject("settings");

            ArrayNode items;
            JsonNode existing = settings.get("carousel_items");
            if (existing != null && existing.isArray()) {
                items = (ArrayNode) existing;
            } else {
                items = objectMapper.createArrayNode();
                settings.set("carousel_items", items);
            }

            List<Integer> indicesAEliminar = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                JsonNode item = items.get(i);
                if (item != null && item.isObject()
                        && cardCorrespondeAPropiedad(
                                (ObjectNode) item, wordpressPageId, fichaUrl, propiedadIdStr)) {
                    indicesAEliminar.add(i);
                }
            }

            // Eliminar de atrás hacia adelante para no invalidar índices.
            for (int i = indicesAEliminar.size() - 1; i >= 0; i--) {
                items.remove(indicesAEliminar.get(i));
            }

            int eliminadas = indicesAEliminar.size();
            if (eliminadas > 0) {
                String elementorData = objectMapper.writeValueAsString(root);
                Map<String, Object> meta = new HashMap<>();
                meta.put("_elementor_data", elementorData);

                Map<String, Object> body = new HashMap<>();
                body.put("meta", meta);

                wordPressClient.actualizarPagina(casasPageId, body, bearerToken);
                log.info(
                        "[WORDPRESS-LISTING] Cards eliminadas de Casas pageId={} propiedadId={} "
                                + "fichaPageId={} eliminadas={} itemsRestantes={}",
                        casasPageId, propiedadIdStr, wordpressPageId, eliminadas, items.size());
            } else {
                log.info(
                        "[WORDPRESS-LISTING] No había cards Casas para propiedadId={} fichaPageId={}",
                        propiedadIdStr, wordpressPageId);
            }

            wordPressClient.rebuildElementor(casasPageId, bearerToken);
            log.info(
                    "[WORDPRESS-LISTING] Elementor rebuild OK tras unpublish pageId={} fichaPageId={}",
                    casasPageId, wordpressPageId);
            return eliminadas;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo eliminar cards del listado Casas: " + ex.getMessage(),
                    ex);
        }
    }

    /**
     * Tras POST meta, relee context=edit hasta ver la card (object-cache lag en WP).
     */
    private void esperarCardVisibleEnMeta(
            Long listadoPageId,
            Long wordpressPageId,
            String fichaUrl,
            String propiedadIdStr,
            String bearerToken) {
        final int maxAttempts = 6;
        final long sleepMs = 400L;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            WordPressPageEditResponse listado = wordPressClient.obtenerPaginaEdit(listadoPageId, bearerToken);
            JsonNode rawData = listado.getMeta() != null ? listado.getMeta().get("_elementor_data") : null;
            if (rawData != null && !rawData.isNull() && StringUtils.hasText(rawData.asText())) {
                try {
                    JsonNode root = objectMapper.readTree(rawData.asText());
                    ObjectNode carousel = encontrarCarousel(root, casasCarouselId);
                    if (carousel != null) {
                        JsonNode existing = carousel.path("settings").path("carousel_items");
                        if (existing.isArray()
                                && encontrarCard(
                                        (ArrayNode) existing, wordpressPageId, fichaUrl, propiedadIdStr)
                                        != null) {
                            log.info(
                                    "[WORDPRESS-LISTING] Meta confirma card fichaPageId={} (attempt={})",
                                    wordpressPageId, attempt);
                            return;
                        }
                    }
                } catch (Exception ex) {
                    log.warn(
                            "[WORDPRESS-LISTING] No se pudo parsear meta en attempt={}: {}",
                            attempt, ex.getMessage());
                }
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "Interrumpido esperando visibilidad de card en meta Casas",
                            ie);
                }
            }
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "La card fichaPageId=" + wordpressPageId
                        + " no aparece aún en meta._elementor_data de página "
                        + listadoPageId + " tras actualizar; rebuild no ejecutado");
    }

    private ObjectNode construirItem(
            Propiedad propiedad,
            String fichaUrl,
            WordPressMediaUploadService.UploadedMedia portada) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("_id", nuevoHexId());
        item.put(CARD_PROPIEDAD_ID_KEY, propiedad.getId().toString());
        item.put("title", propiedad.getTitulo().trim());
        item.put("sub_title", resolverSubTitle(propiedad));
        item.put("text", aHtmlParrafo(propiedad.getDescripcion()));

        ObjectNode image = item.putObject("image");
        image.put("id", portada.id());
        image.put("url", portada.url() != null ? portada.url() : "");
        image.put("alt", "");
        image.put("source", "library");
        image.put("size", "");

        ObjectNode link = item.putObject("readmore_link");
        link.put("url", fichaUrl);
        link.put("is_external", "on");
        link.put("nofollow", "");
        link.put("custom_attributes", "");
        return item;
    }

    /**
     * Actualiza campos esenciales si la card ya existe (p.ej. misma propiedadId con otra ficha URL).
     *
     * @return true si hubo cambios que requieren POST meta
     */
    private boolean actualizarCardSiCorresponde(
            ObjectNode card,
            Propiedad propiedad,
            String fichaUrl,
            WordPressMediaUploadService.UploadedMedia portada) {
        boolean changed = false;
        String propiedadIdStr = propiedad.getId().toString();
        if (!propiedadIdStr.equals(card.path(CARD_PROPIEDAD_ID_KEY).asText(""))) {
            card.put(CARD_PROPIEDAD_ID_KEY, propiedadIdStr);
            changed = true;
        }
        String titulo = propiedad.getTitulo().trim();
        if (!titulo.equals(card.path("title").asText(""))) {
            card.put("title", titulo);
            changed = true;
        }
        ObjectNode link = card.has("readmore_link") && card.get("readmore_link").isObject()
                ? (ObjectNode) card.get("readmore_link")
                : card.putObject("readmore_link");
        if (!fichaUrl.equals(link.path("url").asText(""))) {
            link.put("url", fichaUrl);
            link.put("is_external", "on");
            changed = true;
        }
        ObjectNode image = card.has("image") && card.get("image").isObject()
                ? (ObjectNode) card.get("image")
                : card.putObject("image");
        long mediaId = portada.id();
        if (image.path("id").asLong(0L) != mediaId) {
            image.put("id", mediaId);
            image.put("url", portada.url() != null ? portada.url() : "");
            image.put("source", "library");
            changed = true;
        }
        return changed;
    }

    private ObjectNode encontrarCard(
            ArrayNode items, Long wordpressPageId, String fichaUrl, String propiedadIdStr) {
        String pageToken = "page_id=" + wordpressPageId;
        ObjectNode porPageId = null;
        ObjectNode porPropiedadId = null;

        for (JsonNode item : items) {
            if (item == null || !item.isObject()) {
                continue;
            }
            ObjectNode obj = (ObjectNode) item;

            if (StringUtils.hasText(propiedadIdStr)
                    && propiedadIdStr.equals(obj.path(CARD_PROPIEDAD_ID_KEY).asText(""))) {
                porPropiedadId = obj;
            }

            JsonNode link = obj.get("readmore_link");
            if (link != null && !link.isNull()) {
                String url = link.path("url").asText("");
                if (StringUtils.hasText(url) && (url.equals(fichaUrl) || url.contains(pageToken))) {
                    porPageId = obj;
                }
            }
        }

        // Preferir match por propiedad (idempotencia real); page_id como respaldo (cards viejas).
        if (porPropiedadId != null) {
            return porPropiedadId;
        }
        return porPageId;
    }

    private boolean cardCorrespondeAPropiedad(
            ObjectNode item, Long wordpressPageId, String fichaUrl, String propiedadIdStr) {
        if (StringUtils.hasText(propiedadIdStr)
                && propiedadIdStr.equals(item.path(CARD_PROPIEDAD_ID_KEY).asText(""))) {
            return true;
        }
        if (wordpressPageId == null) {
            return false;
        }
        String pageToken = "page_id=" + wordpressPageId;
        JsonNode link = item.get("readmore_link");
        if (link == null || link.isNull()) {
            return false;
        }
        String url = link.path("url").asText("");
        return StringUtils.hasText(url) && (url.equals(fichaUrl) || url.contains(pageToken));
    }

    private ObjectNode encontrarCarousel(JsonNode node, String carouselId) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            if ("widget".equals(text(obj, "elType"))
                    && "bdt-static-carousel".equals(text(obj, "widgetType"))
                    && carouselId.equals(text(obj, "id"))) {
                return obj;
            }
            for (JsonNode child : obj) {
                ObjectNode found = encontrarCarousel(child, carouselId);
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                ObjectNode found = encontrarCarousel(child, carouselId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private String text(ObjectNode obj, String field) {
        JsonNode n = obj.get(field);
        return n == null || n.isNull() ? null : n.asText();
    }

    private String resolverSubTitle(Propiedad propiedad) {
        if (StringUtils.hasText(propiedad.getOperacion())) {
            return propiedad.getOperacion().trim();
        }
        return "Propiedad";
    }

    private String aHtmlParrafo(String descripcion) {
        String value = descripcion == null ? "" : descripcion.trim();
        return "<p>" + escaparHtml(value) + "</p>";
    }

    private String escaparHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String nuevoHexId() {
        char[] id = new char[7];
        for (int i = 0; i < id.length; i++) {
            id[i] = HEX[RANDOM.nextInt(HEX.length)];
        }
        return new String(id);
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
