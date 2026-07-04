package com.raiz.bakcend.service;

import com.raiz.bakcend.model.PasswordResetToken;
import com.raiz.bakcend.model.Usuario;
import com.raiz.bakcend.repository.PasswordResetTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetTokenService {

    private static final Duration VALIDEZ_TOKEN = Duration.ofMinutes(30);

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public PasswordResetTokenService(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Transactional
    public PasswordResetToken crearToken(Usuario usuario) {
        passwordResetTokenRepository.deleteByUsuarioAndUsadoFalse(usuario);

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID());
        token.setUsuario(usuario);
        token.setFechaExpiracion(LocalDateTime.now().plus(VALIDEZ_TOKEN));
        token.setUsado(false);
        return passwordResetTokenRepository.save(token);
    }

    public Optional<PasswordResetToken> buscarPorToken(UUID token) {
        return passwordResetTokenRepository.findByToken(token);
    }

    public void marcarComoUsado(PasswordResetToken token) {
        token.setUsado(true);
        passwordResetTokenRepository.save(token);
    }
}
