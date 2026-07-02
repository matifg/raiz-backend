package com.raiz.bakcend.controller;

import com.raiz.bakcend.dto.TestEmailRequest;
import com.raiz.bakcend.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class EmailTestController {

    private static final String TEST_SUBJECT = "Prueba Inmo360";
    private static final String TEST_HTML = """
            <h2>Bienvenido a Inmo360</h2>
            <p>La integración con Brevo funciona correctamente.</p>
            """;

    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/email")
    public ResponseEntity<Map<String, String>> enviarEmailPrueba(@RequestBody TestEmailRequest request) {
        emailService.sendHtmlEmail(request.getTo(), TEST_SUBJECT, TEST_HTML);
        return ResponseEntity.ok(Map.of("message", "Email enviado correctamente"));
    }
}
