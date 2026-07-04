package com.raiz.bakcend.service;

import com.raiz.bakcend.model.PasswordResetToken;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String MENSAJE_SOLICITUD =
            "Si el email está registrado, recibirás instrucciones para restablecer tu contraseña.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenService passwordResetTokenService;
    private final PasswordResetEmailService passwordResetEmailService;
    private final BCryptPasswordEncoder passwordEncoder;

    public PasswordResetService(
            UsuarioRepository usuarioRepository,
            PasswordResetTokenService passwordResetTokenService,
            PasswordResetEmailService passwordResetEmailService,
            BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordResetTokenService = passwordResetTokenService;
        this.passwordResetEmailService = passwordResetEmailService;
        this.passwordEncoder = passwordEncoder;
    }

    public String solicitarRecuperacion(String email) {
        if (email == null || email.isBlank()) {
            return MENSAJE_SOLICITUD;
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email.trim());
        if (usuarioOpt.isEmpty()) {
            log.info("[PASSWORD_RESET] Solicitud para email no registrado");
            return MENSAJE_SOLICITUD;
        }

        Usuario usuario = usuarioOpt.get();
        PasswordResetToken token = passwordResetTokenService.crearToken(usuario);
        passwordResetEmailService.enviarEmailRecuperacion(usuario, token.getToken());
        log.info("[PASSWORD_RESET] Email de recuperación enviado userId={}", usuario.getId());

        return MENSAJE_SOLICITUD;
    }

    @Transactional
    public String restablecerPassword(UUID tokenUuid, String nuevaPassword) {
        if (tokenUuid == null) {
            throw new IllegalArgumentException("Token de recuperación inválido.");
        }
        if (nuevaPassword == null || nuevaPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }

        PasswordResetToken token = passwordResetTokenService.buscarPorToken(tokenUuid)
                .orElseThrow(() -> new IllegalArgumentException("Token de recuperación inválido."));

        if (token.isUsado()) {
            throw new IllegalArgumentException("Token de recuperación inválido.");
        }

        if (token.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El enlace de recuperación expiró.");
        }

        Usuario usuario = token.getUsuario();

        if (passwordEncoder.matches(nuevaPassword, usuario.getPasswordHash())) {
            throw new IllegalArgumentException("La nueva contraseña no puede ser igual a la actual.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        passwordResetTokenService.marcarComoUsado(token);

        log.info("[PASSWORD_RESET] Contraseña actualizada userId={}", usuario.getId());
        return "Contraseña actualizada correctamente.";
    }
}
