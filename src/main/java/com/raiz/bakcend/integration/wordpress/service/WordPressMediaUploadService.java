package com.raiz.bakcend.integration.wordpress.service;

import com.raiz.bakcend.integration.wordpress.client.WordPressClient;
import com.raiz.bakcend.integration.wordpress.dto.WordPressMediaResponse;
import com.raiz.bakcend.model.ImagenPropiedad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class WordPressMediaUploadService {

    private static final Logger log = LoggerFactory.getLogger(WordPressMediaUploadService.class);

    private final WordPressClient wordPressClient;
    private final RestClient wordpressRestClient;

    public WordPressMediaUploadService(
            WordPressClient wordPressClient,
            @Qualifier("wordpressRestClient") RestClient wordpressRestClient) {
        this.wordPressClient = wordPressClient;
        this.wordpressRestClient = wordpressRestClient;
    }

    public List<UploadedMedia> subirImagenes(List<ImagenPropiedad> imagenes, String bearerToken) {
        List<UploadedMedia> result = new ArrayList<>();
        int index = 0;
        for (ImagenPropiedad imagen : imagenes) {
            index++;
            if (imagen == null || !StringUtils.hasText(imagen.getUrl())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Imagen sin URL en posición " + index);
            }
            byte[] bytes = leerBytes(imagen.getUrl());
            String filename = resolverFilename(imagen.getUrl(), index);
            String mime = resolverMime(filename);
            WordPressMediaResponse media = wordPressClient.subirMedia(bytes, filename, mime, bearerToken);
            result.add(new UploadedMedia(media.getId(), media.getSourceUrl()));
        }
        return result;
    }

    private byte[] leerBytes(String url) {
        Path local = resolverPathLocal(url);
        if (local != null) {
            try {
                if (!Files.isRegularFile(local)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Archivo local de imagen no encontrado: " + local);
                }
                log.info("[WORDPRESS-MEDIA] Leyendo archivo local {}", local);
                return Files.readAllBytes(local);
            } catch (IOException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "No se pudo leer imagen local: " + ex.getMessage(),
                        ex);
            }
        }

        try {
            log.info("[WORDPRESS-MEDIA] Descargando {}", url);
            byte[] body = wordpressRestClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(byte[].class);
            if (body == null || body.length == 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Imagen vacía o no descargable: " + url);
            }
            return body;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException | IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo descargar imagen: " + url + " (" + ex.getMessage() + ")",
                    ex);
        }
    }

    private Path resolverPathLocal(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("http://localhost") || lower.startsWith("http://127.0.0.1"))) {
            return null;
        }
        int idx = lower.indexOf("/uploads/");
        if (idx < 0) {
            return null;
        }
        String relative = url.substring(idx + 1);
        return Paths.get(relative).normalize();
    }

    private String resolverFilename(String url, int index) {
        try {
            String path = URI.create(url).getPath();
            if (path != null && path.contains("/")) {
                String name = path.substring(path.lastIndexOf('/') + 1);
                if (StringUtils.hasText(name)) {
                    return name;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // fallback abajo
        }
        return "propiedad-" + index + ".jpg";
    }

    private String resolverMime(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        }
        return MediaType.IMAGE_JPEG_VALUE;
    }

    public record UploadedMedia(Long id, String url) {
    }
}
