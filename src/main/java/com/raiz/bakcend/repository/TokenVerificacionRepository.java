package com.raiz.bakcend.repository;

import com.raiz.bakcend.model.TokenVerificacion;
import com.raiz.bakcend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TokenVerificacionRepository extends JpaRepository<TokenVerificacion, Long> {
    Optional<TokenVerificacion> findByToken(UUID token);

    void deleteByUsuarioAndUsadoFalse(Usuario usuario);
}
