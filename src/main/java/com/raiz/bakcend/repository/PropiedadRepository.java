package com.raiz.bakcend.repository;

import com.raiz.bakcend.model.Propiedad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PropiedadRepository extends JpaRepository<Propiedad, UUID> {

    // 🔹 método original (lo podés dejar)
    List<Propiedad> findByAgenteId(UUID agenteId);

    // 🔥 MÉTODO OPTIMIZADO (SIN N+1), se ejecuta en un solo query
    @Query("""
    SELECT DISTINCT p FROM Propiedad p
    LEFT JOIN FETCH p.imagenes
    WHERE p.agenteId = :agenteId
""")
List<Propiedad> findByAgenteIdWithImagenes(UUID agenteId);
}