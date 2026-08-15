package com.raiz.bakcend.integration.wordpress.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

/**
 * Clona _elementor_data de la plantilla, regenera IDs y reconstruye static_tabs_item.
 */
@Service
public class WordPressElementorCloneService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final ObjectMapper objectMapper;

    public WordPressElementorCloneService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String clonar(
            JsonNode meta,
            String slideTitle,
            String descripcionHtml,
            List<WordPressMediaUploadService.UploadedMedia> medias) {
        if (meta == null || meta.isNull()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "La plantilla WordPress no tiene meta Elementor");
        }

        JsonNode rawData = meta.get("_elementor_data");
        if (rawData == null || rawData.isNull() || !StringUtils.hasText(rawData.asText())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "La plantilla WordPress no tiene meta._elementor_data");
        }

        try {
            JsonNode root = objectMapper.readTree(rawData.asText());
            if (!root.isArray()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "_elementor_data de plantilla no es un array");
            }

            ArrayNode cloned = (ArrayNode) root.deepCopy();
            regenerarIds(cloned);
            reemplazarStaticTabs(cloned, slideTitle, descripcionHtml, medias);
            return objectMapper.writeValueAsString(cloned);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo clonar _elementor_data: " + ex.getMessage(),
                    ex);
        }
    }

    public Object pageSettingsFromMeta(JsonNode meta) {
        if (meta == null || meta.isNull()) {
            return Map.of("hide_title", "yes");
        }
        JsonNode settings = meta.get("_elementor_page_settings");
        if (settings == null || settings.isNull()) {
            return Map.of("hide_title", "yes");
        }
        if (settings.isTextual()) {
            try {
                return objectMapper.readTree(settings.asText());
            } catch (Exception ex) {
                return Map.of("hide_title", "yes");
            }
        }
        return settings;
    }

    private void regenerarIds(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            if (obj.has("id") && obj.get("id").isTextual()) {
                obj.put("id", nuevoElementorId());
            }
            // Jackson 3: fields() → properties()
            for (Map.Entry<String, JsonNode> entry : obj.properties()) {
                regenerarIds(entry.getValue());
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                regenerarIds(child);
            }
        }
    }

    private void reemplazarStaticTabs(
            ArrayNode root,
            String slideTitle,
            String descripcionHtml,
            List<WordPressMediaUploadService.UploadedMedia> medias) {
        ObjectNode widget = encontrarWidget(root, "bdt-static-grid-tab");
        if (widget == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Plantilla sin widget bdt-static-grid-tab");
        }

        ObjectNode settings = widget.has("settings") && widget.get("settings").isObject()
                ? (ObjectNode) widget.get("settings")
                : widget.putObject("settings");

        ArrayNode tabs = objectMapper.createArrayNode();
        for (WordPressMediaUploadService.UploadedMedia media : medias) {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("_id", nuevoElementorId());
            item.put("title", slideTitle);
            item.put("text", descripcionHtml);

            ObjectNode image = item.putObject("image");
            image.put("id", media.id());
            image.put("url", media.url() != null ? media.url() : "");
            image.put("alt", "");
            image.put("source", "library");
            image.put("size", "");

            tabs.add(item);
        }
        settings.set("static_tabs_item", tabs);
    }

    private ObjectNode encontrarWidget(JsonNode node, String widgetType) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            if ("widget".equals(text(obj, "elType"))
                    && widgetType.equals(text(obj, "widgetType"))) {
                return obj;
            }
            for (JsonNode child : obj) {
                ObjectNode found = encontrarWidget(child, widgetType);
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                ObjectNode found = encontrarWidget(child, widgetType);
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

    private String nuevoElementorId() {
        char[] id = new char[7];
        for (int i = 0; i < id.length; i++) {
            id[i] = HEX[RANDOM.nextInt(HEX.length)];
        }
        return new String(id);
    }
}
