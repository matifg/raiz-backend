package com.raiz.bakcend.controller;

import com.raiz.bakcend.dto.ForgotPasswordRequest;
import com.raiz.bakcend.dto.LoginRequest;
import com.raiz.bakcend.dto.CreateUsuarioRequest;
import com.raiz.bakcend.dto.ResetPasswordRequest;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.model.Agente;
import com.raiz.bakcend.repository.UsuarioRepository;
import com.raiz.bakcend.repository.AgenteRepository;
import com.raiz.bakcend.service.AuthService;
import com.raiz.bakcend.service.JwtService;
import com.raiz.bakcend.service.PasswordResetService;
import com.raiz.bakcend.service.TokenVerificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.raiz.bakcend.dto.LoginResponseDTO;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

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

    @Autowired
    private TokenVerificacionService tokenVerificacionService;

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("[LOGIN] Intento de login email={}", request.getEmail());

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.getEmail());
        if (usuarioOpt.isEmpty()) {
            log.warn("[LOGIN] Usuario no encontrado email={}", request.getEmail());
            return ResponseEntity.status(401).body("Usuario no encontrado");
        }
        Usuario usuario = usuarioOpt.get();
        log.info("[LOGIN] Usuario encontrado id={} email={} emailVerificado={} activo={} rol={}",
                usuario.getId(),
                usuario.getEmail(),
                usuario.getEmailVerificado(),
                usuario.getActivo(),
                usuario.getRol());

        boolean passwordCorrecto = passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash());
        if (!passwordCorrecto) {
            log.warn("[LOGIN] Password incorrecto email={}", request.getEmail());
            return ResponseEntity.status(401).body("Password incorrecto");
        }

        if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            log.warn("[LOGIN] Login rechazado: email no verificado email={} userId={}",
                    usuario.getEmail(), usuario.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Debés verificar tu correo electrónico antes de iniciar sesión."));
        }

        log.info("[LOGIN] Email verificado, login permitido email={}", usuario.getEmail());

        String token = jwtService.generateToken(usuario.getId().toString());
        log.info("[LOGIN] JWT emitido email={} userId={}", usuario.getEmail(), usuario.getId());

        // Usar DTO en vez de Map
        LoginResponseDTO response = new LoginResponseDTO(
            token,
            usuario.getRol(),
            usuario.getMembresiaActiva(),
            usuario.getNombre()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody CreateUsuarioRequest request) {
        try {
            Usuario usuario = authService.register(request);
            return ResponseEntity.ok(usuario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno");
        }
    }

    @GetMapping("/verificar-email")
    public ResponseEntity<Map<String, String>> verificarEmail(@RequestParam UUID token) {
        String message = tokenVerificacionService.verificarCuenta(token);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/recuperar-password")
    public ResponseEntity<Map<String, String>> recuperarPassword(@RequestBody ForgotPasswordRequest request) {
        String message = passwordResetService.solicitarRecuperacion(request.getEmail());
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/restablecer-password")
    public ResponseEntity<Map<String, String>> restablecerPassword(@RequestBody ResetPasswordRequest request) {
        String message = passwordResetService.restablecerPassword(request.getToken(), request.getPassword());
        return ResponseEntity.ok(Map.of("message", message));
    }
}
