package com.raiz.bakcend.service;

import com.raiz.bakcend.config.BrevoProperties;
import com.raiz.bakcend.dto.brevo.BrevoEmailRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailService {

    private static final Pattern BREVO_MESSAGE_PATTERN =
            Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");

    private final RestClient brevoRestClient;
    private final BrevoProperties brevoProperties;

    public EmailService(
            @Qualifier("brevoRestClient") RestClient brevoRestClient,
            BrevoProperties brevoProperties) {
        this.brevoRestClient = brevoRestClient;
        this.brevoProperties = brevoProperties;
    }

    public void sendHtmlEmail(String to, String subject, String html) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("El destinatario es obligatorio.");
        }

        String senderEmail = brevoProperties.getSender().getEmail();
        if (senderEmail == null || senderEmail.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Remitente no configurado. Definí BREVO_SENDER_EMAIL.");
        }

        BrevoEmailRequest request = new BrevoEmailRequest();
        request.setSender(new BrevoEmailRequest.Sender(
                brevoProperties.getSender().getName(),
                senderEmail));
        request.setTo(List.of(new BrevoEmailRequest.Recipient(to.trim())));
        request.setSubject(subject);
        request.setHtmlContent(html);

        try {
            brevoRestClient.post()
                    .uri(brevoProperties.getApi().getUrl())
                    .body(request)
                    .exchange((req, res) -> {
                        if (res.getStatusCode().is2xxSuccessful()) {
                            return null;
                        }
                        throw toBrevoException(res.getBody(), res.getStatusCode().value());
                    });
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo enviar el email: " + ex.getMessage(),
                    ex);
        }
    }

    private ResponseStatusException toBrevoException(InputStream bodyStream, int statusCode) {
        String brevoMessage = leerMensajeBrevo(bodyStream);
        HttpStatus status = statusCode >= 400 && statusCode < 500
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.BAD_GATEWAY;
        return new ResponseStatusException(status, "Error al enviar email: " + brevoMessage);
    }

    private String leerMensajeBrevo(InputStream bodyStream) {
        if (bodyStream == null) {
            return "respuesta desconocida de Brevo";
        }
        try {
            String raw = new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);
            if (raw.isBlank()) {
                return "respuesta vacía de Brevo";
            }
            Matcher matcher = BREVO_MESSAGE_PATTERN.matcher(raw);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return raw;
        } catch (Exception ex) {
            return "no se pudo interpretar la respuesta de Brevo";
        }
    }
}
