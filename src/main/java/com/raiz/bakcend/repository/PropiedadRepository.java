package com.raiz.bakcend.repository;

import com.raiz.bakcend.model.Propiedad;
import com.raiz.bakcend.model.PublicacionEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PropiedadRepository extends JpaRepository<Propiedad, UUID> {

    // 🔹 método original (lo podés dejar)
    List<Propiedad> findByAgenteId(UUID agenteId);

    List<Propiedad> findByPublicacionEstado(PublicacionEstado publicacionEstado);

    @Query("""
            SELECT p FROM Propiedad p
            WHERE p.publicacionEstado = :estado
            AND EXISTS (
                SELECT 1 FROM Agente a
                WHERE a.id = p.agenteId
                AND EXISTS (
                    SELECT 1 FROM Usuario u
                    WHERE u.id = a.usuarioId AND u.membresiaActiva = true
                )
            )
            """)
    List<Propiedad> findPublicadasConMembresiaActiva(PublicacionEstado estado);

    // 🔥 MÉTODO OPTIMIZADO (SIN N+1), se ejecuta en un solo query
    @Query("""
            SELECT DISTINCT p FROM Propiedad p
            LEFT JOIN FETCH p.imagenes
            WHERE p.agenteId = :agenteId
            """)
    List<Propiedad> findByAgenteIdWithImagenes(UUID agenteId);

    @Query("""
            SELECT DISTINCT p FROM Propiedad p
            LEFT JOIN FETCH p.imagenes
            WHERE p.agenteId = :agenteId
            AND p.publicacionEstado = :estado
            AND EXISTS (
                SELECT 1 FROM Agente a
                WHERE a.id = :agenteId
                AND EXISTS (
                    SELECT 1 FROM Usuario u
                    WHERE u.id = a.usuarioId AND u.membresiaActiva = true
                )
            )
            """)
    List<Propiedad> findPublicadasByAgenteIdConMembresiaActiva(UUID agenteId, PublicacionEstado estado);
}