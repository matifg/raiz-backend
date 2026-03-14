package com.raiz.bakcend.repository;

import com.raiz.bakcend.model.Agente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AgenteRepository extends JpaRepository<Agente, UUID> {
    Optional<Agente> findByEmail(String email);
}
