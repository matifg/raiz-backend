package com.raiz.bakcend.controller;

import com.raiz.bakcend.service.AgenteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.raiz.bakcend.dto.AgenteAdminPageResponse;
import org.springframework.web.bind.annotation.RequestParam;
import com.raiz.bakcend.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.dto.UsuarioResponseDTO;


import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import java.util.UUID;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final AgenteService agenteService;
    private final UsuarioRepository usuarioRepository;

    public AdminController(AgenteService agenteService, UsuarioRepository usuarioRepository) {
        this.agenteService = agenteService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/agentes")
    public ResponseEntity<?> listarAgentes(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        Authentication authentication
    ) {
        logger.info("--- INICIO /admin/agentes ---");
        Usuario usuario = null;
        try {
            usuario = validarAdmin(authentication);
        } catch (RuntimeException e) {
            logger.warn(e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.status(401).body(e.getMessage());
            } else {
                return ResponseEntity.status(403).body(e.getMessage());
            }
        }
        logger.info("Usuario {} autorizado como ADMIN", usuario.getEmail());
        var result = agenteService.listarAgentesAdminPaginado(page, size);
        logger.info("--- FIN /admin/agentes ---");
        return ResponseEntity.ok(result);
    }

    // DTO para el body
    public static class MembresiaRequest {
        public Boolean membresiaActiva;
    }

    @PutMapping("/usuarios/{id}/membresia")
    public ResponseEntity<?> actualizarMembresia(
        @PathVariable("id") UUID id,
        @RequestBody MembresiaRequest request,
        Authentication authentication
    ) {
        try {
            validarAdmin(authentication);
        } catch (RuntimeException e) {
            logger.warn(e.getMessage());
            if (e.getMessage().contains("no encontrado")) {
                return ResponseEntity.status(401).body(e.getMessage());
            } else {
                return ResponseEntity.status(403).body(e.getMessage());
            }
        }

        // 2. Buscar usuario por id
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(404).body("Usuario no encontrado");
        }

        // 3. Actualizar membresiaActiva
        usuario.setMembresiaActiva(request.membresiaActiva);

    // 4. Guardar usuario
    usuarioRepository.save(usuario);

        // 5. Devolver usuario actualizado como DTO
        UsuarioResponseDTO response = new UsuarioResponseDTO(
            usuario.getId(),
            usuario.getNombre(),
            usuario.getApellido(),
            usuario.getEmail(),
            usuario.getTelefono(),
            usuario.getRol(),
            usuario.getActivo(),
            usuario.getMembresiaActiva()
        );
        return ResponseEntity.ok(response);
    }
    // Método privado para validar ADMIN
    private Usuario validarAdmin(Authentication authentication) {
        logger.info("Validando usuario ADMIN...");
        String userId = authentication.getName();
        logger.info("Authentication name (userId): {}", userId);
        Usuario usuario = null;
        try {
            usuario = usuarioRepository.findById(UUID.fromString(userId)).orElse(null);
        } catch (Exception e) {
            logger.error("Error convirtiendo userId a UUID o buscando usuario: {}", e.getMessage());
        }
        if (usuario == null) {
            throw new RuntimeException("Usuario no encontrado para UUID: " + userId);
        }
        logger.info("Usuario encontrado: {} (rol: {})", usuario.getEmail(), usuario.getRol());
        if (!"ADMIN".equalsIgnoreCase(usuario.getRol())) {
            throw new RuntimeException("No autorizado: usuario " + usuario.getEmail() + " no tiene rol ADMIN (rol actual: " + usuario.getRol() + ")");
        }
        return usuario;
    }
}
