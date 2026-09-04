package com.raiz.bakcend.service;

import com.raiz.bakcend.model.TokenVerificacion;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.repository.TokenVerificacionRepository;
import com.raiz.bakcend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class TokenVerificacionService {

    private static final Logger log = LoggerFactory.getLogger(TokenVerificacionService.class);
    private static final Duration VALIDEZ_TOKEN = Duration.ofHours(24);

    private final TokenVerificacionRepository tokenVerificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public TokenVerificacionService(
            TokenVerificacionRepository tokenVerificacionRepository,
            UsuarioRepository usuarioRepository) {
        this.tokenVerificacionRepository = tokenVerificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public TokenVerificacion crearToken(Usuario usuario) {
        tokenVerificacionRepository.deleteByUsuarioAndUsadoFalse(usuario);

        UUID tokenUuid = UUID.randomUUID();
        TokenVerificacion tokenVerificacion = new TokenVerificacion();
        tokenVerificacion.setToken(tokenUuid);
        tokenVerificacion.setUsuario(usuario);
        tokenVerificacion.setFechaExpiracion(LocalDateTime.now().plus(VALIDEZ_TOKEN));
        tokenVerificacion.setUsado(false);
        TokenVerificacion saved = tokenVerificacionRepository.save(tokenVerificacion);
        log.info("[VERIFY_EMAIL] Token creado userId={} token={}", usuario.getId(), tokenUuid);
        return saved;
    }

    public Optional<TokenVerificacion> buscarPorToken(UUID token) {
        return tokenVerificacionRepository.findByToken(token);
    }

    @Transactional
    public String verificarCuenta(UUID tokenUuid) {
        log.info("[VERIFY_EMAIL] UUID recibido: {}", tokenUuid);

        Optional<TokenVerificacion> tokenOpt = tokenVerificacionRepository.findByToken(tokenUuid);
        log.info("[VERIFY_EMAIL] findByToken encontró registro: {}", tokenOpt.isPresent());

        if (tokenOpt.isEmpty()) {
            log.warn("[VERIFY_EMAIL] Token no encontrado");
            throw new IllegalArgumentException("Token de verificación inválido.");
        }

        TokenVerificacion token = tokenOpt.get();
        Usuario usuario = token.getUsuario();
        log.info("[VERIFY_EMAIL] token.isUsado(): {}", token.isUsado());
        log.info("[VERIFY_EMAIL] fechaExpiracion: {}", token.getFechaExpiracion());

        if (token.isUsado() && Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            log.info("[VERIFY_EMAIL] Cuenta ya verificada (idempotente)");
            return "La cuenta ya estaba verificada.";
        }

        if (token.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            log.warn("[VERIFY_EMAIL] Token expirado");
            throw new IllegalArgumentException("El enlace de verificación expiró.");
        }

        if (token.isUsado()) {
            log.warn("[VERIFY_EMAIL] Token ya usado");
            throw new IllegalArgumentException("Token de verificación inválido.");
        }

        usuario.setEmailVerificado(true);

        log.info("[VERIFY_EMAIL] Marcando usado=true para token: {}", tokenUuid);
        token.setUsado(true);

        usuarioRepository.save(usuario);
        tokenVerificacionRepository.save(token);
        log.info("[VERIFY_EMAIL] Token guardado con usado=true: {}", tokenUuid);

        return "Cuenta verificada correctamente.";
    }
}
