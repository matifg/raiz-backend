package com.raiz.bakcend.controller;

import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.service.AgenteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/agentes")
public class AgenteController {

    private final AgenteService agenteService;

    public AgenteController(AgenteService agenteService) {
        this.agenteService = agenteService;
    }

    /**
     * Devuelve los datos del agente autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getAgenteMe(Authentication authentication) {
        // Obtener el email del usuario autenticado
        String email = authentication.getName();

        Optional<Agente> agenteOpt = agenteService.getAgenteForUserEmail(email);

        if (agenteOpt.isEmpty()) {
            return ResponseEntity.status(403).body("No autorizado o no es agente");
        }

        return ResponseEntity.ok(agenteOpt.get());
    }
}