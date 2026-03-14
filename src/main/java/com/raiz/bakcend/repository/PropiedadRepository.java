package com.raiz.bakcend.repository;

import com.raiz.bakcend.model.Propiedad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PropiedadRepository extends JpaRepository<Propiedad, UUID> {
    List<Propiedad> findByAgenteId(UUID agenteId);
}