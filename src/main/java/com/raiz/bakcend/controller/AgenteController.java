package com.raiz.bakcend.controller;

import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.service.AgenteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.raiz.bakcend.dto.AgenteAdminPageResponse;

import java.util.List;
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
        // Obtener el userId (UUID) del usuario autenticado
        String userId = authentication.getName();
        java.util.UUID usuarioId = java.util.UUID.fromString(userId);

        Optional<Agente> agenteOpt = agenteService.getAgenteByUsuarioId(usuarioId);

        if (agenteOpt.isEmpty()) {
            return ResponseEntity.status(403).body("No autorizado o no es agente");
        }

        return ResponseEntity.ok(agenteOpt.get());
    }

    @GetMapping("/agentes")
    public AgenteAdminPageResponse listarAgentes(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return agenteService.listarAgentesAdminPaginado(page, size);
    }
}