package com.raiz.bakcend.repository;

import com.raiz.bakcend.model.PasswordResetToken;
import com.raiz.bakcend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(UUID token);

    void deleteByUsuarioAndUsadoFalse(Usuario usuario);
}
