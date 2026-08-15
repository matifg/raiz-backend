package com.raiz.bakcend.integration.wordpress.auth;

import com.raiz.bakcend.integration.wordpress.dto.WordPressJwtTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Obtiene y cachea el JWT del plugin "JWT Authentication for WP-API".
 * Usa credenciales de wp-admin (no Application Password).
 */
@Service
public class WordPressJwtAuthService {

    private static final Logger log = LoggerFactory.getLogger(WordPressJwtAuthService.class);
    private static final long CACHE_TTL_SECONDS = 50 * 60;

    private final RestClient wordpressRestClient;
    private final String username;
    private final String password;

    private final AtomicReference<CachedToken> cached = new AtomicReference<>();

    public WordPressJwtAuthService(
            @Qualifier("wordpressRestClient") RestClient wordpressRestClient,
            @Value("${wordpress.auth.username:}") String username,
            @Value("${wordpress.auth.password:}") String password) {
        this.wordpressRestClient = wordpressRestClient;
        this.username = username;
        this.password = password;
    }

    public String getToken() {
        CachedToken current = cached.get();
        if (current != null && Instant.now().isBefore(current.expiresAt())) {
            return current.token();
        }
        synchronized (this) {
            current = cached.get();
            if (current != null && Instant.now().isBefore(current.expiresAt())) {
                return current.token();
            }
            String token = login();
            cached.set(new CachedToken(token, Instant.now().plusSeconds(CACHE_TTL_SECONDS)));
            return token;
        }
    }

    public void invalidate() {
        cached.set(null);
    }

    private String login() {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "WordPress auth no configurado: WORDPRESS_USERNAME / WORDPRESS_PASSWORD");
        }

        try {
            log.info("[WORDPRESS-AUTH] Solicitando JWT (usuario={})", username);
            WordPressJwtTokenResponse response = wordpressRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("rest_route", "/jwt-auth/v1/token")
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "username", username,
                            "password", password))
                    .retrieve()
                    .body(WordPressJwtTokenResponse.class);

            if (response == null || !StringUtils.hasText(response.getToken())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "WordPress JWT: respuesta sin token");
            }

            log.info("[WORDPRESS-AUTH] JWT obtenido");
            return response.getToken();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("[WORDPRESS-AUTH] Error al autenticar: {}", ex.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo autenticar contra WordPress: " + ex.getMessage(),
                    ex);
        }
    }

    private record CachedToken(String token, Instant expiresAt) {
    }
}
