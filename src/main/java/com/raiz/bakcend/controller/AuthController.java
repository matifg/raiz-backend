package com.raiz.bakcend.controller;

import com.raiz.bakcend.dto.LoginRequest;
import com.raiz.bakcend.dto.CreateUsuarioRequest;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.repository.UsuarioRepository;
import com.raiz.bakcend.repository.AgenteRepository;
import com.raiz.bakcend.service.AuthService;
import com.raiz.bakcend.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AgenteRepository agenteRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Usuario no encontrado");
        }
        Usuario usuario = usuarioOpt.get();
        boolean passwordCorrecto = passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash());
        if (!passwordCorrecto) {
            return ResponseEntity.status(401).body("Password incorrecto");
        }
        String token = jwtService.generateToken(usuario.getId().toString());

        // Si es agente, buscar el agente por usuario_id
        if ("AGENTE".equalsIgnoreCase(usuario.getRol())) {
            Optional<Agente> agenteOpt = agenteRepository.findByUsuarioId(usuario.getId());
            if (agenteOpt.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "usuario", usuario,
                    "agente", agenteOpt.get(),
                    "token", token
                ));
            }
        }
        // Si no es agente, solo devuelve el usuario y el token
        return ResponseEntity.ok(Map.of(
            "usuario", usuario,
            "token", token
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody CreateUsuarioRequest request) {
        try {
            Usuario usuario = authService.register(request);
            return ResponseEntity.ok(usuario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno");
        }
    }
}
