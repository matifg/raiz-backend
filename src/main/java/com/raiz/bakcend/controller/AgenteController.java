package com.raiz.bakcend.controller;

import com.raiz.bakcend.dto.ActualizarPerfilAgenteRequest;
import com.raiz.bakcend.dto.AgenteAdminPageResponse;
import com.raiz.bakcend.dto.AgenteResponse;
import com.raiz.bakcend.service.AgenteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/agentes")
public class AgenteController {

    private final AgenteService agenteService;

    public AgenteController(AgenteService agenteService) {
        this.agenteService = agenteService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getAgenteMe(Authentication authentication) {
        UUID usuarioId = UUID.fromString(authentication.getName());

        return agenteService.obtenerPorUsuarioId(usuarioId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(403).body("No autorizado o no es agente"));
    }

    @PutMapping("/me")
    public AgenteResponse actualizarPerfil(
            Authentication authentication,
            @RequestBody ActualizarPerfilAgenteRequest request) {
        UUID usuarioId = UUID.fromString(authentication.getName());
        return agenteService.actualizarPerfil(usuarioId, request);
    }

    @GetMapping("/publico/{id}")
    public AgenteResponse obtenerPublico(@PathVariable UUID id) {
        return agenteService.obtenerPorId(id);
    }

    @GetMapping("/{id}")
    public AgenteResponse obtener(@PathVariable UUID id) {
        return agenteService.obtenerPorId(id);
    }

    @GetMapping("/agentes")
    public AgenteAdminPageResponse listarAgentes(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return agenteService.listarAgentesAdminPaginado(page, size);
    }
}
